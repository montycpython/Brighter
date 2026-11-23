package com.supernatural.bwriter

import java.math.BigInteger
import java.security.MessageDigest
import java.util.Calendar
import java.util.UUID

actual fun sha256Digest(input: ByteArray): ByteArray =
    MessageDigest.getInstance("SHA-256").digest(input)

actual fun unsignedBytesToDecimalString(bytes: ByteArray): String =
    BigInteger(1, bytes).toString()

actual fun currentTimeMillis(): Long = System.currentTimeMillis()

actual fun randomUuidString(): String = UUID.randomUUID().toString()

actual fun platformSeasonYear(millis: Long): Int {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = millis
    val year = calendar.get(Calendar.YEAR)
    return when (calendar.get(Calendar.MONTH)) {
        Calendar.NOVEMBER -> year
        Calendar.DECEMBER -> year + 1
        else -> year
    }
}

actual fun logWarn(tag: String, message: String) {
    android.util.Log.w(tag, message)
}
