package com.toolbox.ui.components

import com.github.difflib.DiffUtils

private const val MAX_DIFF_MID_LINES = 100_000

fun fastDiffDeltas(linesA: List<String>, linesB: List<String>): List<DiffDelta> {
    if (linesA == linesB) return emptyList()

    val n = minOf(linesA.size, linesB.size)
    var prefix = 0
    while (prefix < n && linesA[prefix] == linesB[prefix]) prefix++
    var suffix = 0
    while (suffix < n - prefix && linesA[linesA.size - 1 - suffix] == linesB[linesB.size - 1 - suffix]) suffix++

    val midA = linesA.subList(prefix, linesA.size - suffix)
    val midB = linesB.subList(prefix, linesB.size - suffix)

    if (midA.isEmpty() && midB.isEmpty()) return emptyList()
    if (midA.isEmpty()) return listOf(DiffDelta(prefix, emptyList(), prefix, midB.toList()))
    if (midB.isEmpty()) return listOf(DiffDelta(prefix, midA.toList(), prefix, emptyList()))

    if (midA.size + midB.size > MAX_DIFF_MID_LINES) {
        return listOf(DiffDelta(prefix, midA.toList(), prefix, midB.toList()))
    }

    val map = HashMap<String, Int>(midA.size + midB.size)
    val intA = ArrayList<Int>(midA.size)
    for (line in midA) {
        intA.add(map.getOrPut(line) { map.size })
    }
    val intB = ArrayList<Int>(midB.size)
    for (line in midB) {
        intB.add(map.getOrPut(line) { map.size })
    }

    val patch = DiffUtils.diff(intA, intB)
    return patch.deltas.map { d ->
        val sp = d.source.position
        val tp = d.target.position
        DiffDelta(
            sourcePos = prefix + sp,
            sourceLines = midA.subList(sp, sp + d.source.lines.size).toList(),
            targetPos = prefix + tp,
            targetLines = midB.subList(tp, tp + d.target.lines.size).toList()
        )
    }
}
