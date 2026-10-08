package com.toolbox.ui.developer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorScreen(onBack: () -> Unit) {
    var hexInput by remember { mutableStateOf("#3F51B5") }
    var red by remember { mutableStateOf(63) }
    var green by remember { mutableStateOf(81) }
    var blue by remember { mutableStateOf(181) }
    var alpha by remember { mutableStateOf(255) }
    var hue by remember { mutableStateOf(231f) }
    var saturation by remember { mutableStateOf(0.48f) }
    var lightness by remember { mutableStateOf(0.48f) }

    fun updateFromRgb() {
        hexInput = "#%02X%02X%02X".format(red, green, blue)
        val hsl = rgbToHsl(red, green, blue)
        hue = hsl[0]
        saturation = hsl[1]
        lightness = hsl[2]
    }

    fun updateFromHex() {
        try {
            val hex = hexInput.removePrefix("#")
            if (hex.length == 6 || hex.length == 8) {
                red = hex.substring(0, 2).toInt(16)
                green = hex.substring(2, 4).toInt(16)
                blue = hex.substring(4, 6).toInt(16)
                if (hex.length == 8) {
                    alpha = hex.substring(6, 8).toInt(16)
                }
                val hsl = rgbToHsl(red, green, blue)
                hue = hsl[0]
                saturation = hsl[1]
                lightness = hsl[2]
            }
        } catch (e: Exception) {
            // 忽略无效输入
        }
    }

    fun updateFromHsl() {
        val rgb = hslToRgb(hue, saturation, lightness)
        red = rgb[0]
        green = rgb[1]
        blue = rgb[2]
        hexInput = "#%02X%02X%02X".format(red, green, blue)
    }

    // 初始化
    LaunchedEffect(Unit) {
        updateFromRgb()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("颜色工具") },
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
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 左侧：颜色预览和HEX输入
            Column(
                modifier = Modifier.width(250.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 颜色预览
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .background(
                            Color(red, green, blue),
                            MaterialTheme.shapes.medium
                        )
                        .border(
                            2.dp,
                            MaterialTheme.colorScheme.outline,
                            MaterialTheme.shapes.medium
                        )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // HEX 输入
                OutlinedTextField(
                    value = hexInput,
                    onValueChange = { 
                        hexInput = it
                        updateFromHex()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("HEX") },
                    singleLine = true,
                    textStyle = FontFamily.Monospace.let { MaterialTheme.typography.bodyLarge.copy(fontFamily = it) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 格式化输出
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        ColorFormatRow("RGB", "rgb($red, $green, $blue)")
                        ColorFormatRow("RGBA", "rgba($red, $green, $blue, ${alpha / 255f})")
                        ColorFormatRow("HEX", hexInput.uppercase())
                        ColorFormatRow("HSL", "hsl(${hue.toInt()}, ${(saturation * 100).toInt()}%, ${(lightness * 100).toInt()}%)")
                    }
                }
            }

            // 右侧：滑块控制
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // RGB 滑块
                Text(
                    text = "RGB",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))

                ColorSlider("R", red, 0, 255, Color.Red) { 
                    red = it
                    updateFromRgb()
                }
                ColorSlider("G", green, 0, 255, Color.Green) { 
                    green = it
                    updateFromRgb()
                }
                ColorSlider("B", blue, 0, 255, Color.Blue) { 
                    blue = it
                    updateFromRgb()
                }
                ColorSlider("A", alpha, 0, 255, Color.Gray) { 
                    alpha = it
                }

                Spacer(modifier = Modifier.height(24.dp))

                // HSL 滑块
                Text(
                    text = "HSL",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))

                HslSlider("H", hue, 0f, 360f, "°") { 
                    hue = it
                    updateFromHsl()
                }
                HslSlider("S", saturation, 0f, 1f, "%") { 
                    saturation = it
                    updateFromHsl()
                }
                HslSlider("L", lightness, 0f, 1f, "%") { 
                    lightness = it
                    updateFromHsl()
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 预设颜色
                Text(
                    text = "预设颜色",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                val presetColors = listOf(
                    "#F44336", "#E91E63", "#9C27B0", "#673AB7",
                    "#3F51B5", "#2196F3", "#03A9F4", "#00BCD4",
                    "#009688", "#4CAF50", "#8BC34A", "#CDDC39",
                    "#FFEB3B", "#FFC107", "#FF9800", "#FF5722"
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    presetColors.forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(
                                    Color(
                                        color.substring(1, 3).toInt(16),
                                        color.substring(3, 5).toInt(16),
                                        color.substring(5, 7).toInt(16)
                                    ),
                                    MaterialTheme.shapes.small
                                )
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.outline,
                                    MaterialTheme.shapes.small
                                )
                                .padding(0.dp)
                                .let { mod ->
                                    mod
                                }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorFormatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            IconButton(
                onClick = {
                    val selection = StringSelection(value)
                    Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, null)
                },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    Icons.Default.ContentCopy,
                    contentDescription = "复制",
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun ColorSlider(
    label: String,
    value: Int,
    min: Int,
    max: Int,
    color: Color,
    onValueChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(20.dp)
        )
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = min.toFloat()..max.toFloat(),
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = color,
                activeTrackColor = color
            )
        )
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(40.dp)
        )
    }
}

@Composable
private fun HslSlider(
    label: String,
    value: Float,
    min: Float,
    max: Float,
    unit: String,
    onValueChange: (Float) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(20.dp)
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = min..max,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = if (unit == "%") "${(value * 100).toInt()}$unit" else "${value.toInt()}$unit",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(50.dp)
        )
    }
}

private fun rgbToHsl(r: Int, g: Int, b: Int): FloatArray {
    val rf = r / 255f
    val gf = g / 255f
    val bf = b / 255f
    
    val max = maxOf(rf, gf, bf)
    val min = minOf(rf, gf, bf)
    val l = (max + min) / 2f
    
    if (max == min) {
        return floatArrayOf(0f, 0f, l)
    }
    
    val d = max - min
    val s = if (l > 0.5f) d / (2f - max - min) else d / (max + min)
    
    val h = when (max) {
        rf -> ((gf - bf) / d + (if (gf < bf) 6f else 0f)) * 60f
        gf -> ((bf - rf) / d + 2f) * 60f
        else -> ((rf - gf) / d + 4f) * 60f
    }
    
    return floatArrayOf(h, s, l)
}

private fun hslToRgb(h: Float, s: Float, l: Float): IntArray {
    if (s == 0f) {
        val v = (l * 255).toInt()
        return intArrayOf(v, v, v)
    }
    
    val q = if (l < 0.5f) l * (1f + s) else l + s - l * s
    val p = 2f * l - q
    val hNorm = h / 360f
    
    val r = hueToRgb(p, q, hNorm + 1f / 3f)
    val g = hueToRgb(p, q, hNorm)
    val b = hueToRgb(p, q, hNorm - 1f / 3f)
    
    return intArrayOf((r * 255).toInt(), (g * 255).toInt(), (b * 255).toInt())
}

private fun hueToRgb(p: Float, q: Float, t: Float): Float {
    var tVar = t
    if (tVar < 0f) tVar += 1f
    if (tVar > 1f) tVar -= 1f
    if (tVar < 1f / 6f) return p + (q - p) * 6f * tVar
    if (tVar < 1f / 2f) return q
    if (tVar < 2f / 3f) return p + (q - p) * (2f / 3f - tVar) * 6f
    return p
}