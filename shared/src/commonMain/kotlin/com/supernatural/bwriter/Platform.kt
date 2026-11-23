package com.supernatural.bwriter

/**
 * Platform seams for the shared core. Common code calls only these; each
 * target supplies the actual implementation. The Android actuals are the
 * exact platform calls this code used before the extraction, so Android
 * behavior is unchanged.
 */

/** SHA-256 digest of [input]. */
expect fun sha256Digest(input: ByteArray): ByteArray

/** Decimal representation of the unsigned big-endian integer stored in [bytes]. */
expect fun unsignedBytesToDecimalString(bytes: ByteArray): String

/** Current time in milliseconds since the Unix epoch. */
expect fun currentTimeMillis(): Long

/** A random UUID in hyphenated string form. */
expect fun randomUuidString(): String

/**
 * Season year for [millis], evaluated in the local timezone: a November
 * date returns its calendar year, a December date returns the next year,
 * and every other month returns its calendar year.
 */
expect fun platformSeasonYear(millis: Long): Int

/** Writes a warning to the platform log. */
expect fun logWarn(tag: String, message: String)
