import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.security.MessageDigest

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization") version "1.9.20"
    id("org.jetbrains.compose")
}

kotlin {
    jvm {
        withJava()
    }
    
    sourceSets {
        val jvmMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(compose.material3)
                implementation(compose.materialIconsExtended)
                
                // Coroutines
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.7.3")
                
                // Koin
                implementation("io.insert-koin:koin-core:3.5.3")
                
                // Ktor
                implementation("io.ktor:ktor-client-java:2.3.7")
                implementation("io.ktor:ktor-client-content-negotiation:2.3.7")
                implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.7")
                implementation("io.ktor:ktor-client-cio:2.3.7")
                
                // Serialization
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
                
                // Diff 比较
                implementation("io.github.java-diff-utils:java-diff-utils:4.12")
                
                // Markdown 解析
                implementation("org.commonmark:commonmark:0.22.0")
                implementation("org.commonmark:commonmark-ext-gfm-tables:0.22.0")
                
                // CSV 解析
                implementation("org.apache.commons:commons-csv:1.10.0")
                
                // YAML 解析
                implementation("org.yaml:snakeyaml:2.2")
                
                // 系统信息
                implementation("com.github.oshi:oshi-core:6.4.8")
                
                // 全局快捷键
                implementation("com.github.kwhat:jnativehook:2.2.2")
                
                // JNA（注册表访问等本地集成）
                implementation("net.java.dev.jna:jna:5.13.0")
                implementation("net.java.dev.jna:jna-platform:5.13.0")
                
                // ONNX Runtime（AI 修复推理，官方 CPU 版，跨平台稳定）
                implementation("com.microsoft.onnxruntime:onnxruntime:1.17.1")
                
                // 图像处理
                implementation("com.twelvemonkeys.imageio:imageio-core:3.10.1")
                implementation("com.twelvemonkeys.imageio:imageio-jpeg:3.10.1")
                implementation("com.twelvemonkeys.imageio:imageio-webp:3.10.1")
                implementation("org.sejda.imageio:webp-imageio:0.1.6")
                
                // SQLDelight
                implementation("app.cash.sqldelight:sqlite-driver:2.0.1")
                implementation("app.cash.sqldelight:coroutines-extensions:2.0.1")
                
                // Project modules
                implementation(project(":common"))
            }
        }
        
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.toolbox.MainKt"
        
        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Exe, TargetFormat.Dmg, TargetFormat.Deb, TargetFormat.Rpm)
            packageName = "AI-Toolbox"
            packageVersion = "1.0.0"
            
            modules(
                "java.sql",
                "java.naming",
                "java.management",
                "jdk.management",
                "jdk.unsupported",
                "java.net.http"
            )
            
            windows {
                menuGroup = "AI-Toolbox"
                perUserInstall = true
                upgradeUuid = "50492884-e08f-442c-bf76-51826796d4bf"
                dirChooser = true
                shortcut = true
                iconFile.set(project.file("icons/icon.ico"))
            }
            
            macOS {
                bundleID = "com.toolbox.desktop"
                iconFile.set(project.file("icons/icon.icns"))
            }
            
            linux {
                packageName = "ai-toolbox"
                debMaintainer = "dev@ai-toolbox.local"
                menuGroup = "Utilities"
                iconFile.set(project.file("icons/icon.png"))
            }
        }
    }
}

val ensureBundledModel = tasks.register("ensureBundledModel") {
    val outputFile = file("src/jvmMain/resources/models/migan_pipeline_v2.onnx")
    val expectedSha = "6f1f3530a1a2324b19752018ce756088b07973cda8d7d890034ace5c8a48c40b"
    val mirrors = listOf(
        "https://github.com/MohaElder/openenlarge/releases/download/autodust-assets-v1/migan_pipeline_v2.onnx",
        "https://ghproxy.net/https://github.com/MohaElder/openenlarge/releases/download/autodust-assets-v1/migan_pipeline_v2.onnx",
        "https://ghfast.top/https://github.com/MohaElder/openenlarge/releases/download/autodust-assets-v1/migan_pipeline_v2.onnx"
    )
    outputs.file(outputFile)
    doLast {
        fun sha256(f: File): String {
            val d = MessageDigest.getInstance("SHA-256")
            f.inputStream().use { ins ->
                val buf = ByteArray(256 * 1024)
                while (true) {
                    val n = ins.read(buf)
                    if (n < 0) break
                    d.update(buf, 0, n)
                }
            }
            return d.digest().joinToString("") { b -> "%02x".format(b.toInt() and 0xFF) }
        }

        if (outputFile.exists() && outputFile.length() > 1_000_000 && sha256(outputFile).equals(expectedSha, true)) {
            logger.lifecycle("bundled model ok: ${outputFile.name}")
            return@doLast
        }
        outputFile.parentFile.mkdirs()
        if (outputFile.exists()) outputFile.delete()

        var ok = false
        for (round in 1..8) {
            for (url in mirrors) {
                try {
                    val conn = URI(url).toURL().openConnection() as HttpURLConnection
                    conn.connectTimeout = 15000
                    conn.readTimeout = 60000
                    conn.instanceFollowRedirects = true
                    val have = if (outputFile.exists()) outputFile.length() else 0L
                    if (have > 0) conn.setRequestProperty("Range", "bytes=$have-")
                    conn.connect()
                    conn.inputStream.use { input ->
                        FileOutputStream(outputFile, have > 0).use { out ->
                            val buf = ByteArray(256 * 1024)
                            while (true) {
                                val n = input.read(buf)
                                if (n < 0) break
                                out.write(buf, 0, n)
                            }
                        }
                    }
                    conn.disconnect()
                    if (outputFile.length() > 20_000_000 && sha256(outputFile).equals(expectedSha, true)) {
                        ok = true
                        logger.lifecycle("bundled model downloaded from $url")
                        break
                    }
                } catch (e: Exception) {
                    logger.warn("model download failed ($url): ${e.message}")
                }
            }
            if (ok) break
        }
        if (!ok) {
            logger.warn("bundled model NOT ready — app will offer in-app download/import instead")
        }
    }
}

tasks.named("jvmProcessResources") { dependsOn(ensureBundledModel) }
