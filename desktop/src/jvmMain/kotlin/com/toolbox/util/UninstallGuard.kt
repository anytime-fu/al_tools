package com.toolbox.util

import com.sun.jna.platform.win32.Advapi32Util
import com.sun.jna.platform.win32.WinReg
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class UninstallFeedback(
    val reason: String,
    val comment: String,
    val appVersion: String,
    val timestamp: Long
)

object UninstallGuard {

    private const val APP_DISPLAY_NAME = "AI-Toolbox"
    const val GUARD_ARG = "--uninstall-guard"
    private const val UNINSTALL_PATH = "Software\\Microsoft\\Windows\\CurrentVersion\\Uninstall"
    private const val BACKUP_UNINSTALL = "AI-ToolboxUninstallStringBackup"
    private const val BACKUP_QUIET = "AI-ToolboxQuietUninstallStringBackup"
    private const val BACKUP_WINSTALLER = "AI-ToolboxWindowsInstallerBackup"

    private val json = Json { prettyPrint = true }

    val isWindows: Boolean
        get() = System.getProperty("os.name").orEmpty().lowercase().contains("win")

    fun isGuardRequest(args: Array<String>): Boolean = args.any { it == GUARD_ARG }

    fun parseProductCode(uninstallString: String): String? {
        return Regex("\\{[0-9A-Fa-f]{8}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{12}\\}")
            .find(uninstallString)?.value
    }

    data class ArpEntry(
        val root: WinReg.HKEY,
        val keyPath: String,
        val uninstallString: String
    )

    private fun findOwnArpEntry(): ArpEntry? {
        if (!isWindows) return null
        return runCatching {
            val roots = listOf(WinReg.HKEY_CURRENT_USER, WinReg.HKEY_LOCAL_MACHINE)
            for (root in roots) {
                val keys = runCatching { Advapi32Util.registryGetKeys(root, UNINSTALL_PATH) }.getOrDefault(emptyArray())
                for (key in keys) {
                    val path = "$UNINSTALL_PATH\\$key"
                    val display = runCatching {
                        Advapi32Util.registryGetStringValue(root, path, "DisplayName")
                    }.getOrNull()
                    if (display == APP_DISPLAY_NAME) {
                        val uninstall = runCatching {
                            Advapi32Util.registryGetStringValue(root, path, "UninstallString")
                        }.getOrNull() ?: continue
                        return@runCatching ArpEntry(root, path, uninstall)
                    }
                }
            }
            null
        }.getOrNull()
    }

    fun installHook() {
        if (!isWindows) return
        runCatching {
            val entry = findOwnArpEntry() ?: return
            if (entry.uninstallString.contains(GUARD_ARG)) return
            val exe = currentExePath() ?: return
            val guarded = "\"$exe\" $GUARD_ARG"

            Advapi32Util.registrySetStringValue(entry.root, entry.keyPath, BACKUP_UNINSTALL, entry.uninstallString)
            runCatching {
                val quiet = Advapi32Util.registryGetStringValue(entry.root, entry.keyPath, "QuietUninstallString")
                Advapi32Util.registrySetStringValue(entry.root, entry.keyPath, BACKUP_QUIET, quiet)
            }
            runCatching {
                val flag = Advapi32Util.registryGetIntValue(entry.root, entry.keyPath, "WindowsInstaller")
                Advapi32Util.registrySetIntValue(entry.root, entry.keyPath, BACKUP_WINSTALLER, flag)
                Advapi32Util.registrySetIntValue(entry.root, entry.keyPath, "WindowsInstaller", 0)
            }

            Advapi32Util.registrySetStringValue(entry.root, entry.keyPath, "UninstallString", guarded)
        }
    }

    fun restoreAndRunUninstaller(): Boolean {
        if (!isWindows) return false
        return runCatching {
            val entry = findOwnArpEntry() ?: return@runCatching false
            val original = runCatching {
                Advapi32Util.registryGetStringValue(entry.root, entry.keyPath, BACKUP_UNINSTALL)
            }.getOrNull()?.ifBlank { null } ?: entry.uninstallString.removeSuffix(" $GUARD_ARG").trim('"')

            Advapi32Util.registrySetStringValue(entry.root, entry.keyPath, "UninstallString", original)
            runCatching {
                val quiet = Advapi32Util.registryGetStringValue(entry.root, entry.keyPath, BACKUP_QUIET)
                Advapi32Util.registrySetStringValue(entry.root, entry.keyPath, "QuietUninstallString", quiet)
            }
            runCatching {
                val flag = Advapi32Util.registryGetIntValue(entry.root, entry.keyPath, BACKUP_WINSTALLER)
                Advapi32Util.registrySetIntValue(entry.root, entry.keyPath, "WindowsInstaller", flag)
            }

            val code = parseProductCode(original)
            val command = if (code != null) "msiexec.exe /X$code" else original
            ProcessBuilder("cmd.exe", "/c", "ping -n 3 127.0.0.1 > nul & $command")
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start()
            true
        }.getOrDefault(false)
    }

    fun saveFeedback(reason: String, comment: String) {
        runCatching {
            val dir = File(System.getProperty("user.home"), ".ai-toolbox")
            dir.mkdirs()
            val feedback = UninstallFeedback(
                reason = reason,
                comment = comment,
                appVersion = System.getProperty("jpackage.app-version") ?: "dev",
                timestamp = System.currentTimeMillis()
            )
            File(dir, "uninstall-feedback-${feedback.timestamp}.json")
                .writeText(json.encodeToString(feedback), Charsets.UTF_8)
        }
    }

    private fun currentExePath(): String? {
        val fromJpackage = System.getProperty("jpackage.app-path")
        if (!fromJpackage.isNullOrBlank()) return fromJpackage
        return runCatching {
            ProcessHandle.current().info().command().orElse(null)
        }.getOrNull()
    }
}