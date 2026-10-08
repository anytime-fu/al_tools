package com.toolbox.ui.system.launcher

import java.io.File
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal enum class AppKind { FILE, UWP, COMMAND }

internal data class InstalledApp(
    val name: String,
    val path: String,
    val appId: String = "",
    val kind: AppKind = AppKind.FILE,
    val source: String = ""
)

internal suspend fun scanInstalledApps(): List<InstalledApp> = withContext(Dispatchers.IO) {
    val merged = LinkedHashMap<String, InstalledApp>()
    if (isWindows()) {
        scanStartMenuLinks().forEach { merged.putIfAbsent(it.name.lowercase(), it) }
        scanUwpApps().forEach { merged.putIfAbsent(it.name.lowercase(), it) }
    } else {
        scanOtherPlatforms().forEach { merged.putIfAbsent(it.name.lowercase(), it) }
    }
    merged.values.sortedBy { it.name.lowercase() }
}

private fun isWindows(): Boolean =
    System.getProperty("os.name", "").lowercase().contains("windows")

private fun scanStartMenuLinks(): List<InstalledApp> {
    val dirs = listOfNotNull(
        System.getenv("ProgramData")?.let { File(it, "Microsoft\\Windows\\Start Menu\\Programs") },
        System.getenv("APPDATA")?.let { File(it, "Microsoft\\Windows\\Start Menu\\Programs") }
    )
    val result = mutableListOf<InstalledApp>()
    for (dir in dirs) {
        if (!dir.isDirectory) continue
        dir.walkTopDown()
            .filter { it.isFile && (it.extension.equals("lnk", true) || it.extension.equals("url", true)) }
            .forEach { file ->
                val name = file.nameWithoutExtension
                if (name.isNotEmpty()) {
                    result.add(InstalledApp(name, file.absolutePath, source = "开始菜单"))
                }
            }
    }
    return result
}

private const val PS_SCRIPT =
    "[Console]::OutputEncoding=[System.Text.Encoding]::UTF8; " +
        "Get-StartApps | ForEach-Object { \$_.Name + '|||' + \$_.AppID }"

private fun scanUwpApps(): List<InstalledApp> {
    val output = runCatching {
        val process = ProcessBuilder(
            "powershell.exe", "-NoProfile", "-NonInteractive", "-ExecutionPolicy", "Bypass",
            "-Command", PS_SCRIPT
        )
            .redirectErrorStream(true)
            .start()
        val text = process.inputStream.bufferedReader(Charsets.UTF_8).readText()
        if (!process.waitFor(20, TimeUnit.SECONDS)) process.destroyForcibly()
        text
    }.getOrDefault("")
    return output.lineSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .mapNotNull { line ->
            val idx = line.indexOf("|||")
            if (idx <= 0) return@mapNotNull null
            val name = line.substring(0, idx).trim()
            val appId = line.substring(idx + 3).trim()
            if (name.isNotEmpty() && appId.contains('!')) {
                InstalledApp(name, "", appId, AppKind.UWP, "UWP 应用")
            } else {
                null
            }
        }
        .toList()
}

private fun scanOtherPlatforms(): List<InstalledApp> {
    val os = System.getProperty("os.name", "").lowercase()
    val result = mutableListOf<InstalledApp>()
    if (os.contains("mac")) {
        listOf(File("/Applications"), File(System.getProperty("user.home"), "Applications")).forEach { dir ->
            if (!dir.isDirectory) return@forEach
            dir.listFiles { f -> f.isDirectory && f.name.endsWith(".app") }?.forEach { app ->
                result.add(InstalledApp(app.name.removeSuffix(".app"), app.absolutePath, source = "应用程序"))
            }
        }
    } else {
        listOf(
            File("/usr/share/applications"),
            File(System.getProperty("user.home"), ".local/share/applications")
        ).forEach { dir ->
            if (!dir.isDirectory) return@forEach
            dir.listFiles { f -> f.isFile && f.name.endsWith(".desktop") }?.forEach { file ->
                val lines = runCatching { file.readLines() }.getOrDefault(emptyList())
                val name = lines.firstOrNull { it.startsWith("Name=") }?.removePrefix("Name=")?.trim()
                val exec = lines.firstOrNull { it.startsWith("Exec=") }
                    ?.removePrefix("Exec=")
                    ?.substringBefore("%")
                    ?.trim()
                    ?.trim('"')
                if (!name.isNullOrEmpty() && !exec.isNullOrEmpty() && File(exec).isFile) {
                    result.add(InstalledApp(name, exec, kind = AppKind.COMMAND, source = "Linux"))
                }
            }
        }
    }
    return result
}
