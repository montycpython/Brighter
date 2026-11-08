package com.supernatural.bwriter.data

import java.math.BigInteger
import java.security.MessageDigest

fun bisnFor(birthId: String): String {
    if (birthId.isBlank()) return ""
    val digest = MessageDigest.getInstance("SHA-256").digest(birthId.toByteArray(Charsets.UTF_8))
    val decimal = BigInteger(1, digest).toString()
    val first12 = decimal.padStart(12, '0').substring(0, 12)
    var sum = 0
    for (i in first12.indices) {
        val digit = first12[i] - '0'
        sum += if (i % 2 == 0) digit else digit * 3
    }
    val digits = first12 + ((10 - (sum % 10)) % 10).toString()
    return digits.substring(0, 3) + "-" + digits[3] + "-" +
        digits.substring(4, 8) + "-" + digits.substring(8, 12) + "-" + digits[12]
}
