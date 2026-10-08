package com.toolbox

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.toolbox.di.initKoin
import com.toolbox.ui.MainApp
import com.toolbox.ui.image.ImageDropBridge
import com.toolbox.ui.image.ImageIo
import com.toolbox.ui.theme.ToolboxTheme
import com.toolbox.ui.uninstall.UninstallScreen
import com.toolbox.util.AppWindowBridge
import com.toolbox.util.UninstallGuard
import java.awt.datatransfer.DataFlavor
import java.awt.dnd.DnDConstants
import java.awt.dnd.DropTarget
import java.awt.dnd.DropTargetDragEvent
import java.awt.dnd.DropTargetDropEvent
import java.awt.dnd.DropTargetEvent
import java.awt.dnd.DropTargetListener
import java.io.File

fun main(args: Array<String>) {
    UninstallGuard.installHook()
    if (UninstallGuard.isGuardRequest(args)) {
        uninstallGuardMain()
    } else {
        appMain()
    }
}

private fun uninstallGuardMain() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "AI 工具箱",
        state = rememberWindowState(
            width = 720.dp,
            height = 680.dp,
            position = WindowPosition(Alignment.Center)
        )
    ) {
        ToolboxTheme {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .verticalScroll(rememberScrollState()),
                contentAlignment = Alignment.Center
            ) {
                UninstallScreen(
                    onKeep = { exitApplication() },
                    onUninstallRequested = {
                        Thread {
                            Thread.sleep(1500)
                            kotlin.system.exitProcess(0)
                        }.start()
                    }
                )
            }
        }
    }
}

private fun appMain() = application {
    initKoin()
    
    val windowState = rememberWindowState(
        width = 1200.dp,
        height = 800.dp,
        position = WindowPosition(Alignment.Center)
    )
    
    Window(
        onCloseRequest = ::exitApplication,
        title = "AI 工具箱",
        state = windowState
    ) {
        LaunchedEffect(Unit) {
            AppWindowBridge.register(window)
            runCatching {
                window.dropTarget = DropTarget().apply {
                    addDropTargetListener(object : DropTargetListener {
                        override fun dragEnter(dtde: DropTargetDragEvent) {
                            dtde.acceptDrag(DnDConstants.ACTION_COPY)
                        }

                        override fun dragOver(dtde: DropTargetDragEvent) {
                            dtde.acceptDrag(DnDConstants.ACTION_COPY)
                        }

                        override fun dropActionChanged(dtde: DropTargetDragEvent) {
                            dtde.acceptDrag(DnDConstants.ACTION_COPY)
                        }

                        override fun dragExit(dte: DropTargetEvent) {}

                        override fun drop(dtde: DropTargetDropEvent) {
                            dtde.acceptDrop(DnDConstants.ACTION_COPY)
                            val files = runCatching {
                                (dtde.transferable.getTransferData(DataFlavor.javaFileListFlavor) as List<*>)
                                    .filterIsInstance<File>()
                            }.getOrDefault(emptyList())
                            val images = files.filter { ImageIo.isImageFile(it) }
                            if (images.isNotEmpty()) ImageDropBridge.offer(images)
                            dtde.dropComplete(true)
                        }
                    })
                }
            }
        }
        MainApp()
    }
}
