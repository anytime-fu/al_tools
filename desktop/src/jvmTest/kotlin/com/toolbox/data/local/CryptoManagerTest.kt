package com.toolbox.data.local

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CryptoManagerTest {
    
    @Test
    fun encryptThenDecryptRoundTrip() {
        val secret = "sk-test-1234567890"
        val encrypted = CryptoManager.encrypt(secret)
        assertTrue(CryptoManager.isEncrypted(encrypted))
        assertEquals(secret, CryptoManager.decrypt(encrypted))
    }
    
    @Test
    fun encryptProducesDifferentCiphertextEachTime() {
        val a = CryptoManager.encrypt("same")
        val b = CryptoManager.encrypt("same")
        assertTrue(a != b)
    }
    
    @Test
    fun legacyPlaintextPassesThrough() {
        val plaintext = "legacy-api-key"
        assertFalse(CryptoManager.isEncrypted(plaintext))
        assertEquals(plaintext, CryptoManager.decrypt(plaintext))
    }
    
    @Test
    fun emptyStringStaysEmpty() {
        assertEquals("", CryptoManager.encrypt(""))
        assertEquals("", CryptoManager.decrypt(""))
    }
    
    @Test
    fun tamperedCiphertextReturnsEmpty() {
        val encrypted = CryptoManager.encrypt("secret")
        val tampered = encrypted.dropLast(4) + "AAAA"
        assertEquals("", CryptoManager.decrypt(tampered))
    }
}