package com.toolbox.data.local

import java.io.File
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object CryptoManager {
    private const val ENCRYPTED_PREFIX = "enc:"
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_BITS = 128
    private const val KEY_BYTES = 32
    
    private val keyFile = File(System.getProperty("user.home"), ".ai-toolbox/secret.key")
    
    private val secretKey: SecretKey by lazy { loadOrCreateKey() }
    
    fun encrypt(plainText: String): String {
        if (plainText.isEmpty()) return plainText
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val iv = ByteArray(GCM_IV_LENGTH).also { SecureRandom().nextBytes(it) }
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_BITS, iv))
        val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        val payload = iv + cipherText
        return ENCRYPTED_PREFIX + Base64.getEncoder().encodeToString(payload)
    }
    
    fun decrypt(storedValue: String): String {
        if (!isEncrypted(storedValue)) return storedValue
        return try {
            val payload = Base64.getDecoder().decode(storedValue.removePrefix(ENCRYPTED_PREFIX))
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(
                Cipher.DECRYPT_MODE,
                secretKey,
                GCMParameterSpec(GCM_TAG_BITS, payload, 0, GCM_IV_LENGTH)
            )
            val plainBytes = cipher.doFinal(payload, GCM_IV_LENGTH, payload.size - GCM_IV_LENGTH)
            String(plainBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            ""
        }
    }
    
    fun isEncrypted(value: String): Boolean = value.startsWith(ENCRYPTED_PREFIX)
    
    private fun loadOrCreateKey(): SecretKey {
        keyFile.parentFile?.mkdirs()
        if (keyFile.exists()) {
            val bytes = keyFile.readBytes()
            if (bytes.size == KEY_BYTES) {
                return SecretKeySpec(bytes, "AES")
            }
            val corrupt = File(keyFile.parentFile, "secret.key.corrupt-${System.currentTimeMillis()}")
            keyFile.copyTo(corrupt, overwrite = true)
            keyFile.delete()
        }
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(KEY_BYTES * 8)
        val key = keyGen.generateKey()
        keyFile.writeBytes(key.encoded)
        runCatching {
            keyFile.setReadable(false, false)
            keyFile.setReadable(true, true)
            keyFile.setWritable(false, false)
            keyFile.setWritable(true, true)
        }
        return key
    }
}