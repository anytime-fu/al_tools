package com.toolbox.ui.productivity.kanban

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.toolbox.data.repository.SettingsRepository
import java.util.UUID
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent.inject

private val CARD_HEIGHT = 80.dp
private val columnTitles = listOf("待办", "进行中", "完成")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KanbanScreen(onBack: () -> Unit) {
    val settingsRepository: SettingsRepository by inject(SettingsRepository::class.java)
    val scope = rememberCoroutineScope()
    val tasks = remember { mutableStateListOf<KanbanTask>() }
    val cardBounds = remember { mutableStateMapOf<String, Rect>() }
    val colBounds = remember { mutableStateMapOf<Int, Rect>() }
    var rootBounds by remember { mutableStateOf<Rect?>(null) }
    var dragId by remember { mutableStateOf<String?>(null) }
    var dragDelta by remember { mutableStateOf(Offset.Zero) }

    var showDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<KanbanTask?>(null) }
    var draftTitle by remember { mutableStateOf("") }
    var draftNote by remember { mutableStateOf("") }
    var draftColumn by remember { mutableStateOf(0) }

    fun save() {
        val snapshot = tasks.toList()
        scope.launch {
            KanbanTaskStore.save(settingsRepository, snapshot)
        }
    }

    fun dropDragged() {
        val id = dragId ?: return
        val bounds = cardBounds[id] ?: return
        val task = tasks.firstOrNull { it.id == id } ?: return
        val center = bounds.center + dragDelta

        var targetCol = task.column
        for ((col, rect) in colBounds) {
            if (rect.contains(center)) {
                targetCol = col
                break
            }
        }

        val othersInCol = tasks.filter { it.column == targetCol && it.id != id }
        var insertIndex = othersInCol.size
        for ((index, other) in othersInCol.withIndex()) {
            val otherY = cardBounds[other.id]?.center?.y ?: Float.MAX_VALUE
            if (otherY > center.y) {
                insertIndex = index
                break
            }
        }

        val without = tasks.filter { it.id != id }
        val result = mutableListOf<KanbanTask>()
        for (col in 0..2) {
            val colTasks = without.filter { it.column == col }.toMutableList()
            if (col == targetCol) {
                colTasks.add(
                    insertIndex.coerceIn(0, colTasks.size),
                    task.copy(column = col, updatedAt = System.currentTimeMillis())
                )
            }
            result.addAll(colTasks)
        }
        tasks.clear()
        tasks.addAll(result)
        save()
    }

    LaunchedEffect(Unit) {
        val loaded = KanbanTaskStore.load(settingsRepository)
        if (loaded.isNotEmpty()) {
            tasks.clear()
            tasks.addAll(loaded)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { rootBounds = it.boundsInRoot() }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("任务看板") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            editing = null
                            draftTitle = ""
                            draftNote = ""
                            draftColumn = 0
                            showDialog = true
                        }) {
                            Icon(Icons.Default.Add, contentDescription = "添加任务")
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
                Text(
                    text = "长按卡片拖拽排序或移动到其他列，点击卡片编辑",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    for (col in 0..2) {
                        val colTasks = tasks.filter { it.column == col }
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .onGloballyPositioned { colBounds[col] = it.boundsInRoot() }
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${columnTitles[col]} (${colTasks.size})",
                                    style = MaterialTheme.typography.titleSmall,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = {
                                        editing = null
                                        draftTitle = ""
                                        draftNote = ""
                                        draftColumn = col
                                        showDialog = true
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "添加", modifier = Modifier.size(18.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(colTasks, key = { it.id }) { task ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(CARD_HEIGHT)
                                            .onGloballyPositioned { cardBounds[task.id] = it.boundsInRoot() }
                                            .graphicsLayer {
                                                alpha = if (dragId == task.id) 0.3f else 1f
                                            }
                                            .pointerInput(task.id) {
                                                detectDragGesturesAfterLongPress(
                                                    onDragStart = {
                                                        dragId = task.id
                                                        dragDelta = Offset.Zero
                                                    },
                                                    onDrag = { _, delta -> dragDelta += delta },
                                                    onDragEnd = {
                                                        dropDragged()
                                                        dragId = null
                                                    },
                                                    onDragCancel = { dragId = null }
                                                )
                                            },
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 10.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .fillMaxHeight()
                                                    .padding(top = 2.dp),
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Text(
                                                    text = task.title,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                if (task.note.isNotBlank()) {
                                                    Text(
                                                        text = task.note,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                            IconButton(
                                                onClick = {
                                                    tasks.removeAll { it.id == task.id }
                                                    save()
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Delete,
                                                    contentDescription = "删除",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        val dragged = dragId?.let { id -> tasks.firstOrNull { it.id == id } }
        val draggedBounds = dragged?.let { cardBounds[it.id] }
        val root = rootBounds
        if (dragged != null && draggedBounds != null && root != null) {
            val density = LocalDensity.current
            Card(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (draggedBounds.left + dragDelta.x - root.left).roundToInt(),
                            (draggedBounds.top + dragDelta.y - root.top).roundToInt()
                        )
                    }
                    .size(
                        with(density) { draggedBounds.width.toDp() },
                        with(density) { draggedBounds.height.toDp() }
                    ),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Box(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = dragged.title,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(if (editing == null) "添加任务" else "编辑任务") },
            text = {
                Column {
                    OutlinedTextField(
                        value = draftTitle,
                        onValueChange = { draftTitle = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("标题") },
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = draftNote,
                        onValueChange = { draftNote = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("备注（可留空）") },
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        columnTitles.forEachIndexed { index, title ->
                            FilterChip(
                                selected = draftColumn == index,
                                onClick = { draftColumn = index },
                                label = { Text(title) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val now = System.currentTimeMillis()
                        if (editing == null) {
                            val colTasks = tasks.filter { it.column == draftColumn }
                            val remaining = tasks.filter { it.column != draftColumn }
                            val result = mutableListOf<KanbanTask>()
                            for (col in 0..2) {
                                val list = if (col == draftColumn) {
                                    colTasks + KanbanTask(
                                        id = UUID.randomUUID().toString(),
                                        title = draftTitle.trim(),
                                        note = draftNote.trim(),
                                        column = col,
                                        updatedAt = now
                                    )
                                } else {
                                    remaining.filter { it.column == col }
                                }
                                result.addAll(list)
                            }
                            tasks.clear()
                            tasks.addAll(result)
                        } else {
                            val id = editing!!.id
                            val updated = tasks.map {
                                if (it.id == id) {
                                    it.copy(
                                        title = draftTitle.trim(),
                                        note = draftNote.trim(),
                                        column = draftColumn,
                                        updatedAt = now
                                    )
                                } else {
                                    it
                                }
                            }
                            val result = mutableListOf<KanbanTask>()
                            for (col in 0..2) {
                                result.addAll(updated.filter { it.column == col })
                            }
                            tasks.clear()
                            tasks.addAll(result)
                        }
                        save()
                        showDialog = false
                    },
                    enabled = draftTitle.isNotBlank()
                ) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("取消") } }
        )
    }
}
