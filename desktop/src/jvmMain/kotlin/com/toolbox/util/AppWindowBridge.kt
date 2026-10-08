package com.toolbox.util

import java.awt.Frame
import java.awt.Window

object AppWindowBridge {
    private var frame: Frame? = null
    private var lastState: Int = Frame.NORMAL

    fun register(window: Window) {
        frame = window as? Frame
    }

    fun hideForCapture() {
        val f = frame ?: return
        lastState = f.extendedState
        f.extendedState = Frame.ICONIFIED
    }

    fun restoreAfterCapture() {
        val f = frame ?: return
        f.extendedState = lastState
        f.toFront()
    }
}
