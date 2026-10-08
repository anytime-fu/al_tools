package com.toolbox.ui.file.compare

import com.toolbox.ui.components.AppFilterChip

import com.toolbox.ui.components.AppOutlinedButton

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.difflib.patch.DeltaType
import com.toolbox.ui.components.DiffDelta
import com.toolbox.ui.components.DiffRow
import com.toolbox.ui.components.SideBySideDiff
import com.toolbox.ui.components.buildDiffRows
import com.toolbox.ui.components.collapseEqualRows
import com.toolbox.ui.components.fastDiffDeltas
import java.io.File
import java.io.FileInputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction
import javax.swing.JFileChooser
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val BUF_SIZE = 64 * 1024
private const val MAX_TEXT_SIZE = 100L * 1024 * 1024
private const val HEX_CONTEXT_BYTES = 48

private enum class CompareMode(val label: String) {
    AUTO("自动检测"),
    TEXT("文本对比"),
    BINARY("二进制对比")
}

private data class CompareDelta(
    val type: DeltaType,
    val position: Int,
    val targetPosition: Int,
    val sourceLines: List<String>,
    val targetLines: List<String>
)

private sealed class CompareOutcome {
    object Identical : CompareOutcome()
    data class TextDiff(
        val deltas: List<CompareDelta>,
        val rows: List<DiffRow>,
        val charsetLabel: String
    ) : CompareOutcome()
    data class BinaryDiff(
        val firstOffset: Long,
        val sizeA: Long,
        val sizeB: Long
    ) : CompareOutcome()
    data class Error(val message: String) : CompareOutcome()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileCompareScreen(onBack: () -> Unit) {
    var pathA by remember { mutableStateOf("") }
    var pathB by remember { mutableStateOf("") }
    var outcome by remember { mutableStateOf<CompareOutcome?>(null) }
    var comparing by remember { mutableStateOf(false) }
    var phase by remember { mutableStateOf("") }
    var compareJob by remember { mutableStateOf<Job?>(null) }
    var showFullText by remember { mutableStateOf(false) }
    var mode by remember { mutableStateOf(CompareMode.AUTO) }
    val scope = rememberCoroutineScope()

    fun startCompare() {
        if (comparing) return
        comparing = true
        outcome = null
        phase = "准备中..."
        val chosenMode = mode
        compareJob = scope.launch {
            var cancelled = false
            try {
                val result = compareFiles(File(pathA), File(pathB), chosenMode) { phase = it }
                outcome = result
            } catch (e: CancellationException) {
                cancelled = true
            } finally {
                comparing = false
                phase = if (cancelled) "已取消对比" else ""
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("文件对比") },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            FilePickerRow("文件 A", pathA) { pathA = it }
            Spacer(modifier = Modifier.height(8.dp))
            FilePickerRow("文件 B", pathB) { pathB = it }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("对比模式", style = MaterialTheme.typography.labelMedium)
                CompareMode.values().forEach { m ->
                    AppFilterChip(
                        selected = mode == m,
                        onClick = { mode = m },
                        label = { Text(m.label) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        if (comparing) {
                            compareJob?.cancel()
                        } else {
                            startCompare()
                        }
                    },
                    colors = if (comparing) {
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    } else {
                        ButtonDefaults.buttonColors()
                    }
                ) {
                    Icon(
                        if (comparing) Icons.Default.Close else Icons.Default.CompareArrows,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (comparing) "停止" else "对比")
                }

                if (comparing) {
                    Spacer(modifier = Modifier.width(8.dp))
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = phase,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (val result = outcome) {
                null -> {}
                is CompareOutcome.Error -> {
                    Text(
                        text = result.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                is CompareOutcome.Identical -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF2E7D32).copy(alpha = 0.12f)
                        )
                    ) {
                        Text(
                            text = "两个文件完全一致。",
                            modifier = Modifier.padding(16.dp),
                            color = Color(0xFF2E7D32)
                        )
                    }
                }
                is CompareOutcome.BinaryDiff -> {
                    BinaryDiffView(
                        result = result,
                        a = File(pathA),
                        b = File(pathB),
                        onViewAsText = {
                            mode = CompareMode.TEXT
                            startCompare()
                        }
                    )
                }
                is CompareOutcome.TextDiff -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val inserted = result.deltas.count { it.type == DeltaType.INSERT }
                        val deleted = result.deltas.count { it.type == DeltaType.DELETE }
                        val changed = result.deltas.count { it.type == DeltaType.CHANGE }
                        Text(
                            text = "源文本对照（${result.charsetLabel}）：修改 $changed 处 / 新增 $inserted 行 / 删除 $deleted 行",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = showFullText,
                                onCheckedChange = { showFullText = it }
                            )
                            Text("显示完整内容", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    SideBySideDiff(
                        rows = if (showFullText) result.rows else collapseEqualRows(result.rows),
                        modifier = Modifier.fillMaxSize(),
                        leftTitle = "文件 A · ${File(pathA).name}",
                        rightTitle = "文件 B · ${File(pathB).name}"
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilePickerRow(label: String, path: String, onPathChange: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            Icons.Default.InsertDriveFile,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        OutlinedTextField(
            value = path,
            onValueChange = onPathChange,
            modifier = Modifier.weight(1f),
            label = { Text(label) },
            placeholder = { Text("选择文件路径") },
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace)
        )
        AppOutlinedButton(
            onClick = {
                val chooser = JFileChooser()
                chooser.fileSelectionMode = JFileChooser.FILES_ONLY
                if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    onPathChange(chooser.selectedFile.absolutePath)
                }
            }
        ) {
            Text("浏览...")
        }
    }
}

@Composable
private fun BinaryDiffView(
    result: CompareOutcome.BinaryDiff,
    a: File,
    b: File,
    onViewAsText: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFF9A825).copy(alpha = 0.12f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "二进制对照（文件被识别为二进制内容）",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color(0xFFF57F17)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("文件 A：${a.name} · ${result.sizeA} 字节", style = MaterialTheme.typography.bodySmall)
                Text("文件 B：${b.name} · ${result.sizeB} 字节", style = MaterialTheme.typography.bodySmall)
                Text(
                    text = "首个差异位置：偏移 ${result.firstOffset}（0x${result.firstOffset.toString(16).uppercase()}）",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                AppOutlinedButton(onClick = onViewAsText) {
                    Icon(Icons.Default.Article, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("按文本对照查看")
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "首个差异处字节对照（上下两行分别为文件 A / 文件 B，红底为差异字节）",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))

        val startOffset = maxOf(0L, result.firstOffset - HEX_CONTEXT_BYTES)
        val hexRows = remember(result.firstOffset) {
            readHexWindow(a, b, startOffset, HEX_CONTEXT_BYTES * 2 + 32)
        }
        hexRows.forEach { row ->
            HexRowView(row, result.firstOffset)
        }
    }
}

private data class HexRow(
    val offset: Long,
    val bytesA: ByteArray,
    val bytesB: ByteArray
)

private fun readHexWindow(a: File, b: File, start: Long, length: Int): List<HexRow> {
    val rows = mutableListOf<HexRow>()
    try {
        RandomAccessFile(a, "r").use { ra ->
            RandomAccessFile(b, "r").use { rb ->
                val bufA = ByteArray(16)
                val bufB = ByteArray(16)
                var offset = start
                val end = start + length
                while (offset < end) {
                    ra.seek(offset)
                    rb.seek(offset)
                    val na = ra.read(bufA)
                    val nb = rb.read(bufB)
                    if (na < 0 && nb < 0) break
                    rows.add(
                        HexRow(
                            offset = offset,
                            bytesA = if (na > 0) bufA.copyOf(na) else ByteArray(0),
                            bytesB = if (nb > 0) bufB.copyOf(nb) else ByteArray(0)
                        )
                    )
                    offset += 16
                }
            }
        }
    } catch (_: Exception) {
    }
    return rows
}

@Composable
private fun HexRowView(row: HexRow, diffOffset: Long) {
    val mono = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp)
    val isDiffRow = diffOffset >= row.offset && diffOffset < row.offset + 16

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "0x${row.offset.toString(16).uppercase().padStart(8, '0')}",
            style = mono,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 12.dp)
        )
        HexLine(row.bytesA, row.offset, diffOffset, isDiffRow, mono)
        Spacer(modifier = Modifier.width(16.dp))
        HexLine(row.bytesB, row.offset, diffOffset, isDiffRow, mono)
    }
}

@Composable
private fun HexLine(
    bytes: ByteArray,
    baseOffset: Long,
    diffOffset: Long,
    isDiffRow: Boolean,
    style: TextStyle
) {
    Row {
        for (i in bytes.indices) {
            val offset = baseOffset + i
            val isDiff = isDiffRow && offset == diffOffset
            Text(
                text = bytes[i].toHex(),
                style = style,
                color = if (isDiff) Color(0xFFC62828) else MaterialTheme.colorScheme.onSurface,
                modifier = if (isDiff) {
                    Modifier.background(Color(0x33C62828)).padding(horizontal = 1.dp)
                } else {
                    Modifier.padding(horizontal = 1.dp)
                }
            )
        }
    }
}

private fun Byte.toHex(): String = String.format("%02X", this)

private suspend fun compareFiles(
    a: File,
    b: File,
    mode: CompareMode,
    onPhase: (String) -> Unit
): CompareOutcome {
    if (!a.isFile || !b.isFile) return CompareOutcome.Error("文件不存在或不是有效文件")
    val sizeA = a.length()
    val sizeB = b.length()

    val charsetA = sniffTextCharset(a)
    val charsetB = sniffTextCharset(b)
    val autoText = charsetA != null && charsetB != null &&
        sizeA <= MAX_TEXT_SIZE && sizeB <= MAX_TEXT_SIZE
    val useText = mode == CompareMode.TEXT || (mode == CompareMode.AUTO && autoText)

    return if (useText) {
        val csA = charsetA ?: Charsets.UTF_8
        val csB = charsetB ?: Charsets.UTF_8

        onPhase("正在读取 ${a.name}...")
        val linesA = withContext(Dispatchers.IO) {
            readLines(a, csA)
        }
        coroutineContext.ensureActive()

        onPhase("正在读取 ${b.name}...")
        val linesB = withContext(Dispatchers.IO) {
            readLines(b, csB)
        }
        coroutineContext.ensureActive()

        if (linesA == linesB) return CompareOutcome.Identical

        onPhase("正在计算差异（${linesA.size} 行 vs ${linesB.size} 行）...")
        val deltas = withContext(Dispatchers.Default) {
            fastDiffDeltas(linesA, linesB)
        }
        onPhase("")

        val deltaRows = withContext(Dispatchers.Default) {
            buildDiffRows(linesA, linesB, deltas)
        }
        val compareDeltas = deltas.map { d ->
            CompareDelta(
                type = when {
                    d.sourceLines.isEmpty() -> DeltaType.INSERT
                    d.targetLines.isEmpty() -> DeltaType.DELETE
                    else -> DeltaType.CHANGE
                },
                position = d.sourcePos,
                targetPosition = d.targetPos,
                sourceLines = d.sourceLines,
                targetLines = d.targetLines
            )
        }
        val charsetLabel = if (csA == csB) csA.name() else "${csA.name()} / ${csB.name()}"
        CompareOutcome.TextDiff(deltas = compareDeltas, rows = deltaRows, charsetLabel = charsetLabel)
    } else {
        compareBinary(a, b, sizeA, sizeB, onPhase)
    }
}

private fun readLines(file: File, charset: Charset): List<String> {
    val lines = file.bufferedReader(charset).use { it.readLines() }
    if (lines.isEmpty()) return lines
    return listOf(lines.first().removePrefix("\uFEFF")) + lines.drop(1)
}

private suspend fun compareBinary(
    a: File,
    b: File,
    sizeA: Long,
    sizeB: Long,
    onPhase: (String) -> Unit
): CompareOutcome {
    val bufA = ByteArray(BUF_SIZE)
    val bufB = ByteArray(BUF_SIZE)
    val total = maxOf(sizeA, sizeB)
    var offset = 0L
    var lastPaint = 0L

    FileInputStream(a).use { ia ->
        FileInputStream(b).use { ib ->
            while (true) {
                coroutineContext.ensureActive()

                val na = ia.read(bufA)
                val nb = ib.read(bufB)
                if (na < 0 && nb < 0) return CompareOutcome.Identical

                val overlap = minOf(if (na < 0) 0 else na, if (nb < 0) 0 else nb)
                for (i in 0 until overlap) {
                    if (bufA[i] != bufB[i]) {
                        return CompareOutcome.BinaryDiff(offset + i, sizeA, sizeB)
                    }
                }
                if (na != nb) {
                    return CompareOutcome.BinaryDiff(offset + overlap, sizeA, sizeB)
                }

                offset += overlap

                val now = System.currentTimeMillis()
                if (now - lastPaint > 100) {
                    lastPaint = now
                    onPhase("已比较 ${offset / 1024} / ${total / 1024} KB")
                }
            }
        }
    }
}

private fun sniffTextCharset(file: File): Charset? {
    val sample = try {
        file.inputStream().use { input ->
            val buf = ByteArray(65536)
            var off = 0
            while (off < buf.size) {
                val n = input.read(buf, off, buf.size - off)
                if (n < 0) break
                off += n
            }
            buf.copyOf(off)
        }
    } catch (_: Exception) {
        return null
    }

    if (sample.isEmpty()) return Charsets.UTF_8

    if (sample.size >= 2) {
        if (sample[0] == 0xFF.toByte() && sample[1] == 0xFE.toByte()) return Charsets.UTF_16LE
        if (sample[0] == 0xFE.toByte() && sample[1] == 0xFF.toByte()) return Charsets.UTF_16BE
    }
    if (sample.size >= 3 &&
        sample[0] == 0xEF.toByte() && sample[1] == 0xBB.toByte() && sample[2] == 0xBF.toByte()
    ) {
        return Charsets.UTF_8
    }

    var nulEven = 0
    var nulOdd = 0
    var suspicious = 0
    for (i in sample.indices) {
        val v = sample[i].toInt() and 0xFF
        if (v == 0) {
            if (i % 2 == 0) nulEven++ else nulOdd++
        } else if ((v in 1..8) || v == 0x0B || (v in 0x0E..0x1F) || v == 0x7F) {
            suspicious++
        }
    }

    val nuls = nulEven + nulOdd
    if (nuls > sample.size / 8) {
        return if (nulOdd >= nulEven) Charsets.UTF_16LE else Charsets.UTF_16BE
    }
    if (nuls > 0) return null
    if (suspicious > sample.size / 20) return null

    fun decodable(cs: Charset): Boolean = try {
        cs.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(sample))
        true
    } catch (_: Exception) {
        false
    }

    return when {
        decodable(Charsets.UTF_8) -> Charsets.UTF_8
        decodable(Charset.forName("GBK")) -> Charset.forName("GBK")
        else -> Charsets.UTF_8
    }
}
