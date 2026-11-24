package com.supernatural.bwriter.crypto

/**
 * RIPEMD-160, implemented from the specification (Dobbertin, Bosselaers,
 * Preneel) in pure Kotlin: the author-address derivation needs it on every
 * platform, and WebCrypto has no RIPEMD. Verified against the published
 * test suite and OpenSSL (see the crypto proof in the project notes).
 */
object Ripemd160 {

  private val RL = intArrayOf(
    0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15,
    7, 4, 13, 1, 10, 6, 15, 3, 12, 0, 9, 5, 2, 14, 11, 8,
    3, 10, 14, 4, 9, 15, 8, 1, 2, 7, 0, 6, 13, 11, 5, 12,
    1, 9, 11, 10, 0, 8, 12, 4, 13, 3, 7, 15, 14, 5, 6, 2,
    4, 0, 5, 9, 7, 12, 2, 10, 14, 1, 3, 8, 11, 6, 15, 13
  )
  private val RR = intArrayOf(
    5, 14, 7, 0, 9, 2, 11, 4, 13, 6, 15, 8, 1, 10, 3, 12,
    6, 11, 3, 7, 0, 13, 5, 10, 14, 15, 8, 12, 4, 9, 1, 2,
    15, 5, 1, 3, 7, 14, 6, 9, 11, 8, 12, 2, 10, 0, 4, 13,
    8, 6, 4, 1, 3, 11, 15, 0, 5, 12, 2, 13, 9, 7, 10, 14,
    12, 15, 10, 4, 1, 5, 8, 7, 6, 2, 13, 14, 0, 3, 9, 11
  )
  private val SL = intArrayOf(
    11, 14, 15, 12, 5, 8, 7, 9, 11, 13, 14, 15, 6, 7, 9, 8,
    7, 6, 8, 13, 11, 9, 7, 15, 7, 12, 15, 9, 11, 7, 13, 12,
    11, 13, 6, 7, 14, 9, 13, 15, 14, 8, 13, 6, 5, 12, 7, 5,
    11, 12, 14, 15, 14, 15, 9, 8, 9, 14, 5, 6, 8, 6, 5, 12,
    9, 15, 5, 11, 6, 8, 13, 12, 5, 12, 13, 14, 11, 8, 5, 6
  )
  private val SR = intArrayOf(
    8, 9, 9, 11, 13, 15, 15, 5, 7, 7, 8, 11, 14, 14, 12, 6,
    9, 13, 15, 7, 12, 8, 9, 11, 7, 7, 12, 7, 6, 15, 13, 11,
    9, 7, 15, 11, 8, 6, 6, 14, 12, 13, 5, 14, 13, 13, 7, 5,
    15, 5, 8, 11, 14, 14, 6, 14, 6, 9, 12, 9, 12, 5, 15, 8,
    8, 5, 12, 9, 12, 5, 14, 6, 8, 13, 6, 5, 15, 13, 11, 11
  )
  private val KL = intArrayOf(0x00000000, 0x5A827999, 0x6ED9EBA1, 0x8F1BBCDC.toInt(), 0xA953FD4E.toInt())
  private val KR = intArrayOf(0x50A28BE6, 0x5C4DD124, 0x6D703EF3, 0x7A6D76E9, 0x00000000)

  private fun f(j: Int, x: Int, y: Int, z: Int): Int = when {
    j < 16 -> x xor y xor z
    j < 32 -> (x and y) or (x.inv() and z)
    j < 48 -> (x or y.inv()) xor z
    j < 64 -> (x and z) or (y and z.inv())
    else -> x xor (y or z.inv())
  }

  /** The 20-byte RIPEMD-160 digest of [data]. */
  fun digest(data: ByteArray): ByteArray {
    val bitLength = data.size.toLong() * 8
    val paddedLength = ((data.size + 9 + 63) / 64) * 64
    val msg = ByteArray(paddedLength)
    data.copyInto(msg)
    msg[data.size] = 0x80.toByte()
    for (i in 0 until 8) {
      msg[paddedLength - 8 + i] = (bitLength ushr (8 * i)).toByte()
    }
    val h = intArrayOf(0x67452301, 0xEFCDAB89.toInt(), 0x98BADCFE.toInt(), 0x10325476, 0xC3D2E1F0.toInt())
    val x = IntArray(16)
    var offset = 0
    while (offset < paddedLength) {
      for (i in 0 until 16) {
        val p = offset + 4 * i
        x[i] = (msg[p].toInt() and 0xFF) or
          ((msg[p + 1].toInt() and 0xFF) shl 8) or
          ((msg[p + 2].toInt() and 0xFF) shl 16) or
          ((msg[p + 3].toInt() and 0xFF) shl 24)
      }
      var al = h[0]; var bl = h[1]; var cl = h[2]; var dl = h[3]; var el = h[4]
      var ar = h[0]; var br = h[1]; var cr = h[2]; var dr = h[3]; var er = h[4]
      for (j in 0 until 80) {
        val t = ((al + f(j, bl, cl, dl) + x[RL[j]] + KL[j / 16]).rotateLeft(SL[j]) + el)
        al = el; el = dl; dl = cl.rotateLeft(10); cl = bl; bl = t
        val t2 = ((ar + f(79 - j, br, cr, dr) + x[RR[j]] + KR[j / 16]).rotateLeft(SR[j]) + er)
        ar = er; er = dr; dr = cr.rotateLeft(10); cr = br; br = t2
      }
      val t = h[1] + cl + dr
      h[1] = h[2] + dl + er
      h[2] = h[3] + el + ar
      h[3] = h[4] + al + br
      h[4] = h[0] + bl + cr
      h[0] = t
      offset += 64
    }
    val out = ByteArray(20)
    for (i in 0 until 5) {
      for (j in 0 until 4) {
        out[4 * i + j] = (h[i] ushr (8 * j)).toByte()
      }
    }
    return out
  }
}
