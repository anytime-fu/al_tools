package com.toolbox.ui.password

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.toolbox.ui.components.AppPrimaryButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordEditScreen(
    passwordId: Long?,
    onBack: () -> Unit,
    viewModel: PasswordViewModel = hiltViewModel()
) {
    val editingPassword by viewModel.editingPassword.collectAsState()
    var appName by remember { mutableStateOf("") }
    var account by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoaded by remember { mutableStateOf(false) }

    val isEdit = passwordId != null && passwordId > 0

    LaunchedEffect(passwordId) {
        if (isEdit) {
            viewModel.loadPassword(passwordId!!)
        }
    }

    LaunchedEffect(editingPassword) {
        if (isEdit && editingPassword != null && !isLoaded) {
            editingPassword?.let {
                appName = it.appName
                account = it.account
                password = it.password
                note = it.note
                isLoaded = true
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEdit) "编辑密码" else "添加密码") },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.clearEditingPassword()
                        onBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // 应用名称
            OutlinedTextField(
                value = appName,
                onValueChange = { appName = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("应用名称 *") },
                placeholder = { Text("例如：微信、淘宝、GitHub") },
                leadingIcon = { Icon(Icons.Default.Apps, contentDescription = null) },
                singleLine = true,
                shape = MaterialTheme.shapes.large
            )

            // 账号
            OutlinedTextField(
                value = account,
                onValueChange = { account = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("账号 *") },
                placeholder = { Text("手机号/邮箱/用户名") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                singleLine = true,
                shape = MaterialTheme.shapes.large
            )

            // 密码
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("密码 *") },
                placeholder = { Text("输入密码") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (passwordVisible) "隐藏密码" else "显示密码"
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                shape = MaterialTheme.shapes.large
            )

            // 备注
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("备注") },
                placeholder = { Text("可选备注信息") },
                leadingIcon = { Icon(Icons.Default.Notes, contentDescription = null) },
                minLines = 3,
                shape = MaterialTheme.shapes.large
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 保存按钮
            AppPrimaryButton(
                text = "保存",
                onClick = {
                    if (appName.isNotBlank() && account.isNotBlank() && password.isNotBlank()) {
                        viewModel.savePassword(
                            id = if (isEdit) passwordId else null,
                            appName = appName.trim(),
                            account = account.trim(),
                            password = password.trim(),
                            note = note.trim()
                        )
                        viewModel.clearEditingPassword()
                        onBack()
                    }
                },
                enabled = appName.isNotBlank() && account.isNotBlank() && password.isNotBlank()
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
