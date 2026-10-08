package com.toolbox.ui.productivity.sticky

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.toolbox.data.repository.SettingsRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.UUID
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.java.KoinJavaComponent.inject

private const val SETTINGS_KEY = "sticky_notes"

private val json = Json { ignoreUnknownKeys = true }

@Serializable
private data class StickyNote(
    val id: String,
    val content: String,
    val color: Int,
    val group: String,
    val pinned: Boolean,
    val updatedAt: Long
)

private data class NoteColor(val name: String, val bg: Color, val fg: Color)

private val noteColors = listOf(
    NoteColor("黄", Color(0xFFFFE066), Color(0xFF5C4A00)),
    NoteColor("绿", Color(0xFF95DE64), Color(0xFF1F4A00)),
    NoteColor("蓝", Color(0xFF69C0FF), Color(0xFF00355C)),
    NoteColor("紫", Color(0xFFD3ADF7), Color(0xFF3B0070)),
    NoteColor("粉", Color(0xFFFF9C9C), Color(0xFF5C0000)),
    NoteColor("橙", Color(0xFFFFC069), Color(0xFF5C3300))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StickyNotesScreen(onBack: () -> Unit) {
    val settingsRepository: SettingsRepository by inject(SettingsRepository::class.java)
    val scope = rememberCoroutineScope()
    var notes by remember { mutableStateOf<List<StickyNote>>(emptyList()) }
    var filterGroup by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<StickyNote?>(null) }
    var draftContent by remember { mutableStateOf("") }
    var draftColor by remember { mutableStateOf(0) }
    var draftGroup by remember { mutableStateOf("") }
    var draftPinned by remember { mutableStateOf(false) }

    fun sorted(list: List<StickyNote>) =
        list.sortedWith(compareByDescending<StickyNote> { it.pinned }.thenByDescending { it.updatedAt })

    fun save() {
        val snapshot = notes
        scope.launch {
            settingsRepository.saveSetting(SETTINGS_KEY, json.encodeToString(snapshot))
        }
    }

    LaunchedEffect(Unit) {
        val saved = settingsRepository.getSettingValue(SETTINGS_KEY, "")
        if (saved.isNotBlank()) {
            notes = runCatching { sorted(json.decodeFromString<List<StickyNote>>(saved)) }
                .getOrDefault(emptyList())
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("便签管理") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        editing = null
                        draftContent = ""
                        draftColor = 0
                        draftGroup = filterGroup
                        draftPinned = false
                        showDialog = true
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "新建便签")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(12.dp)
        ) {
            val groups = notes.map { it.group }.filter { it.isNotBlank() }.distinct().sorted()
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(
                    selected = filterGroup == "",
                    onClick = { filterGroup = "" },
                    label = { Text("全部 (${notes.size})") }
                )
                groups.forEach { group ->
                    FilterChip(
                        selected = filterGroup == group,
                        onClick = { filterGroup = group },
                        label = { Text(group) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val visible = notes.filter { filterGroup == "" || it.group == filterGroup }
            if (visible.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "点击右上角 + 新建便签",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 180.dp),
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(visible, key = { it.id }) { note ->
                        val palette = noteColors[note.color.coerceIn(0, noteColors.size - 1)]
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 140.dp)
                                .clickable {
                                    editing = note
                                    draftContent = note.content
                                    draftColor = note.color
                                    draftGroup = note.group
                                    draftPinned = note.pinned
                                    showDialog = true
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = palette.bg)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (note.group.isNotBlank()) {
                                        Text(
                                            text = note.group,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = palette.fg.copy(alpha = 0.8f),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color.White.copy(alpha = 0.35f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.weight(1f))
                                    IconButton(
                                        onClick = {
                                            notes = sorted(
                                                notes.map {
                                                    if (it.id == note.id) it.copy(pinned = !it.pinned, updatedAt = System.currentTimeMillis()) else it
                                                }
                                            )
                                            save()
                                        },
                                        modifier = Modifier.size(26.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.PushPin,
                                            contentDescription = "置顶",
                                            tint = if (note.pinned) palette.fg else palette.fg.copy(alpha = 0.4f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            notes = notes.filter { it.id != note.id }
                                            save()
                                        },
                                        modifier = Modifier.size(26.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "删除",
                                            tint = palette.fg.copy(alpha = 0.7f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = note.content,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = palette.fg,
                                    maxLines = 6,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                                Text(
                                    text = SimpleDateFormat("MM-dd HH:mm").format(Date(note.updatedAt)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.fg.copy(alpha = 0.6f),
                                    modifier = Modifier.padding(top = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(if (editing == null) "新建便签" else "编辑便签") },
            text = {
                Column {
                    OutlinedTextField(
                        value = draftContent,
                        onValueChange = { draftContent = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("内容") },
                        minLines = 3,
                        maxLines = 6
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("颜色", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        noteColors.forEachIndexed { index, palette ->
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(palette.bg)
                                    .clickable { draftColor = index },
                                contentAlignment = Alignment.Center
                            ) {
                                if (draftColor == index) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(palette.fg)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = draftGroup,
                        onValueChange = { draftGroup = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("分组（可留空）") },
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = draftPinned, onCheckedChange = { draftPinned = it })
                        Text("置顶显示", style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val now = System.currentTimeMillis()
                        notes = if (editing == null) {
                            sorted(
                                notes + StickyNote(
                                    id = UUID.randomUUID().toString(),
                                    content = draftContent,
                                    color = draftColor,
                                    group = draftGroup.trim(),
                                    pinned = draftPinned,
                                    updatedAt = now
                                )
                            )
                        } else {
                            sorted(
                                notes.map {
                                    if (it.id == editing!!.id) {
                                        it.copy(
                                            content = draftContent,
                                            color = draftColor,
                                            group = draftGroup.trim(),
                                            pinned = draftPinned,
                                            updatedAt = now
                                        )
                                    } else {
                                        it
                                    }
                                }
                            )
                        }
                        save()
                        showDialog = false
                    },
                    enabled = draftContent.isNotBlank()
                ) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("取消") } }
        )
    }
}
