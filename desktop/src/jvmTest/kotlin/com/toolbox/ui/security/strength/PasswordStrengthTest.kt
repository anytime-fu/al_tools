package com.toolbox.ui.security.strength

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PasswordStrengthTest {
    
    @Test
    fun emptyPassword() {
        val result = PasswordStrength.evaluate("")
        assertEquals(0, result.score)
        assertEquals("无", result.level)
    }
    
    @Test
    fun strongPasswordScoresHigh() {
        val result = PasswordStrength.evaluate("Aa1!Bb2@Cc3#Dd4\$Ee5%")
        assertTrue(result.score >= 80, "score was ${result.score}")
        assertEquals("极强", result.level)
    }
    
    @Test
    fun commonPasswordIsPenalized() {
        val weak = PasswordStrength.evaluate("password123")
        val strong = PasswordStrength.evaluate("Xk9#mQ2\$pL7!")
        assertTrue(weak.score < strong.score)
    }
    
    @Test
    fun shortPasswordIsWeak() {
        val result = PasswordStrength.evaluate("ab")
        assertTrue(result.score < 40, "score was ${result.score}")
    }
    
    @Test
    fun generateRespectsLengthAndClasses() {
        val password = PasswordStrength.generate(
            length = 16,
            useUpper = true,
            useLower = true,
            useDigit = true,
            useSymbol = true,
            excludeConfusing = false
        )
        assertEquals(16, password.length)
        assertTrue(password.any { it in 'A'..'Z' })
        assertTrue(password.any { it in 'a'..'z' })
        assertTrue(password.any { it in '0'..'9' })
        assertTrue(password.any { !it.isLetterOrDigit() })
    }
    
    @Test
    fun generateExcludeConfusing() {
        val password = PasswordStrength.generate(
            length = 32,
            useUpper = true,
            useLower = true,
            useDigit = true,
            useSymbol = true,
            excludeConfusing = true
        )
        assertTrue(password.none { it in "0OoIl1" })
    }
}