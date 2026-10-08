package com.toolbox.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class DiffRowKind { EQUAL, CHANGE, DELETE, INSERT, SEPARATOR }

data class DiffRow(
    val kind: DiffRowKind,
    val leftNo: Int? = null,
    val leftText: String? = null,
    val rightNo: Int? = null,
    val rightText: String? = null
)

data class DiffDelta(
    val sourcePos: Int,
    val sourceLines: List<String>,
    val targetPos: Int,
    val targetLines: List<String>
)

private val DiffRed = Color(0xFFC62828)
private val DiffGreen = Color(0xFF2E7D32)
private val DiffRedBg = Color(0x33C62828)
private val DiffGreenBg = Color(0x332E7D32)
private val SeparatorBg = Color(0xFFF0F0F0)

fun buildDiffRows(
    linesA: List<String>,
    linesB: List<String>,
    deltas: List<DiffDelta>
): List<DiffRow> {
    val rows = mutableListOf<DiffRow>()
    var srcPos = 0
    var tgtPos = 0
    for (d in deltas.sortedBy { it.sourcePos }) {
        val equalCount = d.sourcePos - srcPos
        for (i in 0 until equalCount) {
            rows.add(
                DiffRow(
                    kind = DiffRowKind.EQUAL,
                    leftNo = srcPos + i + 1,
                    leftText = linesA[srcPos + i],
                    rightNo = tgtPos + i + 1,
                    rightText = linesB[tgtPos + i]
                )
            )
        }
        srcPos = d.sourcePos
        tgtPos = d.targetPos
        val n = maxOf(d.sourceLines.size, d.targetLines.size)
        for (i in 0 until n) {
            val l = d.sourceLines.getOrNull(i)
            val r = d.targetLines.getOrNull(i)
            rows.add(
                DiffRow(
                    kind = when {
                        l != null && r != null -> DiffRowKind.CHANGE
                        l != null -> DiffRowKind.DELETE
                        else -> DiffRowKind.INSERT
                    },
                    leftNo = if (l != null) srcPos + i + 1 else null,
                    leftText = l,
                    rightNo = if (r != null) tgtPos + i + 1 else null,
                    rightText = r
                )
            )
        }
        srcPos += d.sourceLines.size
        tgtPos += d.targetLines.size
    }
    while (srcPos < linesA.size && tgtPos < linesB.size) {
        rows.add(
            DiffRow(
                kind = DiffRowKind.EQUAL,
                leftNo = srcPos + 1,
                leftText = linesA[srcPos],
                rightNo = tgtPos + 1,
                rightText = linesB[tgtPos]
            )
        )
        srcPos++
        tgtPos++
    }
    return rows
}

fun collapseEqualRows(rows: List<DiffRow>, context: Int = 3): List<DiffRow> {
    if (rows.isEmpty()) return rows
    val keep = BooleanArray(rows.size)
    var hasChange = false
    for (i in rows.indices) {
        if (rows[i].kind != DiffRowKind.EQUAL) {
            hasChange = true
            for (j in maxOf(0, i - context)..minOf(rows.size - 1, i + context)) {
                keep[j] = true
            }
        }
    }
    if (!hasChange) return rows
    val out = mutableListOf<DiffRow>()
    var i = 0
    while (i < rows.size) {
        if (keep[i]) {
            out.add(rows[i])
            i++
        } else {
            val gapStart = i
            while (i < rows.size && !keep[i]) i++
            out.add(DiffRow(kind = DiffRowKind.SEPARATOR, leftText = "⋯ ${i - gapStart} 行相同内容已折叠 ⋯"))
        }
    }
    return out
}

@Composable
fun SideBySideDiff(
    rows: List<DiffRow>,
    modifier: Modifier = Modifier,
    leftTitle: String = "文件 A",
    rightTitle: String = "文件 B"
) {
    Column(modifier = modifier) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = leftTitle,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f).padding(horizontal = 6.dp, vertical = 4.dp)
            )
            Text(
                text = rightTitle,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f).padding(horizontal = 6.dp, vertical = 4.dp)
            )
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            items(rows) { row ->
                DiffRowLine(row)
            }
        }
    }
}

@Composable
private fun DiffRowLine(row: DiffRow) {
    if (row.kind == DiffRowKind.SEPARATOR) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SeparatorBg)
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = row.leftText ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    Row(modifier = Modifier.fillMaxWidth()) {
        DiffSideCell(
            no = row.leftNo,
            text = row.leftText,
            background = when (row.kind) {
                DiffRowKind.DELETE, DiffRowKind.CHANGE -> DiffRedBg
                DiffRowKind.INSERT -> SeparatorBg
                else -> Color.Transparent
            },
            textColor = when (row.kind) {
                DiffRowKind.DELETE, DiffRowKind.CHANGE -> DiffRed
                else -> MaterialTheme.colorScheme.onSurface
            }
        )
        DiffSideCell(
            no = row.rightNo,
            text = row.rightText,
            background = when (row.kind) {
                DiffRowKind.INSERT, DiffRowKind.CHANGE -> DiffGreenBg
                DiffRowKind.DELETE -> SeparatorBg
                else -> Color.Transparent
            },
            textColor = when (row.kind) {
                DiffRowKind.INSERT, DiffRowKind.CHANGE -> DiffGreen
                else -> MaterialTheme.colorScheme.onSurface
            }
        )
    }
}

@Composable
private fun RowScope.DiffSideCell(
    no: Int?,
    text: String?,
    background: Color,
    textColor: Color
) {
    Row(
        modifier = Modifier
            .weight(1f)
            .background(background)
            .padding(horizontal = 6.dp, vertical = 3.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = no?.toString() ?: "",
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(40.dp)
        )
        Text(
            text = text ?: "",
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = textColor,
            modifier = Modifier.weight(1f)
        )
    }
}
