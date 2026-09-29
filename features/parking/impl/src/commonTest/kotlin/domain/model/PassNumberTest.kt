package domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PassNumberTest {

    @Test
    fun `converts valid pass numbers to decimal numbers`() {
        CONVERSION_CASES.forEach { (pass, expected) ->
            assertEquals(expected, PassNumber(pass).toDecimalNumber(), "Неверная конвертация для '$pass'")
        }
    }

    @Test
    fun `rejects empty pass number`() {
        assertFailsWith<IllegalStateException>("Pass number must not be empty") {
            PassNumber("")
        }
    }

    @Test
    fun `rejects pass numbers without a facility delimiter`() {
        assertFailsWith<Exception> {
            PassNumber("12345")
        }
    }

    @Test
    fun `rejects negative facility and card numbers`() {
        NEGATIVE_CASES.forEach { pass ->
            assertFailsWith<Exception>("Неожиданно принят номер с отрицательной частью: '$pass'") {
                PassNumber(pass)
            }
        }
    }

    @Test
    fun `rejects facility and card numbers out of range`() {
        OUT_OF_RANGE_CASES.forEach { (pass, message) ->
            assertFailsWith<IllegalStateException>(message) {
                PassNumber(pass)
            }
        }
    }

    @Test
    fun `reports parsing outcome through Result`() {
        val valid = "1/1".toPassNumber()
        assertEquals(true, valid.isSuccess)
        assertEquals("00065537", valid.getOrNull()?.toDecimalNumber())

        assertEquals(true, "".toPassNumber().isFailure)
    }

    private companion object {
        val CONVERSION_CASES = listOf(
            "0/0" to "00000000",
            "1/1" to "00065537",
            "255/65535" to "16777215",
            "255/0" to "16711680",
            "0/65535" to "00065535",
        )

        val NEGATIVE_CASES = listOf(
            "-1/0",
            "0/-1",
        )

        val OUT_OF_RANGE_CASES = listOf(
            "256/0" to "Invalid Facility Number",
            "0/65536" to "Invalid Card Number",
        )
    }
}
