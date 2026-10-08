package com.toolbox.ui.security.encrypt

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import javax.swing.JFileChooser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val MAGIC = byteArrayOf(0x54, 0x42, 0x41, 0x45)
private const val SALT_LEN = 16
private const val IV_LEN = 12
private const val PBKDF2_ITERATIONS = 120_000

private fun deriveKey(password: String, salt: ByteArray): SecretKeySpec {
    val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
    val spec = PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, 256)
    val key = factory.generateSecret(spec).encoded
    return SecretKeySpec(key, "AES")
}

private fun encryptFile(src: File, dst: File, password: String) {
    val random = SecureRandom()
    val salt = ByteArray(SALT_LEN).also { random.nextBytes(it) }
    val iv = ByteArray(IV_LEN).also { random.nextBytes(it) }
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.ENCRYPT_MODE, deriveKey(password, salt), GCMParameterSpec(128, iv))
    FileOutputStream(dst).use { out ->
        out.write(MAGIC)
        out.write(salt)
        out.write(iv)
        CipherOutputStream(out, cipher).use { cos ->
            FileInputStream(src).use { it.copyTo(cos) }
        }
    }
}

private fun decryptFile(src: File, dst: File, password: String) {
    FileInputStream(src).use { input ->
        val header = ByteArray(MAGIC.size + SALT_LEN + IV_LEN)
        var read = 0
        while (read < header.size) {
            val n = input.read(header, read, header.size - read)
            if (n < 0) error("文件格式不正确或已损坏")
            read += n
        }
        if (!header.copyOfRange(0, MAGIC.size).contentEquals(MAGIC)) {
            error("不是本工具加密的文件")
        }
        val salt = header.copyOfRange(MAGIC.size, MAGIC.size + SALT_LEN)
        val iv = header.copyOfRange(MAGIC.size + SALT_LEN, header.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, deriveKey(password, salt), GCMParameterSpec(128, iv))
        try {
            CipherInputStream(input, cipher).use { cis ->
                FileOutputStream(dst).use { cis.copyTo(it) }
            }
        } catch (e: Exception) {
            dst.delete()
            error("解密失败：密码错误或文件已损坏")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileEncryptScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var encryptMode by remember { mutableStateOf(true) }
    var inputPath by remember { mutableStateOf("") }
    var outputPath by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var running by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("文件加密") },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = encryptMode,
                    onClick = {
                        encryptMode = true
                        message = ""
                    },
                    label = { Text("加密") }
                )
                FilterChip(
                    selected = !encryptMode,
                    onClick = {
                        encryptMode = false
                        message = ""
                    },
                    label = { Text("解密") }
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = inputPath,
                    onValueChange = { inputPath = it },
                    modifier = Modifier.weight(1f),
                    label = { Text(if (encryptMode) "选择要加密的文件" else "选择要解密的文件 (.bae)") },
                    singleLine = true
                )
                OutlinedButton(onClick = {
                    val chooser = JFileChooser()
                    if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                        inputPath = chooser.selectedFile.absolutePath
                        outputPath = if (encryptMode) {
                            chooser.selectedFile.absolutePath + ".bae"
                        } else {
                            val name = chooser.selectedFile.nameWithoutExtension
                            File(chooser.selectedFile.parentFile, name).absolutePath
                        }
                    }
                }) {
                    Icon(Icons.Default.Folder, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("浏览")
                }
            }

            OutlinedTextField(
                value = outputPath,
                onValueChange = { outputPath = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("输出文件路径") },
                singleLine = true
            )

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
                            if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "切换显示"
                        )
                    }
                }
            )

            if (encryptMode) {
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("确认密码") },
                    singleLine = true,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation()
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        if (password.isBlank()) {
                            message = "密码不能为空"
                            return@Button
                        }
                        if (encryptMode && password != confirmPassword) {
                            message = "两次输入的密码不一致"
                            return@Button
                        }
                        val src = File(inputPath)
                        val dst = File(outputPath)
                        if (!src.isFile) {
                            message = "输入文件不存在"
                            return@Button
                        }
                        running = true
                        message = ""
                        scope.launch {
                            val result = withContext(Dispatchers.IO) {
                                runCatching {
                                    if (encryptMode) encryptFile(src, dst, password) else decryptFile(src, dst, password)
                                }
                            }
                            running = false
                            message = result.fold(
                                onSuccess = {
                                    if (encryptMode) "加密完成: ${dst.absolutePath}" else "解密完成: ${dst.absolutePath}"
                                },
                                onFailure = { it.message ?: "操作失败" }
                            )
                        }
                    },
                    enabled = !running && inputPath.isNotBlank() && outputPath.isNotBlank() && password.isNotBlank()
                ) {
                    Icon(
                        if (encryptMode) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        when {
                            running -> "处理中..."
                            encryptMode -> "加密文件"
                            else -> "解密文件"
                        }
                    )
                }
                if (running) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            }

            if (message.isNotEmpty()) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (message.contains("完成")) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "算法：AES-256-GCM + PBKDF2(12 万次迭代) 密钥派生；加密文件以 .bae 结尾，头 28 字节为格式标记/盐/IV。\n" +
                    "请牢记密码，密码丢失无法解密；解密失败时输出文件会被自动删除。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
