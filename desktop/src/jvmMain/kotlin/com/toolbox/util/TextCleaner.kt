package com.toolbox.util

data class CleanOptions(
    val trimLines: Boolean = false,
    val removeEmptyLines: Boolean = false,
    val mergeSpaces: Boolean = false,
    val removeSpaces: Boolean = false,
    val dedupeLines: Boolean = false,
    val sortLines: Boolean = false,
    val removeInvisible: Boolean = false
)

object TextCleaner {
    fun clean(input: String, options: CleanOptions): String {
        var text = input
        if (options.removeInvisible) {
            text = text.replace(Regex("[\\p{Cntrl}&&[^\r\n\t]]"), "")
                .replace("\uFEFF", "")
        }
        if (options.trimLines) {
            text = text.lines().joinToString("\n") { it.trim() }
        }
        if (options.removeSpaces) {
            text = text.replace(" ", "").replace("\u3000", "")
        } else if (options.mergeSpaces) {
            text = text.replace(Regex("[ \\t\\u3000]+"), " ")
        }
        if (options.removeEmptyLines) {
            text = text.lines().filter { it.isNotBlank() }.joinToString("\n")
        }
        if (options.dedupeLines) {
            text = text.lines().distinct().joinToString("\n")
        }
        if (options.sortLines) {
            text = text.lines().sorted().joinToString("\n")
        }
        return text
    }
}