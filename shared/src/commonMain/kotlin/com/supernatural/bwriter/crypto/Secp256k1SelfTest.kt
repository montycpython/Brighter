package com.supernatural.bwriter.crypto

/**
 * Known-answer checks for the identity math, from published vectors and
 * from cross-implementation proofs against OpenSSL. The key vault runs
 * these before it trusts a key: if any check fails, the platform's
 * arithmetic cannot be trusted with an author's identity.
 */
object Secp256k1SelfTest {

  fun run(): Boolean {
    // RIPEMD-160, the published vector for "abc".
    if (Ripemd160.digest("abc".encodeToByteArray()).toHex() !=
      "8eb208f7e05d987a9b044a8e98c6b087f15a0bfc"
    ) return false

    // Private key 1: the generator point itself, and the famous HASH160
    // of its compressed public key — the address our formula must give.
    val one = ByteArray(32)
    one[31] = 1
    if (Secp256k1.publicKeyHex(one) !=
      "0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798"
    ) return false
    if (Secp256k1.addressFromPrivateKey(one) !=
      "751e76e8199196d454941c45d1b3a323f1433bd6"
    ) return false

    // A fixed ephemeral key makes signing deterministic here: the exact
    // DER bytes were proven against OpenSSL and the reference math.
    val fixedK = ByteArray(32)
    fixedK[30] = 0xB1.toByte()
    fixedK[31] = 0x0C
    val expected =
      "3044022050f7da326e5a243b38a4d6de345354a4313081c397cdf8b387277183ba7dc928" +
        "02200bc55b2d9ea596c22f6270f78639a8a0cf86b788df5ae700b12d47dae9806488"
    val sig = Secp256k1.sign(one, "bwriter") { fixedK }
    if (sig != expected) return false
    if (!Secp256k1.verify(Secp256k1.publicKeyHex(one), "bwriter", sig)) return false
    if (Secp256k1.verify(Secp256k1.publicKeyHex(one), "bwriter!", sig)) return false
    return true
  }
}
