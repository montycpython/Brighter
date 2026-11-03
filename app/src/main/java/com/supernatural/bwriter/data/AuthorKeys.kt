package com.supernatural.bwriter.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import org.bouncycastle.crypto.digests.RIPEMD160Digest
import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.jcajce.provider.asymmetric.util.EC5Util
import java.math.BigInteger
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.security.Signature
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.security.spec.ECParameterSpec
import java.security.spec.ECPrivateKeySpec
import java.security.spec.ECPublicKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * The author's own secp256k1 keypair, generated once and kept on this
 * device. The private key is stored AES-GCM-encrypted, the AES key held
 * by the Android Keystore. The address — RIPEMD160 of SHA-256 of the
 * compressed public key — is the author's unique ID, and the key signs
 * the stamp of every synced version of a book.
 */
object AuthorKeys {

    private const val PREFS = "author_keys"
    private const val KEY_PUB = "pub_hex"
    private const val KEY_PRIV_IV = "priv_iv"
    private const val KEY_PRIV_CT = "priv_ct"
    private const val AES_ALIAS = "bwriter_author_key"

    private val provider = BouncyCastleProvider()

    fun ensureKeyPair(context: Context): Boolean {
        return try {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            if (prefs.getString(KEY_PUB, null) != null && prefs.getString(KEY_PRIV_CT, null) != null) {
                return true
            }
            val generator = KeyPairGenerator.getInstance("EC", provider)
            generator.initialize(ECGenParameterSpec("secp256k1"))
            val keyPair = generator.generateKeyPair()
            val priv = keyPair.private as ECPrivateKey
            val pub = keyPair.public as ECPublicKey
            val pubBytes = compressedPubBytes(pub)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, aesKey())
            val encrypted = cipher.doFinal(priv.s.toFixed32())
            prefs.edit()
                .putString(KEY_PUB, pubBytes.toHex())
                .putString(KEY_PRIV_IV, cipher.iv.toHex())
                .putString(KEY_PRIV_CT, encrypted.toHex())
                .apply()
            true
        } catch (e: Exception) {
            false
        }
    }

    fun publicKeyHex(context: Context): String {
        return try {
            ensureKeyPair(context)
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_PUB, null) ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    /** The author's address: hex of RIPEMD160(SHA-256(compressed public key)). */
    fun address(context: Context): String {
        return try {
            val pubHex = publicKeyHex(context)
            if (pubHex.isBlank()) return ""
            val sha = MessageDigest.getInstance("SHA-256").digest(pubHex.hexToBytes())
            val ripe = RIPEMD160Digest()
            ripe.update(sha, 0, sha.size)
            val out = ByteArray(ripe.digestSize)
            ripe.doFinal(out, 0)
            out.toHex()
        } catch (e: Exception) {
            ""
        }
    }

    /** SHA256withECDSA over the message's UTF-8 bytes; the signature as hex. */
    fun sign(context: Context, message: String): String {
        return try {
            val key = loadPrivateKey(context) ?: return ""
            val sig = Signature.getInstance("SHA256withECDSA", provider)
            sig.initSign(key)
            sig.update(message.toByteArray(Charsets.UTF_8))
            sig.sign().toHex()
        } catch (e: Exception) {
            ""
        }
    }

    fun verify(publicKeyHex: String, message: String, signatureHex: String): Boolean {
        return try {
            if (publicKeyHex.isBlank() || signatureHex.isBlank()) return false
            val key = publicKeyFromHex(publicKeyHex) ?: return false
            val sig = Signature.getInstance("SHA256withECDSA", provider)
            sig.initVerify(key)
            sig.update(message.toByteArray(Charsets.UTF_8))
            sig.verify(signatureHex.hexToBytes())
        } catch (e: Exception) {
            false
        }
    }

    private fun aesKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getEntry(AES_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                AES_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }

    private fun loadPrivateKey(context: Context): ECPrivateKey? {
        return try {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val ivHex = prefs.getString(KEY_PRIV_IV, null) ?: return null
            val ctHex = prefs.getString(KEY_PRIV_CT, null) ?: return null
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, aesKey(), GCMParameterSpec(128, ivHex.hexToBytes()))
            val d = BigInteger(1, cipher.doFinal(ctHex.hexToBytes()))
            KeyFactory.getInstance("EC", provider)
                .generatePrivate(ECPrivateKeySpec(d, jcaParams())) as ECPrivateKey
        } catch (e: Exception) {
            null
        }
    }

    private fun publicKeyFromHex(pubHex: String): ECPublicKey? {
        return try {
            val spec = ECNamedCurveTable.getParameterSpec("secp256k1")
            val point = spec.curve.decodePoint(pubHex.hexToBytes())
            KeyFactory.getInstance("EC", provider).generatePublic(
                ECPublicKeySpec(EC5Util.convertPoint(point), jcaParams())
            ) as ECPublicKey
        } catch (e: Exception) {
            null
        }
    }

    private fun compressedPubBytes(pub: ECPublicKey): ByteArray {
        val curve = ECNamedCurveTable.getParameterSpec("secp256k1").curve
        return curve.createPoint(pub.w.affineX, pub.w.affineY).getEncoded(true)
    }

    private fun jcaParams(): ECParameterSpec =
        EC5Util.convertSpec(ECNamedCurveTable.getParameterSpec("secp256k1"))

    private fun BigInteger.toFixed32(): ByteArray {
        val raw = toByteArray()
        return when {
            raw.size == 32 -> raw
            raw.size > 32 -> raw.copyOfRange(raw.size - 32, raw.size)
            else -> ByteArray(32 - raw.size) + raw
        }
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    private fun String.hexToBytes(): ByteArray {
        val out = ByteArray(length / 2)
        for (i in out.indices) {
            out[i] = substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
        return out
    }
}
