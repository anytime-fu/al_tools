package com.toolbox.util.onnx

import ai.onnxruntime.OnnxJavaType
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.TensorInfo
import java.awt.Color
import java.awt.Graphics2D
import java.awt.Rectangle
import java.awt.image.BufferedImage
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import kotlin.math.max
import kotlin.math.min

object MaskOps {

    fun create(width: Int, height: Int): BufferedImage {
        val mask = BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY)
        clear(mask)
        return mask
    }

    fun clear(mask: BufferedImage) {
        val g = mask.createGraphics()
        g.color = Color.BLACK
        g.fillRect(0, 0, mask.width, mask.height)
        g.dispose()
    }

    fun fillRect(mask: BufferedImage, x: Int, y: Int, w: Int, h: Int, value: Int = 255) {
        val g = mask.createGraphics()
        g.color = Color(value, value, value)
        g.fillRect(x, y, w, h)
        g.dispose()
    }

    fun stampCircle(mask: BufferedImage, cx: Int, cy: Int, radius: Int, value: Int = 255) {
        val g = mask.createGraphics()
        g.color = Color(value, value, value)
        g.fillOval(cx - radius, cy - radius, radius * 2, radius * 2)
        g.dispose()
    }

    fun bounds(mask: BufferedImage): Rectangle? {
        var minX = mask.width
        var minY = mask.height
        var maxX = -1
        var maxY = -1
        val raster = mask.raster
        for (y in 0 until mask.height) {
            for (x in 0 until mask.width) {
                if (raster.getSample(x, y, 0) > 127) {
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                }
            }
        }
        if (maxX < 0) return null
        return Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1)
    }

    fun coverage(mask: BufferedImage): Int {
        var count = 0
        val raster = mask.raster
        for (y in 0 until mask.height) {
            for (x in 0 until mask.width) {
                if (raster.getSample(x, y, 0) > 127) count++
            }
        }
        return count
    }
}

object AiInpainter {

    private const val MAX_PATCH_SIDE = 1024

    private var session: OrtSession? = null
    private var sessionPath: String? = null
    private var sessionMigan: Boolean = true
    private var activeProvider: String = "CPU"

    val providerLabel: String get() = activeProvider

    @Synchronized
    fun ensureSession(modelPath: String): OrtSession {
        val existing = session
        if (existing != null && sessionPath == modelPath) return existing
        existing?.close()
        val env = OrtEnvironment.getEnvironment()
        val opts = OrtSession.SessionOptions().apply {
            setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
            setIntraOpNumThreads(max(1, Runtime.getRuntime().availableProcessors() / 2))
        }
        activeProvider = "CPU"
        runCatching { opts.addDirectML(0) }.onSuccess { activeProvider = "DirectML" }
        if (activeProvider == "CPU") {
            runCatching { opts.addCUDA(0) }.onSuccess { activeProvider = "CUDA" }
        }
        val created = env.createSession(modelPath, opts)
        session = created
        sessionPath = modelPath
        val imageInfo = created.inputInfo.entries.firstOrNull {
            it.key.contains("image", true) || it.value.info is TensorInfo
        }?.value?.info as? TensorInfo
        sessionMigan = imageInfo?.type == OnnxJavaType.UINT8
        return created
    }

    @Synchronized
    fun closeSession() {
        runCatching { session?.close() }
        session = null
        sessionPath = null
    }

    fun inpaint(
        image: BufferedImage,
        mask: BufferedImage,
        modelPath: String,
        onStage: (String) -> Unit = {}
    ): BufferedImage {
        require(image.width == mask.width && image.height == mask.height) {
            "图片与标记尺寸不一致"
        }
        if (MaskOps.coverage(mask) == 0) return image

        onStage("准备修复区域…")
        val bounds = MaskOps.bounds(mask)!!
        val patchRect = roundToEight(expandToBounds(bounds, image.width, image.height), image.width, image.height)

        val patchImage = cropCopy(image, patchRect)
        val patchMask = cropCopy(mask, patchRect)

        var workImage = patchImage
        var workMask = patchMask
        val maxSide = max(patchImage.width, patchImage.height)
        if (maxSide > MAX_PATCH_SIDE) {
            val scale = MAX_PATCH_SIDE.toDouble() / maxSide
            val w = roundUp8((patchImage.width * scale).toInt())
            val h = roundUp8((patchImage.height * scale).toInt())
            workImage = scaleTo(patchImage, w, h)
            workMask = scaleTo(patchMask, w, h)
        }

        onStage("AI 推理中（${activeProvider}）…")
        val result = runModel(workImage, workMask, modelPath)

        val restoredPatch = scaleTo(result, patchRect.width, patchRect.height)
        val output = BufferedImage(image.width, image.height, BufferedImage.TYPE_INT_RGB)
        val copyG = output.createGraphics()
        copyG.drawImage(image, 0, 0, null)
        copyG.dispose()

        val outRaster = output.raster
        val restoredRaster = restoredPatch.raster
        val maskRaster = patchMask.raster
        for (y in 0 until patchRect.height) {
            for (x in 0 until patchRect.width) {
                if (maskRaster.getSample(x, y, 0) > 127) {
                    for (c in 0 until 3) {
                        outRaster.setSample(
                            patchRect.x + x, patchRect.y + y, c,
                            restoredRaster.getSample(x, y, c)
                        )
                    }
                }
            }
        }
        onStage("完成")
        return output
    }

    private fun runModel(
        inputImage: BufferedImage,
        inputMask: BufferedImage,
        modelPath: String
    ): BufferedImage {
        val session = ensureSession(modelPath)
        val env = OrtEnvironment.getEnvironment()
        val w = inputImage.width
        val h = inputImage.height

        val inputNames = session.inputNames.toList()
        val imageName = inputNames.firstOrNull { it.contains("image", true) } ?: inputNames.first()
        val maskName = inputNames.firstOrNull { it.contains("mask", true) }
            ?: inputNames.getOrElse(1) { inputNames.first() }

        if (sessionMigan) {
            val imageBuffer = ByteBuffer.allocate(3 * w * h)
            for (c in 0 until 3) {
                for (y in 0 until h) {
                    for (x in 0 until w) {
                        imageBuffer.put(inputImage.raster.getSample(x, y, c).toByte())
                    }
                }
            }
            val maskBuffer = ByteBuffer.allocate(w * h)
            for (y in 0 until h) {
                for (x in 0 until w) {
                    maskBuffer.put(if (inputMask.raster.getSample(x, y, 0) > 127) 0.toByte() else 255.toByte())
                }
            }
            imageBuffer.rewind()
            maskBuffer.rewind()
            val imgT = OnnxTensor.createTensor(env, imageBuffer, longArrayOf(1, 3, h.toLong(), w.toLong()), OnnxJavaType.UINT8)
            val mskT = OnnxTensor.createTensor(env, maskBuffer, longArrayOf(1, 1, h.toLong(), w.toLong()), OnnxJavaType.UINT8)
            imgT.use { img ->
                mskT.use { msk ->
                    session.run(mapOf(imageName to img, maskName to msk)).use { results ->
                        @Suppress("UNCHECKED_CAST")
                        val data = results[0].value as Array<Array<Array<ByteArray>>>
                        val out = BufferedImage(w, h, BufferedImage.TYPE_INT_RGB)
                        for (y in 0 until h) {
                            for (x in 0 until w) {
                                val r = data[0][0][y][x].toInt() and 0xFF
                                val g = data[0][1][y][x].toInt() and 0xFF
                                val b = data[0][2][y][x].toInt() and 0xFF
                                out.setRGB(x, y, (r shl 16) or (g shl 8) or b)
                            }
                        }
                        return out
                    }
                }
            }
        }

        val imageBuffer = FloatBuffer.allocate(3 * w * h)
        for (c in 0 until 3) {
            for (y in 0 until h) {
                for (x in 0 until w) {
                    imageBuffer.put(inputImage.raster.getSample(x, y, c) / 255f)
                }
            }
        }
        val maskBuffer = FloatBuffer.allocate(w * h)
        for (y in 0 until h) {
            for (x in 0 until w) {
                maskBuffer.put(if (inputMask.raster.getSample(x, y, 0) > 127) 1f else 0f)
            }
        }
        imageBuffer.rewind()
        maskBuffer.rewind()
        val imgT = OnnxTensor.createTensor(env, imageBuffer, longArrayOf(1, 3, h.toLong(), w.toLong()))
        val mskT = OnnxTensor.createTensor(env, maskBuffer, longArrayOf(1, 1, h.toLong(), w.toLong()))
        imgT.use { img ->
            mskT.use { msk ->
                session.run(mapOf(imageName to img, maskName to msk)).use { results ->
                    @Suppress("UNCHECKED_CAST")
                    val data = results[0].value as Array<Array<Array<FloatArray>>>
                    var maxValue = 0f
                    for (c in 0 until 3) {
                        for (y in 0 until h) {
                            for (x in 0 until w) {
                                val v = data[0][c][y][x]
                                if (v > maxValue) maxValue = v
                            }
                        }
                    }
                    val factor = if (maxValue <= 2f) 255f else 1f
                    val out = BufferedImage(w, h, BufferedImage.TYPE_INT_RGB)
                    for (y in 0 until h) {
                        for (x in 0 until w) {
                            val r = (data[0][0][y][x] * factor).toInt().coerceIn(0, 255)
                            val g = (data[0][1][y][x] * factor).toInt().coerceIn(0, 255)
                            val b = (data[0][2][y][x] * factor).toInt().coerceIn(0, 255)
                            out.setRGB(x, y, (r shl 16) or (g shl 8) or b)
                        }
                    }
                    return out
                }
            }
        }
    }

    private fun roundUp8(v: Int): Int = ((v + 7) / 8) * 8

    private fun roundToEight(rect: Rectangle, imgW: Int, imgH: Int): Rectangle {
        val x2 = min(imgW, rect.x + roundUp8(rect.width))
        val y2 = min(imgH, rect.y + roundUp8(rect.height))
        return Rectangle(rect.x, rect.y, x2 - rect.x, y2 - rect.y)
    }

    private fun expandToBounds(bounds: Rectangle, imgW: Int, imgH: Int): Rectangle {
        if (bounds.width * bounds.height > (imgW.toLong() * imgH * 3) / 5) {
            return Rectangle(0, 0, imgW, imgH)
        }
        val margin = max(bounds.width, bounds.height) / 2
        val x = max(0, bounds.x - margin)
        val y = max(0, bounds.y - margin)
        val right = min(imgW, bounds.x + bounds.width + margin)
        val bottom = min(imgH, bounds.y + bounds.height + margin)
        return Rectangle(x, y, right - x, bottom - y)
    }

    private fun cropCopy(image: BufferedImage, rect: Rectangle): BufferedImage {
        val out = BufferedImage(rect.width, rect.height, BufferedImage.TYPE_INT_RGB)
        val g: Graphics2D = out.createGraphics()
        g.drawImage(image, 0, 0, rect.width, rect.height, rect.x, rect.y, rect.x + rect.width, rect.y + rect.height, null)
        g.dispose()
        return out
    }

    private fun scaleTo(image: BufferedImage, width: Int, height: Int): BufferedImage {
        if (image.width == width && image.height == height) return image
        val out = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val g = out.createGraphics()
        g.drawImage(image, 0, 0, width, height, null)
        g.dispose()
        return out
    }
}