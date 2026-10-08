package com.toolbox.ui.image

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.loadImageBitmap
import java.awt.Color
import java.awt.Graphics2D
import java.awt.GraphicsEnvironment
import java.awt.Rectangle
import java.awt.RenderingHints
import java.awt.Robot
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.awt.datatransfer.Transferable
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam
import kotlinx.coroutines.flow.MutableStateFlow

object ImageDropBridge {
    val dropped = MutableStateFlow<List<File>>(emptyList())

    fun offer(files: List<File>) {
        if (files.isNotEmpty()) dropped.value = files
    }

    fun clear() {
        dropped.value = emptyList()
    }
}

object ImageIo {
    val extensions = setOf("png", "jpg", "jpeg", "bmp", "gif", "webp")

    fun isImageFile(file: File): Boolean =
        file.isFile && file.extension.lowercase() in extensions

    fun formatOf(file: File): String = when (file.extension.lowercase()) {
        "jpeg" -> "jpg"
        else -> file.extension.lowercase()
    }

    fun writerName(format: String): String = when (format) {
        "jpg", "jpeg" -> "jpg"
        else -> format
    }

    fun canWrite(format: String): Boolean =
        ImageIO.getImageWritersByFormatName(writerName(format)).hasNext()

    fun read(file: File): BufferedImage? = ImageIO.read(file)

    fun flattenToRgb(image: BufferedImage): BufferedImage {
        if (image.type == BufferedImage.TYPE_INT_RGB) return image
        val out = BufferedImage(image.width, image.height, BufferedImage.TYPE_INT_RGB)
        val g = out.createGraphics()
        g.color = Color.WHITE
        g.fillRect(0, 0, image.width, image.height)
        g.drawImage(image, 0, 0, null)
        g.dispose()
        return out
    }

    fun write(image: BufferedImage, file: File, format: String, quality: Float? = null) {
        val fmt = writerName(format)
        val iterator = ImageIO.getImageWritersByFormatName(fmt)
        require(iterator.hasNext()) { "当前环境不支持写入 $format 格式" }
        val writer = iterator.next()
        try {
            val target = if (fmt == "jpg") flattenToRgb(image) else image
            val ios = ImageIO.createImageOutputStream(file) ?: error("无法创建输出文件")
            writer.output = ios
            val param = writer.defaultWriteParam
            if (quality != null && param.canWriteCompressed()) {
                runCatching {
                    param.compressionMode = ImageWriteParam.MODE_EXPLICIT
                    param.compressionQuality = quality.coerceIn(0f, 1f)
                }.recoverCatching {
                    param.compressionMode = ImageWriteParam.MODE_EXPLICIT
                    val types = param.compressionTypes
                    if (!types.isNullOrEmpty()) {
                        param.compressionType = types.first()
                        param.compressionQuality = quality.coerceIn(0f, 1f)
                    }
                }
            }
            writer.write(null, IIOImage(target, null, null), param)
            ios.close()
        } finally {
            writer.dispose()
        }
    }

    fun resizeMaxSide(image: BufferedImage, maxSide: Int): BufferedImage {
        val longest = maxOf(image.width, image.height)
        if (maxSide <= 0 || longest <= maxSide) return image
        val scale = maxSide.toDouble() / longest
        val w = maxOf(1, (image.width * scale).toInt())
        val h = maxOf(1, (image.height * scale).toInt())
        return scaleTo(image, w, h)
    }

    fun scaleTo(image: BufferedImage, width: Int, height: Int): BufferedImage {
        val out = BufferedImage(width, height, if (image.type == 0) BufferedImage.TYPE_INT_ARGB else image.type)
        val g = out.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        g.drawImage(image, 0, 0, width, height, null)
        g.dispose()
        return out
    }

    fun formatSize(bytes: Long): String = when {
        bytes >= 1024 * 1024 -> "%.2f MB".format(bytes / 1024.0 / 1024.0)
        bytes >= 1024 -> "%.1f KB".format(bytes / 1024.0)
        else -> "$bytes B"
    }

    fun uniqueTarget(dir: File, baseName: String, ext: String): File {
        var candidate = File(dir, "$baseName.$ext")
        var index = 1
        while (candidate.exists()) {
            candidate = File(dir, "${baseName}_$index.$ext")
            index++
        }
        return candidate
    }

    fun toImageBitmap(image: BufferedImage): ImageBitmap {
        val bytes = ByteArrayOutputStream()
        ImageIO.write(image, "png", bytes)
        return loadImageBitmap(ByteArrayInputStream(bytes.toByteArray()))
    }

    fun captureScreen(): BufferedImage {
        val ge = GraphicsEnvironment.getLocalGraphicsEnvironment()
        val bounds = ge.screenDevices.fold(Rectangle()) { acc, device ->
            acc.union(device.defaultConfiguration.bounds)
        }
        return Robot().createScreenCapture(bounds)
    }

    fun copyTextToClipboard(text: String) {
        Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
    }

    fun copyImageToClipboard(image: BufferedImage) {
        val transferable = object : Transferable {
            override fun getTransferDataFlavors(): Array<DataFlavor> = arrayOf(DataFlavor.imageFlavor)
            override fun isDataFlavorSupported(flavor: DataFlavor): Boolean = flavor == DataFlavor.imageFlavor
            override fun getTransferData(flavor: DataFlavor): Any = image
        }
        Toolkit.getDefaultToolkit().systemClipboard.setContents(transferable, null)
    }

    fun colorToHex(r: Int, g: Int, b: Int): String = "#%02X%02X%02X".format(r, g, b)

    data class FitLayout(
        val scale: Float,
        val offsetX: Float,
        val offsetY: Float,
        val dispW: Float,
        val dispH: Float
    ) {
        fun toImageX(displayX: Float): Float = (displayX - offsetX) / scale
        fun toImageY(displayY: Float): Float = (displayY - offsetY) / scale
        fun toDisplayX(imageX: Float): Float = offsetX + imageX * scale
        fun toDisplayY(imageY: Float): Float = offsetY + imageY * scale
    }

    fun fitLayout(boxW: Float, boxH: Float, imgW: Int, imgH: Int): FitLayout {
        if (imgW <= 0 || imgH <= 0 || boxW <= 0f || boxH <= 0f) {
            return FitLayout(1f, 0f, 0f, 0f, 0f)
        }
        val scale = minOf(boxW / imgW, boxH / imgH)
        val dispW = imgW * scale
        val dispH = imgH * scale
        return FitLayout(
            scale = scale,
            offsetX = (boxW - dispW) / 2f,
            offsetY = (boxH - dispH) / 2f,
            dispW = dispW,
            dispH = dispH
        )
    }
}
