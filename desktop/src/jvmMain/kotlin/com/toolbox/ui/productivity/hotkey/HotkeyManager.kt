package com.toolbox.ui.productivity.hotkey

import com.github.kwhat.jnativehook.GlobalScreen
import com.github.kwhat.jnativehook.NativeInputEvent
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener
import java.util.logging.Level
import java.util.logging.Logger

object HotkeyManager {

    var onCombo: ((String) -> Unit)? = null

    private var installed = false

    fun isRunning(): Boolean = GlobalScreen.isNativeHookRegistered()

    fun start(): Boolean {
        if (GlobalScreen.isNativeHookRegistered()) return true
        return runCatching {
            runCatching {
                Logger.getLogger(GlobalScreen::class.java.`package`.name).level = Level.OFF
            }
            GlobalScreen.registerNativeHook()
            if (!installed) {
                GlobalScreen.addNativeKeyListener(object : NativeKeyListener {
                    override fun nativeKeyPressed(e: NativeKeyEvent) {
                        val combo = comboOf(e)
                        if (combo.isNotEmpty()) {
                            onCombo?.invoke(combo)
                        }
                    }

                    override fun nativeKeyReleased(e: NativeKeyEvent) {}

                    override fun nativeKeyTyped(e: NativeKeyEvent) {}
                })
                installed = true
            }
            true
        }.getOrDefault(false)
    }

    fun stop() {
        runCatching { GlobalScreen.unregisterNativeHook() }
    }

    fun comboOf(e: NativeKeyEvent): String {
        val keyText = NativeKeyEvent.getKeyText(e.keyCode)
        if (isModifierKey(e.keyCode, keyText)) return ""
        val parts = mutableListOf<String>()
        val modifiers = e.modifiers
        if (modifiers and NativeInputEvent.CTRL_MASK != 0) parts.add("Ctrl")
        if (modifiers and NativeInputEvent.ALT_MASK != 0) parts.add("Alt")
        if (modifiers and NativeInputEvent.SHIFT_MASK != 0) parts.add("Shift")
        if (modifiers and NativeInputEvent.META_MASK != 0) parts.add("Meta")
        parts.add(keyText.replaceFirstChar { it.uppercase() })
        return parts.joinToString("+")
    }

    private fun isModifierKey(code: Int, text: String): Boolean {
        if (
            code == NativeKeyEvent.VC_SHIFT || code == NativeKeyEvent.VC_CONTROL ||
            code == NativeKeyEvent.VC_ALT || code == NativeKeyEvent.VC_META
        ) {
            return true
        }
        val lower = text.lowercase()
        return "shift" in lower || "control" in lower || "ctrl" in lower || "alt" in lower ||
            "meta" in lower || "windows" in lower || "command" in lower || "option" in lower
    }
}
