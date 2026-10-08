package com.toolbox.util

import java.net.URLDecoder
import java.net.URLEncoder
import java.security.MessageDigest
import java.util.Base64

object CodecUtils {
    fun base64Encode(text: String): String {
        return Base64.getEncoder().encodeToString(text.toByteArray(Charsets.UTF_8))
    }
    
    fun base64Decode(text: String): String {
        return String(Base64.getDecoder().decode(text), Charsets.UTF_8)
    }
    
    fun urlEncode(text: String, spaceAsPercent20: Boolean): String {
        val encoded = URLEncoder.encode(text, "UTF-8")
        return if (spaceAsPercent20) encoded.replace("+", "%20") else encoded
    }
    
    fun urlDecode(text: String): String {
        return URLDecoder.decode(text, "UTF-8")
    }
    
    fun hashBytes(bytes: ByteArray, algorithm: String): String {
        val digest = MessageDigest.getInstance(algorithm)
        val hashBytes = digest.digest(bytes)
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
    
    fun hashText(text: String, algorithm: String): String {
        return hashBytes(text.toByteArray(Charsets.UTF_8), algorithm)
    }
}