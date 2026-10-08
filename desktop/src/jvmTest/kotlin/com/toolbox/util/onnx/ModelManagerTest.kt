package com.toolbox.util.onnx

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ModelManagerTest {

    @Test
    fun sha256MatchesKnownVector() {
        val f = File.createTempFile("sha", ".txt")
        try {
            f.writeText("abc")
            assertEquals(
                "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
                ModelManager.sha256(f)
            )
        } finally {
            f.delete()
        }
    }

    @Test
    fun installFromFileRejectsWrongSha() {
        val f = File.createTempFile("model", ".onnx")
        try {
            f.writeBytes(ByteArray(2_000_000) { 1 })
            val thrown = runCatching {
                ModelManager.installFromFile(f, verifySha256 = "deadbeef")
            }.exceptionOrNull()
            assertTrue(thrown is IllegalStateException, "expected sha rejection, got $thrown")
        } finally {
            f.delete()
        }
    }

    @Test
    fun defaultModelUrlAndChecksumPresent() {
        assertTrue(ModelManager.DEFAULT_URL.startsWith("https://"))
        assertEquals(64, ModelManager.DEFAULT_SHA256.length)
    }
}