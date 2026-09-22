package com.toolbox.ui.pdf

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfScreen(
    onBack: () -> Unit,
    viewModel: PdfViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val pdfDocument by viewModel.pdfDocument.collectAsState()
    val currentPage by viewModel.currentPage.collectAsState()
    val totalPages by viewModel.totalPages.collectAsState()
    val currentPageText by viewModel.currentPageText.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val fileName by viewModel.fileName.collectAsState()

    var showAiDialog by remember { mutableStateOf(false) }
    var aiQuestion by remember { mutableStateOf("") }
    var aiAnswer by remember { mutableStateOf("") }
    var isAiLoading by remember { mutableStateOf(false) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.loadPdf(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (fileName.isNotEmpty()) fileName else "PDF阅读器") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (pdfDocument != null) {
                        IconButton(onClick = { showAiDialog = true }) {
                            Icon(Icons.Default.SmartToy, contentDescription = "AI问答")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if (pdfDocument == null && !isLoading) {
                // 空状态 - 选择PDF文件
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "选择PDF文件",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "支持本地PDF文件阅读",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { filePicker.launch("application/pdf") }
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("打开PDF")
                        }
                    }
                }
            } else if (isLoading) {
                // 加载状态
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("正在加载PDF...")
                    }
                }
            } else if (error != null) {
                // 错误状态
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = error ?: "",
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { filePicker.launch("application/pdf") }) {
                            Text("重新选择")
                        }
                    }
                }
            } else {
                // PDF内容显示
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = currentPageText.ifEmpty { "此页无文字内容" },
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 页面导航
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { viewModel.previousPage() },
                            enabled = currentPage > 1
                        ) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "上一页")
                        }

                        Text(
                            text = "第 $currentPage / $totalPages 页",
                            style = MaterialTheme.typography.titleMedium
                        )

                        IconButton(
                            onClick = { viewModel.nextPage() },
                            enabled = currentPage < totalPages
                        ) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "下一页")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 打开其他文件
                OutlinedButton(
                    onClick = { filePicker.launch("application/pdf") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("打开其他PDF")
                }
            }
        }
    }

    // AI问答对话框
    if (showAiDialog) {
        AlertDialog(
            onDismissRequest = {
                showAiDialog = false
                aiQuestion = ""
                aiAnswer = ""
            },
            title = { Text("PDF AI问答") },
            text = {
                Column {
                    Text(
                        text = "基于当前PDF内容提问",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    OutlinedTextField(
                        value = aiQuestion,
                        onValueChange = { aiQuestion = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("你的问题") },
                        minLines = 2
                    )

                    if (aiAnswer.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "AI回答：",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Text(
                                text = aiAnswer,
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    if (isAiLoading) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("AI思考中...")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        // TODO: 调用AI API进行问答
                        // 这里需要使用AiApiService
                        aiAnswer = "AI问答功能需要配置API Key后使用，请在设置中配置。"
                    },
                    enabled = aiQuestion.isNotBlank() && !isAiLoading
                ) {
                    Text("提问")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAiDialog = false
                    aiQuestion = ""
                    aiAnswer = ""
                }) {
                    Text("关闭")
                }
            }
        )
    }
}
