package com.supernatural.bwriter.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.util.Base64
import android.security.keystore.KeyProperties
import org.bouncycastle.crypto.digests.RIPEMD160Digest
import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.json.JSONObject
import org.bouncycastle.jcajce.provider.asymmetric.util.EC5Util
import java.math.BigInteger
import java.security.AlgorithmParameters
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
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
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

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
    private const val PBKDF2_ITERATIONS = 200_000

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

    fun isUsable(context: Context): Boolean {
        return try {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val hasMaterial = prefs.getString(KEY_PUB, null) != null || prefs.getString(KEY_PRIV_CT, null) != null
            if (!hasMaterial) {
                true
            } else {
                loadPrivateKey(context) != null
            }
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
            addressFromPubKeyHex(publicKeyHex(context))
        } catch (e: Exception) {
            ""
        }
    }

    fun addressFromPubKeyHex(publicKeyHex: String): String {
        if (publicKeyHex.isBlank()) return ""
        val sha = MessageDigest.getInstance("SHA-256").digest(publicKeyHex.hexToBytes())
        val ripe = RIPEMD160Digest()
        ripe.update(sha, 0, sha.size)
        val out = ByteArray(ripe.digestSize)
        ripe.doFinal(out, 0)
        return out.toHex()
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

    /**
     * The private key sealed under a passphrase, for a copy kept in the
     * writer's own Drive: PBKDF2-HMAC-SHA256 (200,000 rounds) deriving
     * the AES-256-GCM key. The sealed bytes are the raw 32-byte private
     * scalar, the same form this object stores. Returns "" on failure.
     */
    fun exportEncrypted(context: Context, passphrase: String): String {
        return try {
            if (passphrase.length < 8) return ""
            if (!ensureKeyPair(context)) return ""
            val priv = loadPrivateKey(context) ?: return ""
            val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
            val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, passphraseKey(passphrase, salt), GCMParameterSpec(128, iv))
            val sealed = cipher.doFinal(priv.s.toFixed32())
            JSONObject().apply {
                put("v", 1)
                put("kdf", "PBKDF2-HMAC-SHA256")
                put("iter", PBKDF2_ITERATIONS)
                put("form", "ec-private-scalar-32")
                put("salt", Base64.encodeToString(salt, Base64.NO_WRAP))
                put("iv", Base64.encodeToString(iv, Base64.NO_WRAP))
                put("data", Base64.encodeToString(sealed, Base64.NO_WRAP))
                put("address", address(context))
            }.toString()
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Restore a keypair sealed by [exportEncrypted]. A wrong passphrase
     * fails the GCM tag check and leaves the current keypair untouched.
     * On success the restored keypair replaces the stored one, and the
     * check is that the restored address matches the sealed address.
     */
    fun importEncrypted(context: Context, json: String, passphrase: String): Boolean {
        return try {
            val root = JSONObject(json)
            val salt = Base64.decode(root.getString("salt"), Base64.NO_WRAP)
            val iv = Base64.decode(root.getString("iv"), Base64.NO_WRAP)
            val sealed = Base64.decode(root.getString("data"), Base64.NO_WRAP)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, passphraseKey(passphrase, salt), GCMParameterSpec(128, iv))
            val scalar = cipher.doFinal(sealed)
            if (scalar.size != 32) return false
            val curveSpec = ECNamedCurveTable.getParameterSpec("secp256k1")
            val point = curveSpec.g.multiply(BigInteger(1, scalar))
            val pubBytes = point.getEncoded(true)
            val expected = root.optString("address")
            val verified = if (expected.isNotBlank()) {
                addressFromPubKeyHex(pubBytes.toHex()) == expected
            } else {
                val privKey = KeyFactory.getInstance("EC", provider)
                    .generatePrivate(ECPrivateKeySpec(BigInteger(1, scalar), jcaParams())) as ECPrivateKey
                val probe = "bwriter-key-restore-check"
                val probeSig = Signature.getInstance("SHA256withECDSA", provider)
                probeSig.initSign(privKey)
                probeSig.update(probe.toByteArray(Charsets.UTF_8))
                verify(pubBytes.toHex(), probe, probeSig.sign().toHex())
            }
            if (!verified) return false
            val storeCipher = Cipher.getInstance("AES/GCM/NoPadding")
            storeCipher.init(Cipher.ENCRYPT_MODE, aesKey())
            val stored = storeCipher.doFinal(scalar)
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString(KEY_PUB, pubBytes.toHex())
                .putString(KEY_PRIV_IV, storeCipher.iv.toHex())
                .putString(KEY_PRIV_CT, stored.toHex())
                .apply()
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun passphraseKey(passphrase: String, salt: ByteArray): SecretKey {
        val spec = PBEKeySpec(passphrase.toCharArray(), salt, PBKDF2_ITERATIONS, 256)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return SecretKeySpec(factory.generateSecret(spec).encoded, "AES")
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

    private fun jcaParams(): ECParameterSpec {
        val ap = AlgorithmParameters.getInstance("EC")
        ap.init(ECGenParameterSpec("secp256k1"))
        return ap.getParameterSpec(ECParameterSpec::class.java)
    }

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
