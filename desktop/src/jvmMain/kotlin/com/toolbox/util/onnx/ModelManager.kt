package com.toolbox.util.onnx

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

object ModelManager {

    const val DEFAULT_URL = "https://github.com/MohaElder/openenlarge/releases/download/autodust-assets-v1/migan_pipeline_v2.onnx"
    const val DEFAULT_SHA256 = "6f1f3530a1a2324b19752018ce756088b07973cda8d7d890034ace5c8a48c40b"
    const val BUNDLED_MODEL_NAME = "migan_pipeline_v2.onnx"

    private val MIRRORS = listOf(
        DEFAULT_URL,
        "https://ghproxy.net/$DEFAULT_URL",
        "https://ghfast.top/$DEFAULT_URL"
    )

    val modelDir: File
        get() = File(System.getProperty("user.home"), ".ai-toolbox/models")

    val modelFile: File
        get() = File(modelDir, BUNDLED_MODEL_NAME)

    fun isBundledAvailable(): Boolean {
        return ModelManager::class.java.getResource("/models/$BUNDLED_MODEL_NAME") != null
    }

    fun isModelReady(): Boolean = resolveModel() != null

    fun resolveModel(): File? {
        if (modelFile.exists() && modelFile.length() > 1_000_000) return modelFile
        val resource = ModelManager::class.java.getResourceAsStream("/models/$BUNDLED_MODEL_NAME")
            ?: return null
        return runCatching {
            modelDir.mkdirs()
            resource.use { input ->
                modelFile.outputStream().use { out -> input.copyTo(out) }
            }
            modelFile
        }.getOrNull()
    }

    fun statusText(): String {
        return when {
            modelFile.exists() && modelFile.length() > 1_000_000 ->
                "模型已就绪（${"%.0f".format(modelFile.length() / 1024.0 / 1024.0)} MB）"
            isBundledAvailable() -> "模型已内置（首次使用自动释放）"
            else -> "模型未安装：可下载或手动导入 ONNX 模型"
        }
    }

    suspend fun download(
        url: String? = null,
        expectedSha256: String? = DEFAULT_SHA256,
        onProgress: (downloaded: Long, total: Long) -> Unit = { _, _ -> }
    ): File = withContext(Dispatchers.IO) {
        modelDir.mkdirs()
        val partFile = File(modelDir, "$BUNDLED_MODEL_NAME.part")
        val targets = if (url != null) listOf(url) else MIRRORS
        var lastError: Exception? = null

        for (round in 1..8) {
            for (target in targets) {
                try {
                    val connection = URL(target).openConnection() as HttpURLConnection
                    connection.connectTimeout = 15_000
                    connection.readTimeout = 60_000
                    connection.instanceFollowRedirects = true
                    val have = if (partFile.exists()) partFile.length() else 0L
                    if (have > 0) connection.setRequestProperty("Range", "bytes=$have-")
                    connection.connect()
                    if (connection.responseCode !in 200..299) {
                        connection.disconnect()
                        throw IllegalStateException("HTTP ${connection.responseCode}")
                    }
                    val total = connection.contentLengthLong + have
                    connection.inputStream.use { input ->
                        partFile.outputStream().use { out ->
                            val buffer = ByteArray(256 * 1024)
                            var downloaded = have
                            while (true) {
                                val read = input.read(buffer)
                                if (read < 0) break
                                out.write(buffer, 0, read)
                                downloaded += read
                                onProgress(downloaded, total)
                            }
                        }
                    }
                    connection.disconnect()
                    if (partFile.length() > 1_000_000) {
                        if (expectedSha256 == null || sha256(partFile).equals(expectedSha256, true)) {
                            if (modelFile.exists()) modelFile.delete()
                            if (!partFile.renameTo(modelFile)) {
                                partFile.copyTo(modelFile, overwrite = true)
                                partFile.delete()
                            }
                            return@withContext modelFile
                        }
                        partFile.delete()
                        throw IllegalStateException("模型校验失败")
                    }
                } catch (e: Exception) {
                    lastError = e
                }
            }
        }
        throw IllegalStateException("模型下载失败: ${lastError?.message ?: "网络不可达"}")
    }

    fun installFromFile(source: File, verifySha256: String? = null): File {
        if (!source.exists()) throw IllegalArgumentException("文件不存在: ${source.name}")
        if (source.length() < 100_000) throw IllegalArgumentException("所选文件不是有效模型")
        modelDir.mkdirs()
        if (verifySha256 != null) {
            val actual = sha256(source)
            if (!actual.equals(verifySha256, true)) {
                throw IllegalStateException("模型校验失败：文件与预期 SHA-256 不符")
            }
        }
        source.copyTo(modelFile, overwrite = true)
        return modelFile
    }

    fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(256 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}