package com.toolbox.ui.password

import com.toolbox.ui.components.appClickable

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.toolbox.data.local.entity.PasswordEntry
import com.toolbox.data.repository.PasswordRepository
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent.inject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordScreen(onBack: (() -> Unit)? = null) {
    val passwordRepository: PasswordRepository by inject(PasswordRepository::class.java)
    val coroutineScope = rememberCoroutineScope()
    
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedEntry by remember { mutableStateOf<PasswordEntry?>(null) }
    
    // Get passwords from repository
    val passwords by passwordRepository.getAllPasswordEntries().collectAsState(initial = emptyList())
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        TopAppBar(
            title = { Text("密码管理") },
            navigationIcon = {
                if (onBack != null) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface
            ),
            actions = {
                IconButton(onClick = { showAddDialog = true }) {
                    Icon(
                        Icons.Default.Add, 
                        contentDescription = "添加",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        )
        
        // Search
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            placeholder = { Text("搜索密码...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "搜索") },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                cursorColor = MaterialTheme.colorScheme.primary
            )
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Password list
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            val filteredPasswords = if (searchQuery.isBlank()) {
                passwords
            } else {
                passwords.filter {
                    it.appName.contains(searchQuery, ignoreCase = true) ||
                    it.account.contains(searchQuery, ignoreCase = true)
                }
            }
            
            items(filteredPasswords) { entry ->
                PasswordListItem(
                    entry = entry,
                    onClick = { selectedEntry = entry },
                    onDelete = {
                        coroutineScope.launch {
                            passwordRepository.deletePasswordEntry(entry)
                        }
                    }
                )
            }
        }
    }
    
    // Add dialog
    if (showAddDialog) {
        AddPasswordDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { appName, account, password ->
                coroutineScope.launch {
                    val newEntry = PasswordEntry(
                        appName = appName,
                        account = account,
                        password = password,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                    passwordRepository.insertPasswordEntry(newEntry)
                }
                showAddDialog = false
            }
        )
    }
    
    // Detail dialog
    selectedEntry?.let { entry ->
        PasswordDetailDialog(
            entry = entry,
            onDismiss = { selectedEntry = null },
            onDelete = {
                coroutineScope.launch {
                    passwordRepository.deletePasswordEntry(entry)
                }
                selectedEntry = null
            }
        )
    }
}

@Composable
fun PasswordListItem(
    entry: PasswordEntry,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .appClickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.appName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = entry.account,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun AddPasswordDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, String) -> Unit
) {
    var appName by remember { mutableStateOf("") }
    var account by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加密码") },
        text = {
            Column {
                OutlinedTextField(
                    value = appName,
                    onValueChange = { appName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("应用名称") },
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = account,
                    onValueChange = { account = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("账号") },
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("密码") },
                    singleLine = true,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (showPassword) "隐藏" else "显示"
                            )
                        }
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onAdd(appName, account, password) },
                enabled = appName.isNotBlank() && account.isNotBlank() && password.isNotBlank()
            ) {
                Text("添加")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
fun PasswordDetailDialog(
    entry: PasswordEntry,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    var showPassword by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(entry.appName) },
        text = {
            Column {
                Text(
                    "账号: ${entry.account}",
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "密码: ",
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (showPassword) entry.password else "••••••••",
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (showPassword) "隐藏" else "显示"
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDelete) {
                Text("删除")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}
