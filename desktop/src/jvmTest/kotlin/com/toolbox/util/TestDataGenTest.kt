package com.toolbox.util

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestDataGenTest {

    @Test
    fun idCardHasValidChecksumAndLength() {
        repeat(50) {
            val id = TestDataGen.idCardNumber(TestDataGen.randomBirthDate(), TestDataGen.Gender.RANDOM)
            assertEquals(18, id.length)
            val body = id.substring(0, 17)
            assertTrue(body.all { it.isDigit() }, "body has non-digit: $id")
            assertEquals(TestDataGen.idCheckDigit(body), id[17])
        }
    }

    @Test
    fun idCardGenderParityMatches() {
        repeat(20) {
            val male = TestDataGen.idCardNumber(LocalDate.of(2000, 1, 1), TestDataGen.Gender.MALE)
            assertEquals(1, male.substring(14, 17).toInt() % 2)
            val female = TestDataGen.idCardNumber(LocalDate.of(2000, 1, 1), TestDataGen.Gender.FEMALE)
            assertEquals(0, female.substring(14, 17).toInt() % 2)
        }
    }

    @Test
    fun idCardEncodesBirthDate() {
        val id = TestDataGen.idCardNumber(LocalDate.of(1995, 7, 23), TestDataGen.Gender.RANDOM)
        assertEquals("19950723", id.substring(6, 14))
    }

    @Test
    fun phoneMatchesCnMobileFormat() {
        repeat(20) {
            val phone = TestDataGen.phoneNumber()
            assertTrue(phone.matches(Regex("^1[3-9]\\d{9}$")), "bad phone: $phone")
        }
    }

    @Test
    fun emailFormat() {
        repeat(20) {
            val email = TestDataGen.email()
            assertTrue(email.matches(Regex("^[a-z]+\\d{3}@[a-z0-9.-]+$")), "bad email: $email")
        }
    }

    @Test
    fun chineseNameLength() {
        repeat(20) {
            val name = TestDataGen.chineseName(TestDataGen.Gender.RANDOM)
            assertTrue(name.length in 2..3, "bad name: $name")
        }
    }

    @Test
    fun bankCardPassesLuhn() {
        repeat(30) {
            val card = TestDataGen.bankCard()
            assertEquals(16, card.length)
            assertEquals(TestDataGen.luhnCheckDigit(card.substring(0, 15)), card[15])
        }
    }

    @Test
    fun placeholderParagraphs() {
        val en = TestDataGen.placeholderText(3, chinese = false)
        assertEquals(3, en.split("\n\n").size)
        val zh = TestDataGen.placeholderText(2, chinese = true)
        assertEquals(2, zh.split("\n\n").size)
    }

    @Test
    fun randomDigitsLength() {
        val digits = TestDataGen.randomDigits(8)
        assertEquals(8, digits.length)
        assertTrue(digits.all { it.isDigit() })
    }
}