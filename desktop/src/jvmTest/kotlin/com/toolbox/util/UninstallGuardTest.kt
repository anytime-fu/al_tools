package com.toolbox.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UninstallGuardTest {

    @Test
    fun parseProductCodeFromMsiExecString() {
        val code = UninstallGuard.parseProductCode("MsiExec.exe /X{1A2B3C4D-5E6F-7890-ABCD-EF1234567890}")
        assertEquals("{1A2B3C4D-5E6F-7890-ABCD-EF1234567890}", code)
    }

    @Test
    fun parseProductCodeFromISetupString() {
        val code = UninstallGuard.parseProductCode("\"C:\\\\Program Files\\\\AI-Toolbox\\\\unins000.exe\"")
        assertNull(code)
    }

    @Test
    fun parseProductCodeRejectsGarbage() {
        assertNull(UninstallGuard.parseProductCode("not a code"))
    }

    @Test
    fun guardRequestDetection() {
        assertTrue(UninstallGuard.isGuardRequest(arrayOf("--uninstall-guard")))
        assertTrue(UninstallGuard.isGuardRequest(arrayOf("--other", UninstallGuard.GUARD_ARG)))
        assertFalse(UninstallGuard.isGuardRequest(arrayOf()))
        assertFalse(UninstallGuard.isGuardRequest(arrayOf("--help")))
    }
}