package domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PassNumberTest {

    @Test
    fun minValues() {
        val pass = "0/0".toPassNumber().getOrNull()
        val decimal = pass?.toDecimalNumber()
        assertEquals("00000000", decimal)
    }

    @Test
    fun simpleValid() {
        val pass = PassNumber("1/1").toDecimalNumber()
        assertEquals("00065537", pass)
    }

    @Test
    fun maxValues() {
        val pass = PassNumber("255/65535").toDecimalNumber()
        assertEquals("16777215", pass)
    }

    @Test
    fun maxFacilityZeroCard() {
        val pass = PassNumber("255/0").toDecimalNumber()
        assertEquals("16711680", pass)
    }

    @Test
    fun zeroFacilityMaxCard() {
        val pass = PassNumber("0/65535").toDecimalNumber()
        assertEquals("00065535", pass)
    }

    @Test
    fun emptyStringFails() {
        assertFailsWith<IllegalStateException>("Pass number must not be empty") {
            PassNumber("")
        }
    }

    @Test
    fun noDelimiterFails() {
        assertFailsWith<Exception> {
            PassNumber("12345")
        }
    }

    @Test
    fun facilityExceedsMaxFails() {
        assertFailsWith<IllegalStateException>("Invalid Facility Number") {
            PassNumber("256/0")
        }
    }

    @Test
    fun cardNumberExceedsMaxFails() {
        assertFailsWith<IllegalStateException>("Invalid Card Number") {
            PassNumber("0/65536")
        }
    }

    @Test
    fun negativeFacilityFails() {
        assertFailsWith<Exception> {
            PassNumber("-1/0")
        }
    }

    @Test
    fun negativeCardNumberFails() {
        assertFailsWith<Exception> {
            PassNumber("0/-1")
        }
    }

    @Test
    fun toPassNumberValid() {
        val result = "1/1".toPassNumber()
        assertEquals(true, result.isSuccess)
        assertEquals("00065537", result.getOrNull()?.toDecimalNumber())
    }

    @Test
    fun toPassNumberInvalid() {
        val result = "".toPassNumber()
        assertEquals(true, result.isFailure)
    }
}
