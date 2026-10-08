package com.toolbox.util

import kotlin.test.Test
import kotlin.test.assertEquals

class CodecUtilsTest {
    
    @Test
    fun base64RoundTrip() {
        val original = "Hello, 世界! @#\$"
        val encoded = CodecUtils.base64Encode(original)
        assertEquals(original, CodecUtils.base64Decode(encoded))
    }
    
    @Test
    fun base64KnownVector() {
        assertEquals("aGVsbG8=", CodecUtils.base64Encode("hello"))
    }
    
    @Test
    fun urlEncodeSpaceAsPercent20() {
        assertEquals("a%20b", CodecUtils.urlEncode("a b", spaceAsPercent20 = true))
    }
    
    @Test
    fun urlEncodeSpaceAsPlus() {
        assertEquals("a+b", CodecUtils.urlEncode("a b", spaceAsPercent20 = false))
    }
    
    @Test
    fun urlRoundTrip() {
        val original = "键值对 a=b&c=d 中文"
        assertEquals(original, CodecUtils.urlDecode(CodecUtils.urlEncode(original, true)))
    }
    
    @Test
    fun md5KnownVector() {
        assertEquals("900150983cd24fb0d6963f7d28e17f72", CodecUtils.hashText("abc", "MD5"))
    }
    
    @Test
    fun sha1KnownVector() {
        assertEquals("a9993e364706816aba3e25717850c26c9cd0d89d", CodecUtils.hashText("abc", "SHA-1"))
    }
    
    @Test
    fun sha256KnownVector() {
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            CodecUtils.hashText("abc", "SHA-256")
        )
    }
}