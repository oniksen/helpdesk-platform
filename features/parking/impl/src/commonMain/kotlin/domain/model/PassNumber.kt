package domain.model

import kotlin.jvm.JvmInline

internal fun String.toPassNumber(): Result<PassNumber> {
    return try {
        Result.success(PassNumber(this))
    } catch (e: IllegalStateException) {
        Result.failure(e)
    }
}

internal fun PassNumber.toDecimalNumber(): String {
    return transformNumber(this.number)
}

@JvmInline
value class PassNumber(val number: String) {
    init {
        if (number.isEmpty()) error("Pass number must not be empty")

        val regex = "^\\d{1,3}/\\d{1,5}$".toRegex()
        if (!regex.matches(number)) error("Invalid Pass Number")

        val (facility, cardNumber) = number.split('/')

        if (facility.toInt() !in 0..MAX_FACILITY_VALUE) error("Invalid Facility Number")
        if (cardNumber.toInt() !in 0..MAX_CARD_NUMBER_VALUE) error("Invalid Card Number")
    }
}

private fun transformNumber(number: String): String {
    val (facility, cardNumber) = number.split('/')

    // Внимание! Использование toHexString() поломает функцию, т.к. возвращает все нули всегда.
    // Нам нужно обрезать HEX до определённого кол-ва цифр. Поэтому нужно возвращать HEX без
    // ведущих нулей. Для этого используем toString(16).
    val facilityHex = facility.toInt().toString(16).padStart(2, '0')
    val cardNumberHex = cardNumber.toInt().toString(16).padStart(4, '0')
    val fullNumber = "$facilityHex$cardNumberHex".toInt(radix = 16)
    return fullNumber.toString().padStart(8, '0')
}

private const val MAX_FACILITY_VALUE = 255
private const val MAX_CARD_NUMBER_VALUE = 65535