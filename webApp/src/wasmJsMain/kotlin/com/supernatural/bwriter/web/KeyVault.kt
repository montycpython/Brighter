package com.supernatural.bwriter.web

import com.supernatural.bwriter.crypto.Secp256k1
import com.supernatural.bwriter.crypto.Secp256k1SelfTest
import com.supernatural.bwriter.crypto.hexToBytes
import com.supernatural.bwriter.crypto.toHex

/**
 * The author's key vault, bound to the signed-in Gmail account as a
 * requirement: a key is created or imported only inside that sign-in,
 * the sealed record is stored under the account's email, and the key's
 * address is registered to the account. The sealed form is the Android
 * backup format itself (PBKDF2-HMAC-SHA256, 200,000 iterations,
 * AES-256-GCM over the 32-byte scalar), so one format serves both apps.
 */
object KeyVault {

  private fun storeKey(email: String): String =
    "bwriter.vault." + email.lowercase().trim()

  /** The sealed record stored for this account, or null when none. */
  fun sealed(email: String): String? =
    vaultStored(storeKey(email)).ifBlank { null }

  /** The address carried in the sealed record, readable while locked. */
  fun storedAddress(email: String): String {
    val record = sealed(email) ?: return ""
    return jsonStringField(record, "address")
  }

  /**
   * Creates a fresh key for the account, seals it under the passphrase,
   * stores it, and registers the binding. Delivers the address, or ""
   * when anything failed — including the arithmetic self-test.
   */
  fun create(email: String, passphrase: String, onDone: (String) -> Unit) {
    if (passphrase.length < 8 || !Secp256k1SelfTest.run()) {
      onDone("")
      return
    }
    val scalar = Secp256k1.generatePrivateKey {
      secureRandomHex().hexToBytes() ?: ByteArray(0)
    }
    val address = Secp256k1.addressFromPrivateKey(scalar)
    sealKey(passphrase, scalar.toHex(), address) { json ->
      if (json.isBlank()) {
        onDone("")
      } else {
        vaultStore(storeKey(email), json)
        FirestoreBooks.registerAuthorKey(
          email, address, Secp256k1.publicKeyHex(scalar)
        )
        onDone(address)
      }
    }
  }

  /**
   * Imports an Android (or web) backup: opens it with the passphrase,
   * proves the scalar derives the backup's address — or, when the backup
   * carries none, proves it by a sign-and-verify probe, as Android does —
   * then stores the record under this account. Delivers the address.
   */
  fun import(email: String, backupJson: String, passphrase: String, onDone: (String) -> Unit) {
    if (!Secp256k1SelfTest.run()) {
      onDone("")
      return
    }
    openKey(passphrase, backupJson) { scalarHex ->
      val scalar = scalarHex.hexToBytes()
      if (scalar == null || scalar.size != 32) {
        onDone("")
        return@openKey
      }
      val address = Secp256k1.addressFromPrivateKey(scalar)
      val expected = jsonStringField(backupJson, "address")
      val proven = if (expected.isNotBlank()) {
        expected == address
      } else {
        val probe = Secp256k1.sign(scalar, "bwriter-key-restore-check") {
          secureRandomHex().hexToBytes() ?: ByteArray(0)
        }
        probe.isNotBlank() &&
          Secp256k1.verify(Secp256k1.publicKeyHex(scalar), "bwriter-key-restore-check", probe)
      }
      if (!proven) {
        onDone("")
      } else {
        vaultStore(storeKey(email), backupJson)
        FirestoreBooks.registerAuthorKey(
          email, address, Secp256k1.publicKeyHex(scalar)
        )
        onDone(address)
      }
    }
  }

  /** Unlocks the stored key; delivers the scalar, or null when it cannot open. */
  fun unlock(email: String, passphrase: String, onDone: (ByteArray?) -> Unit) {
    val record = sealed(email)
    if (record == null) {
      onDone(null)
      return
    }
    openKey(passphrase, record) { scalarHex ->
      onDone(scalarHex.hexToBytes()?.takeIf { it.size == 32 })
    }
  }

  /** Signs with an unlocked scalar, drawing ephemeral keys from the browser. */
  fun sign(scalar: ByteArray, message: String): String =
    Secp256k1.sign(scalar, message) {
      secureRandomHex().hexToBytes() ?: ByteArray(0)
    }

  /** Forgets the stored key on this device. The account keeps its books;
   *  the key itself survives only in the author's backup. */
  fun forget(email: String) {
    vaultRemove(storeKey(email))
  }
}
