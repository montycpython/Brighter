package com.supernatural.bwriter.crypto

import com.supernatural.bwriter.sha256Digest

/**
 * secp256k1 author identity, pure Kotlin, matching the Android build's
 * AuthorKeys semantics exactly:
 *
 *   public key  = the compressed point, hex
 *   address     = hex(RIPEMD160(SHA256(public key bytes)))
 *   sign/verify = SHA256withECDSA over the message's UTF-8 bytes,
 *                 DER-encoded signature, hex
 *
 * The arithmetic is proven against OpenSSL and published known answers
 * (see Secp256k1SelfTest); the curve formulas are the Explicit-Formulas
 * Database Jacobian forms for a = 0 (add-2007-bl, dbl-2001-b).
 */
object Secp256k1 {

  private val P: U256 = U256.fromHex("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFC2F")
  private val N: U256 = U256.fromHex("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEBAAEDCE6AF48A03BBFD25E8CD0364141")
  private val P_MINUS_2: U256 = U256.fromHex("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFC2D")
  private val N_MINUS_2: U256 = U256.fromHex("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEBAAEDCE6AF48A03BBFD25E8CD036413F")
  private val P_QUARTER: U256 = U256.fromHex("3FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFBFFFFF0C")
  private val GX: U256 = U256.fromHex("79BE667EF9DCBBAC55A06295CE870B07029BFCDB2DCE28D959F2815B16F81798")
  private val GY: U256 = U256.fromHex("483ADA7726A3C4655DA4FBFC0E1108A8FD17B448A68554199C47D08FFB10D4B8")
  private val THREE: U256 = U256.fromHex("0000000000000000000000000000000000000000000000000000000000000003")
  private val SEVEN: U256 = U256.fromHex("0000000000000000000000000000000000000000000000000000000000000007")
  private val EIGHT: U256 = U256.fromHex("0000000000000000000000000000000000000000000000000000000000000008")

  /** A curve point in Jacobian coordinates; infinity has Z = 0. */
  private class Jac(val x: U256, val y: U256, val z: U256)

  private fun generator() = Jac(GX, GY, U256.ONE)
  private fun infinity() = Jac(U256.ZERO, U256.ONE, U256.ZERO)

  private fun double(p: Jac): Jac {
    if (p.z.isZero() || p.y.isZero()) return infinity()
    val xx = mulMod(p.x, p.x, P)
    val yy = mulMod(p.y, p.y, P)
    val yyyy = mulMod(yy, yy, P)
    val zz = mulMod(p.z, p.z, P)
    // S = 2 * ((X + YY)^2 - XX - YYYY) = 4 * X * YY
    val sInner = subMod(subMod(mulMod(addMod(p.x, yy, P), addMod(p.x, yy, P), P), xx, P), yyyy, P)
    val s = addMod(sInner, sInner, P)
    val m = mulMod(THREE, xx, P)
    val t = subMod(mulMod(m, m, P), addMod(s, s, P), P)
    val y3 = subMod(mulMod(m, subMod(s, t, P), P), mulMod(EIGHT, yyyy, P), P)
    // Z = (Y + Z)^2 - YY - ZZ = 2 * Y * Z
    val zInner = subMod(subMod(mulMod(addMod(p.y, p.z, P), addMod(p.y, p.z, P), P), yy, P), zz, P)
    return Jac(t, y3, zInner)
  }

  private fun add(p: Jac, q: Jac): Jac {
    if (p.z.isZero()) return q
    if (q.z.isZero()) return p
    val z1z1 = mulMod(p.z, p.z, P)
    val z2z2 = mulMod(q.z, q.z, P)
    val u1 = mulMod(p.x, z2z2, P)
    val u2 = mulMod(q.x, z1z1, P)
    val s1 = mulMod(mulMod(p.y, q.z, P), z2z2, P)
    val s2 = mulMod(mulMod(q.y, p.z, P), z1z1, P)
    if (u1 == u2) {
      return if (s1 != s2) infinity() else double(p)
    }
    val h = subMod(u2, u1, P)
    val twoH = addMod(h, h, P)
    val i = mulMod(twoH, twoH, P)
    val j = mulMod(h, i, P)
    val r = addMod(subMod(s2, s1, P), subMod(s2, s1, P), P)
    val v = mulMod(u1, i, P)
    val x3 = subMod(subMod(mulMod(r, r, P), j, P), addMod(v, v, P), P)
    val y3 = subMod(mulMod(r, subMod(v, x3, P), P), addMod(mulMod(s1, j, P), mulMod(s1, j, P), P), P)
    val zSum = addMod(p.z, q.z, P)
    val z3 = mulMod(subMod(subMod(mulMod(zSum, zSum, P), z1z1, P), z2z2, P), h, P)
    return Jac(x3, y3, z3)
  }

  private fun scalarMul(k: U256, point: Jac): Jac {
    var result = infinity()
    var addend = point
    for (i in 0 until 256) {
      if (k.bit(i)) result = add(result, addend)
      addend = double(addend)
    }
    return result
  }

  private fun toAffine(p: Jac): Pair<U256, U256>? {
    if (p.z.isZero()) return null
    val zInv = invMod(p.z, P, P_MINUS_2)
    val zInv2 = mulMod(zInv, zInv, P)
    val zInv3 = mulMod(zInv2, zInv, P)
    return Pair(mulMod(p.x, zInv2, P), mulMod(p.y, zInv3, P))
  }

  /** The compressed public key bytes for a 32-byte private key. */
  fun publicKeyBytes(privateKey: ByteArray): ByteArray {
    val d = U256.fromBytes32(privateKey)
    val affine = toAffine(scalarMul(d, generator())) ?: return ByteArray(0)
    val out = ByteArray(33)
    out[0] = if (affine.second.bit(0)) 0x03 else 0x02
    affine.first.toBytes32().copyInto(out, 1)
    return out
  }

  fun publicKeyHex(privateKey: ByteArray): String = publicKeyBytes(privateKey).toHex()

  /** The point for compressed public key bytes, or null when invalid. */
  private fun decompress(pub: ByteArray): Jac? {
    if (pub.size != 33 || (pub[0] != 0x02.toByte() && pub[0] != 0x03.toByte())) return null
    val x = U256.fromBytes32(pub.copyOfRange(1, 33))
    if (x >= P) return null
    val ySquared = addMod(mulMod(mulMod(x, x, P), x, P), SEVEN, P)
    var y = powMod(ySquared, P_QUARTER, P)
    if (mulMod(y, y, P) != ySquared) return null
    val wantOdd = pub[0] == 0x03.toByte()
    if (y.bit(0) != wantOdd) y = subMod(P, y, P)
    return Jac(x, y, U256.ONE)
  }

  /** hex(RIPEMD160(SHA256(public key bytes))) — the author address. */
  fun addressFromPubKeyHex(publicKeyHex: String): String {
    if (publicKeyHex.isBlank()) return ""
    val pub = publicKeyHex.hexToBytes() ?: return ""
    val sha = sha256Digest(pub)
    return Ripemd160.digest(sha).toHex()
  }

  fun addressFromPrivateKey(privateKey: ByteArray): String =
    addressFromPubKeyHex(publicKeyHex(privateKey))

  /** A fresh private key drawn from the caller's randomness until valid. */
  fun generatePrivateKey(nextRandom: () -> ByteArray): ByteArray {
    while (true) {
      val candidate = nextRandom()
      if (candidate.size != 32) continue
      val d = U256.fromBytes32(candidate)
      if (!d.isZero() && d < N) return candidate
    }
  }

  /**
   * SHA256withECDSA over the message's UTF-8 bytes; DER signature, hex.
   * Randomness for the ephemeral key comes from the caller (the platform's
   * secure source); candidates are drawn until one yields a signature.
   */
  fun sign(privateKey: ByteArray, message: String, nextRandom: () -> ByteArray): String {
    val d = U256.fromBytes32(privateKey)
    if (d.isZero() || d >= N) return ""
    val z = reducedHash(message)
    while (true) {
      val kBytes = nextRandom()
      if (kBytes.size != 32) continue
      val k = U256.fromBytes32(kBytes)
      if (k.isZero() || k >= N) continue
      val affine = toAffine(scalarMul(k, generator())) ?: continue
      val r = modN(affine.first)
      if (r.isZero()) continue
      val s = mulMod(
        invMod(k, N, N_MINUS_2),
        addMod(z, mulMod(r, d, N), N),
        N
      )
      if (s.isZero()) continue
      return derEncode(r, s).toHex()
    }
  }

  /** Verifies a DER-hex SHA256withECDSA signature, as AuthorKeys.verify. */
  fun verify(publicKeyHex: String, message: String, signatureHex: String): Boolean {
    if (publicKeyHex.isBlank() || signatureHex.isBlank()) return false
    val pub = publicKeyHex.hexToBytes() ?: return false
    val point = decompress(pub) ?: return false
    val sig = signatureHex.hexToBytes() ?: return false
    val rs = derDecode(sig) ?: return false
    val r = rs.first
    val s = rs.second
    if (r.isZero() || r >= N || s.isZero() || s >= N) return false
    val z = reducedHash(message)
    val w = invMod(s, N, N_MINUS_2)
    val u1 = mulMod(z, w, N)
    val u2 = mulMod(r, w, N)
    val sum = add(scalarMul(u1, generator()), scalarMul(u2, point))
    val affine = toAffine(sum) ?: return false
    return modN(affine.first) == r
  }

  /** The message hash as an integer below the group order. */
  private fun reducedHash(message: String): U256 {
    val z = U256.fromBytes32(sha256Digest(message.encodeToByteArray()))
    return if (z >= N) subMod(z, N, N) else z
  }

  private fun modN(v: U256): U256 = if (v >= N) subMod(v, N, N) else v

  private fun derEncode(r: U256, s: U256): ByteArray {
    val rb = derInt(r)
    val sb = derInt(s)
    val out = ByteArray(2 + rb.size + sb.size)
    out[0] = 0x30
    out[1] = (rb.size + sb.size).toByte()
    rb.copyInto(out, 2)
    sb.copyInto(out, 2 + rb.size)
    return out
  }

  private fun derInt(v: U256): ByteArray {
    var bytes = v.toBytes32()
    var start = 0
    while (start < 31 && bytes[start] == 0.toByte() && (bytes[start + 1].toInt() and 0x80) == 0) {
      start++
    }
    bytes = bytes.copyOfRange(start, 32)
    val needsPad = (bytes[0].toInt() and 0x80) != 0
    val out = ByteArray(2 + bytes.size + if (needsPad) 1 else 0)
    out[0] = 0x02
    out[1] = (bytes.size + if (needsPad) 1 else 0).toByte()
    if (needsPad) {
      bytes.copyInto(out, 3)
    } else {
      bytes.copyInto(out, 2)
    }
    return out
  }

  private fun derDecode(sig: ByteArray): Pair<U256, U256>? {
    if (sig.size < 8 || sig[0] != 0x30.toByte()) return null
    if ((sig[1].toInt() and 0xFF) != sig.size - 2) return null
    var i = 2
    if (sig[i] != 0x02.toByte()) return null
    val lr = sig[i + 1].toInt() and 0xFF
    if (lr == 0 || lr > 33 || i + 2 + lr > sig.size) return null
    val r = derIntValue(sig, i + 2, lr) ?: return null
    i += 2 + lr
    if (i + 2 > sig.size || sig[i] != 0x02.toByte()) return null
    val ls = sig[i + 1].toInt() and 0xFF
    if (ls == 0 || ls > 33 || i + 2 + ls != sig.size) return null
    val s = derIntValue(sig, i + 2, ls) ?: return null
    return Pair(r, s)
  }

  private fun derIntValue(sig: ByteArray, offset: Int, length: Int): U256? {
    var start = offset
    var len = length
    if (len == 33) {
      if (sig[start] != 0.toByte()) return null
      start++
      len = 32
    }
    if (len > 32) return null
    val bytes = ByteArray(32)
    sig.copyInto(bytes, 32 - len, start, start + len)
    return U256.fromBytes32(bytes)
  }
}

/** Lowercase hex, as used throughout the bookkeeping. */
fun ByteArray.toHex(): String {
  val digits = "0123456789abcdef"
  val out = StringBuilder(size * 2)
  for (b in this) {
    out.append(digits[(b.toInt() shr 4) and 0xF])
    out.append(digits[b.toInt() and 0xF])
  }
  return out.toString()
}

/** The bytes of a hex string, or null when it is not even-length hex. */
fun String.hexToBytes(): ByteArray? {
  if (length % 2 != 0) return null
  val out = ByteArray(length / 2)
  for (i in out.indices) {
    val hi = this[2 * i].digitToIntOrNull(16) ?: return null
    val lo = this[2 * i + 1].digitToIntOrNull(16) ?: return null
    out[i] = ((hi shl 4) or lo).toByte()
  }
  return out
}
