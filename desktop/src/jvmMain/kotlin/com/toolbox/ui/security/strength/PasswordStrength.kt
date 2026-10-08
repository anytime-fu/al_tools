package com.toolbox.ui.security.strength

import java.security.SecureRandom
import java.util.Collections

object PasswordStrength {

    data class Check(val label: String, val passed: Boolean)

    data class Result(
        val score: Int,
        val level: String,
        val entropyBits: Double,
        val checks: List<Check>,
        val suggestions: List<String>
    )

    private val common = listOf(
        "123456", "password", "12345678", "qwerty", "abc123", "111111", "123123",
        "admin", "letmein", "000000", "666666", "888888", "password1", "iloveyou", "dragon", "a123456"
    )

    fun evaluate(pw: String): Result {
        if (pw.isEmpty()) {
            return Result(0, "无", 0.0, emptyList(), listOf("请输入或生成密码"))
        }
        val hasLower = pw.any { it in 'a'..'z' }
        val hasUpper = pw.any { it in 'A'..'Z' }
        val hasDigit = pw.any { it in '0'..'9' }
        val hasSymbol = pw.any { !it.isLetterOrDigit() }
        val variety = listOf(hasLower, hasUpper, hasDigit, hasSymbol).count { it }
        val charset = (if (hasLower) 26 else 0) + (if (hasUpper) 26 else 0) +
            (if (hasDigit) 10 else 0) + (if (hasSymbol) 33 else 0)
        val entropy = if (charset > 0) {
            pw.length * kotlin.math.ln(charset.toDouble()) / kotlin.math.ln(2.0)
        } else {
            0.0
        }

        var score = 0
        score += when {
            pw.length >= 16 -> 40
            pw.length >= 12 -> 32
            pw.length >= 8 -> 22
            pw.length >= 6 -> 12
            else -> 4
        }
        score += variety * 12
        if (pw.length >= 10 && variety >= 3) score += 8
        val lower = pw.lowercase()
        if (common.any { lower.contains(it) }) score -= 25
        if (hasSequential(pw)) score -= 10
        if (hasRepeat(pw)) score -= 10
        if (pw.toSet().size <= 2 && pw.length > 3) score -= 15
        score = score.coerceIn(0, 100)

        val checks = listOf(
            Check("长度 ≥ 8 位", pw.length >= 8),
            Check("长度 ≥ 12 位", pw.length >= 12),
            Check("包含小写字母", hasLower),
            Check("包含大写字母", hasUpper),
            Check("包含数字", hasDigit),
            Check("包含符号", hasSymbol),
            Check("无常见弱密码片段", common.none { lower.contains(it) }),
            Check("无连续/重复字符", !hasSequential(pw) && !hasRepeat(pw))
        )

        val suggestions = mutableListOf<String>()
        if (pw.length < 12) suggestions.add("加长到 12 位以上（当前 ${pw.length} 位）")
        if (!hasUpper) suggestions.add("加入大写字母 A-Z")
        if (!hasLower) suggestions.add("加入小写字母 a-z")
        if (!hasDigit) suggestions.add("加入数字 0-9")
        if (!hasSymbol) suggestions.add("加入符号，如 !@#\$%^&*")
        if (common.any { lower.contains(it) }) suggestions.add("移除常见密码片段（如 123456、password）")
        if (hasSequential(pw)) suggestions.add("避免 abc/123 等连续字符")
        if (hasRepeat(pw)) suggestions.add("避免 aaa/111 等重复字符")

        val level = when {
            score >= 80 -> "极强"
            score >= 60 -> "强"
            score >= 40 -> "中等"
            score >= 20 -> "弱"
            else -> "很弱"
        }
        return Result(score, level, entropy, checks, suggestions)
    }

    fun generate(
        length: Int,
        useUpper: Boolean,
        useLower: Boolean,
        useDigit: Boolean,
        useSymbol: Boolean,
        excludeConfusing: Boolean,
        random: SecureRandom = SecureRandom()
    ): String {
        val upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        val lower = "abcdefghijklmnopqrstuvwxyz"
        val digit = "0123456789"
        val symbol = "!@#\$%^&*()-_=+[]{};:,.?"
        val confusing = "0OoIl1"
        var pools = listOfNotNull(
            if (useUpper) upper else null,
            if (useLower) lower else null,
            if (useDigit) digit else null,
            if (useSymbol) symbol else null
        )
        if (pools.isEmpty()) pools = listOf(lower)

        fun clean(s: String): String = if (excludeConfusing) s.filter { it !in confusing } else s

        val all = clean(pools.joinToString("")).ifEmpty { pools.joinToString("") }
        val safeLen = length.coerceIn(4, 128)
        val required = pools.map { pool ->
            val chars = clean(pool).ifEmpty { pool }
            chars[random.nextInt(chars.length)]
        }
        val rest = List((safeLen - required.size).coerceAtLeast(0)) {
            all[random.nextInt(all.length)]
        }
        val chars = ArrayList(required + rest)
        Collections.shuffle(chars, random)
        return chars.joinToString("")
    }

    private fun hasSequential(s: String): Boolean {
        for (i in 0 until s.length - 2) {
            val a = s[i]
            val b = s[i + 1]
            val c = s[i + 2]
            if ((b - a == 1 && c - b == 1) || (a - b == 1 && b - c == 1)) return true
        }
        return false
    }

    private fun hasRepeat(s: String): Boolean {
        for (i in 0 until s.length - 2) {
            if (s[i] == s[i + 1] && s[i] == s[i + 2]) return true
        }
        return false
    }
}
