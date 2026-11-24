@file:OptIn(kotlin.ExperimentalUnsignedTypes::class)

package com.supernatural.bwriter.crypto

/**
 * 256-bit unsigned arithmetic on eight 32-bit limbs, little-endian — the
 * foundation of the secp256k1 field and group math. Only what the curve
 * code needs: comparison, modular addition and subtraction, modular
 * multiplication (full product, then long-division reduction), and
 * modular inverse by Fermat's little theorem (both moduli are prime).
 * Every operation is fixed-size and allocation-explicit; correctness is
 * by construction and by the known-answer vectors in Secp256k1SelfTest.
 */
internal class U256(val limbs: UIntArray) {

  init {
    require(limbs.size == 8)
  }

  fun isZero(): Boolean = limbs.all { it == 0u }

  fun bit(i: Int): Boolean = ((limbs[i / 32] shr (i % 32)) and 1u) == 1u

  operator fun compareTo(other: U256): Int {
    for (i in 7 downTo 0) {
      if (limbs[i] != other.limbs[i]) return if (limbs[i] < other.limbs[i]) -1 else 1
    }
    return 0
  }

  override fun equals(other: Any?): Boolean =
    other is U256 && limbs.contentEquals(other.limbs)

  override fun hashCode(): Int = limbs.contentHashCode()

  fun toBytes32(): ByteArray {
    val out = ByteArray(32)
    for (i in 0 until 8) {
      val limb = limbs[7 - i]
      out[4 * i] = (limb shr 24).toByte()
      out[4 * i + 1] = (limb shr 16).toByte()
      out[4 * i + 2] = (limb shr 8).toByte()
      out[4 * i + 3] = limb.toByte()
    }
    return out
  }

  companion object {
    val ZERO = U256(UIntArray(8))
    val ONE = U256(UIntArray(8).also { it[0] = 1u })

    fun fromBytes32(bytes: ByteArray): U256 {
      require(bytes.size == 32)
      val out = UIntArray(8)
      for (i in 0 until 8) {
        val p = 4 * (7 - i)
        out[i] = ((bytes[p].toUInt() and 0xFFu) shl 24) or
          ((bytes[p + 1].toUInt() and 0xFFu) shl 16) or
          ((bytes[p + 2].toUInt() and 0xFFu) shl 8) or
          (bytes[p + 3].toUInt() and 0xFFu)
      }
      return U256(out)
    }

    fun fromHex(hex: String): U256 {
      require(hex.length == 64)
      val bytes = ByteArray(32)
      for (i in 0 until 32) {
        bytes[i] = hex.substring(2 * i, 2 * i + 2).toInt(16).toByte()
      }
      return fromBytes32(bytes)
    }
  }
}

/** (a + b) mod m, for a, b < m: the sum is below 2m, one subtraction. */
internal fun addMod(a: U256, b: U256, m: U256): U256 {
  val sum = UIntArray(9)
  var carry = 0uL
  for (i in 0 until 8) {
    val t = a.limbs[i].toULong() + b.limbs[i].toULong() + carry
    sum[i] = t.toUInt()
    carry = t shr 32
  }
  sum[8] = carry.toUInt()
  if (compare9(sum, m) >= 0) subtract9(sum, m)
  return U256(sum.copyOfRange(0, 8))
}

/** (a - b) mod m, for a, b < m. */
internal fun subMod(a: U256, b: U256, m: U256): U256 {
  val diff = UIntArray(8)
  var borrow = 0uL
  for (i in 0 until 8) {
    val t = a.limbs[i].toULong() - b.limbs[i].toULong() - borrow
    diff[i] = t.toUInt()
    borrow = if (t > 0xFFFFFFFFuL) 1uL else 0uL
  }
  if (borrow == 1uL) {
    var carry = 0uL
    for (i in 0 until 8) {
      val t = diff[i].toULong() + m.limbs[i].toULong() + carry
      diff[i] = t.toUInt()
      carry = t shr 32
    }
  }
  return U256(diff)
}

private fun compare9(a: UIntArray, m: U256): Int {
  if (a[8] != 0u) return 1
  for (i in 7 downTo 0) {
    if (a[i] != m.limbs[i]) return if (a[i] < m.limbs[i]) -1 else 1
  }
  return 0
}

private fun subtract9(a: UIntArray, m: U256) {
  var borrow = 0uL
  for (i in 0 until 8) {
    val t = a[i].toULong() - m.limbs[i].toULong() - borrow
    a[i] = t.toUInt()
    borrow = if (t > 0xFFFFFFFFuL) 1uL else 0uL
  }
  a[8] = (a[8].toULong() - borrow).toUInt()
}

/** The full 512-bit product of two 256-bit values, 16 limbs. */
private fun mulFull(a: U256, b: U256): UIntArray {
  val out = UIntArray(16)
  for (i in 0 until 8) {
    var carry = 0uL
    for (j in 0 until 8) {
      val t = out[i + j].toULong() + a.limbs[i].toULong() * b.limbs[j].toULong() + carry
      out[i + j] = t.toUInt()
      carry = t shr 32
    }
    out[i + 8] = (out[i + 8].toULong() + carry).toUInt()
  }
  return out
}

/** x mod m for a 512-bit x, by shift-and-subtract long division. */
private fun modReduce(x: UIntArray, m: U256): U256 {
  val rem = UIntArray(9)
  for (i in 511 downTo 0) {
    var carry = 0u
    for (j in 0 until 9) {
      val next = rem[j] shr 31
      rem[j] = (rem[j] shl 1) or carry
      carry = next
    }
    if (((x[i / 32] shr (i % 32)) and 1u) == 1u) rem[0] = rem[0] or 1u
    if (compare9(rem, m) >= 0) subtract9(rem, m)
  }
  return U256(rem.copyOfRange(0, 8))
}

/** (a * b) mod m. */
internal fun mulMod(a: U256, b: U256, m: U256): U256 = modReduce(mulFull(a, b), m)

/** base^exp mod m, square-and-multiply over the exponent's 256 bits. */
internal fun powMod(base: U256, exp: U256, m: U256): U256 {
  var result = U256.ONE
  for (i in 255 downTo 0) {
    result = mulMod(result, result, m)
    if (exp.bit(i)) result = mulMod(result, base, m)
  }
  return result
}

/** a^-1 mod m, by Fermat: a^(m-2) mod m for prime m. */
internal fun invMod(a: U256, m: U256, mMinus2: U256): U256 = powMod(a, mMinus2, m)
