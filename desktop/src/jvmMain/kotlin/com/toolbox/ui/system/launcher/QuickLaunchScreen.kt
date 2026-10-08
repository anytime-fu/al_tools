package com.toolbox.ui.system.launcher

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.awt.Desktop
import java.io.File
import java.net.URI
import javax.swing.JFileChooser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.java.KoinJavaComponent.inject
import com.toolbox.data.repository.SettingsRepository

private const val SETTINGS_KEY = "quick_launch_entries"

private data class LaunchEntry(
    val name: String,
    val path: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickLaunchScreen(onBack: () -> Unit) {
    val settingsRepository: SettingsRepository by inject(SettingsRepository::class.java)
    val scope = rememberCoroutineScope()
    val entries = remember { mutableStateListOf<LaunchEntry>() }
    var nameText by remember { mutableStateOf("") }
    var pathText by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(0) }
    var installedApps by remember { mutableStateOf<List<InstalledApp>>(emptyList()) }
    var installedLoading by remember { mutableStateOf(false) }
    var appSearch by remember { mutableStateOf("") }
    var launchingName by remember { mutableStateOf<String?>(null) }

    fun saveEntries() {
        scope.launch {
            val encoded = entries.joinToString("\n") { "${it.name}\t${it.path}" }
            settingsRepository.saveSetting(SETTINGS_KEY, encoded)
        }
    }

    fun loadInstalled() {
        if (installedLoading) return
        installedLoading = true
        scope.launch {
            installedApps = runCatching { scanInstalledApps() }.getOrDefault(emptyList())
            installedLoading = false
        }
    }

    fun launchWithOverlay(name: String, action: () -> Unit) {
        if (launchingName != null) return
        launchingName = name
        scope.launch {
            val start = System.currentTimeMillis()
            val result = runCatching { withContext(Dispatchers.IO) { action() } }
            val elapsed = System.currentTimeMillis() - start
            if (elapsed < 600) delay(600 - elapsed)
            message = if (result.isSuccess) "已启动: $name" else "启动失败: ${result.exceptionOrNull()?.message}"
            launchingName = null
        }
    }

    fun launchEntry(entry: LaunchEntry) {
        launchWithOverlay(entry.name) {
            if (entry.path.startsWith("http://") || entry.path.startsWith("https://")) {
                Desktop.getDesktop().browse(URI(entry.path))
            } else {
                Desktop.getDesktop().open(File(entry.path))
            }
        }
    }

    fun launchInstalledApp(app: InstalledApp) {
        launchWithOverlay(app.name) {
            when (app.kind) {
                AppKind.UWP -> ProcessBuilder("explorer.exe", "shell:AppsFolder\\${app.appId}").start()
                AppKind.COMMAND -> ProcessBuilder(app.path).start()
                AppKind.FILE -> Desktop.getDesktop().open(File(app.path))
            }
        }
    }

    LaunchedEffect(Unit) {
        val saved = settingsRepository.getSettingValue(SETTINGS_KEY, "")
        if (saved.isNotBlank()) {
            saved.lines().filter { it.isNotBlank() }.forEach { line ->
                val parts = line.split("\t", limit = 2)
                if (parts.size == 2) {
                    entries.add(LaunchEntry(parts[0], parts[1]))
                }
            }
        }
        loadInstalled()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("快捷启动") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = nameText,
                    onValueChange = { nameText = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("名称") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = pathText,
                    onValueChange = { pathText = it },
                    modifier = Modifier.weight(2f),
                    label = { Text("路径或 URL") },
                    singleLine = true
                )
                OutlinedButton(
                    onClick = {
                        val chooser = JFileChooser()
                        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                            pathText = chooser.selectedFile.absolutePath
                            if (nameText.isBlank()) nameText = chooser.selectedFile.name
                        }
                    }
                ) {
                    Icon(Icons.Default.Folder, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("浏览")
                }
                Button(
                    onClick = {
                        val name = nameText.trim()
                        val path = pathText.trim()
                        if (name.isEmpty() || path.isEmpty()) {
                            message = "名称和路径不能为空"
                            return@Button
                        }
                        entries.add(LaunchEntry(name, path))
                        nameText = ""
                        pathText = ""
                        message = ""
                        saveEntries()
                    }
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("添加")
                }
            }

            if (message.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("我的快捷方式 (${entries.size})") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("已安装软件 (${installedApps.size})") }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (selectedTab == 0) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(entries) { entry ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    Icons.Default.Launch,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = entry.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = entry.path,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Button(
                                    onClick = { launchEntry(entry) },
                                    enabled = launchingName == null
                                ) {
                                    Text("启动")
                                }

                                IconButton(
                                    onClick = {
                                        entries.remove(entry)
                                        saveEntries()
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "删除",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = appSearch,
                            onValueChange = { appSearch = it },
                            modifier = Modifier.weight(1f),
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            placeholder = { Text("搜索已安装软件...") },
                            singleLine = true
                        )
                        OutlinedButton(onClick = { loadInstalled() }, enabled = !installedLoading) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("刷新")
                        }
                    }

                    if (installedLoading) {
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val filteredApps = remember(installedApps, appSearch) {
                        if (appSearch.isBlank()) installedApps
                        else installedApps.filter { it.name.contains(appSearch, ignoreCase = true) }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(filteredApps) { app ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Apps,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = app.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = if (app.kind == AppKind.UWP) app.source else app.path,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Button(
                                        onClick = { launchInstalledApp(app) },
                                        enabled = launchingName == null
                                    ) {
                                        Text("启动")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = launchingName != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { }
                )
                Card(
                    modifier = Modifier.align(Alignment.Center),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 28.dp, vertical = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                        Text(
                            text = "正在启动 ${launchingName}…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
        }
    }
}
