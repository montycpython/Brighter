package com.supernatural.bwriter

import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTimeApi
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

actual fun sha256Digest(input: ByteArray): ByteArray = Sha256Wasm.digest(input)

actual fun unsignedBytesToDecimalString(bytes: ByteArray): String {
    // Repeated division by ten over the big-endian byte array; yields the
    // same decimal string as the Android BigInteger implementation.
    val work = bytes.copyOf()
    var start = 0
    while (start < work.size && work[start] == 0.toByte()) start++
    if (start == work.size) return "0"
    val digits = StringBuilder()
    var lo = start
    while (lo < work.size) {
        var remainder = 0
        for (i in lo until work.size) {
            val current = (remainder shl 8) or (work[i].toInt() and 0xFF)
            work[i] = (current / 10).toByte()
            remainder = current % 10
        }
        digits.append('0' + remainder)
        while (lo < work.size && work[lo] == 0.toByte()) lo++
    }
    return digits.reverse().toString()
}

@OptIn(ExperimentalTimeApi::class)
actual fun currentTimeMillis(): Long = Clock.System.now().toEpochMilliseconds()

@OptIn(ExperimentalUuidApi::class)
actual fun randomUuidString(): String = Uuid.random().toString()

@OptIn(ExperimentalTimeApi::class)
actual fun platformSeasonYear(millis: Long): Int {
    val local = Instant.fromEpochMilliseconds(millis)
        .toLocalDateTime(TimeZone.currentSystemDefault())
    return when (local.month) {
        Month.NOVEMBER -> local.year
        Month.DECEMBER -> local.year + 1
        else -> local.year
    }
}

actual fun logWarn(tag: String, message: String) {
    // Diagnostics only on web; the browser console is the log sink.
    println("WARN $tag: $message")
}
