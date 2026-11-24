package com.supernatural.bwriter.web

import kotlin.js.JsAny
import kotlin.js.JsString
import kotlin.js.toJsString

// The browser side of the key vault: WebCrypto sealing in the Android
// backup format, secure randomness, and local storage for the sealed
// key. All values cross the boundary as strings, as everywhere here.

private fun cryptoSealKey(
  passphrase: JsString,
  scalarHex: JsString,
  address: JsString,
  callback: (JsString) -> Unit
): Unit =
  js("{ window.BwriterCrypto.sealKey(passphrase, scalarHex, address).then((j) => callback(j)).catch(() => callback('')); }")

private fun cryptoOpenKey(
  passphrase: JsString,
  json: JsString,
  callback: (JsString) -> Unit
): Unit =
  js("{ window.BwriterCrypto.openKey(passphrase, json).then((h) => callback(h)).catch(() => callback('')); }")

private fun randomHex32(): JsString =
  js("{ return window.BwriterRandom.hex32(); }")

private fun vaultStoreGet(key: JsString): JsString =
  js("{ return window.BwriterVaultStore.get(key); }")

private fun vaultStoreSet(key: JsString, value: JsString): Unit =
  js("{ window.BwriterVaultStore.set(key, value); }")

private fun vaultStoreRemove(key: JsString): Unit =
  js("{ window.BwriterVaultStore.remove(key); }")

/** Thirty-two secure random bytes, as hex — the vault's randomness. */
fun secureRandomHex(): String = randomHex32().toString()

/** Seals a private scalar under a passphrase, in the backup format. */
fun sealKey(passphrase: String, scalarHex: String, address: String, onDone: (String) -> Unit) {
  cryptoSealKey(passphrase.toJsString(), scalarHex.toJsString(), address.toJsString()) { json ->
    onDone(json.toString())
  }
}

/** Opens a sealed key; delivers the scalar hex, or "" when it cannot open. */
fun openKey(passphrase: String, sealedJson: String, onDone: (String) -> Unit) {
  cryptoOpenKey(passphrase.toJsString(), sealedJson.toJsString()) { hex ->
    onDone(hex.toString())
  }
}

/** Reads one sealed-vault record from local storage, "" when absent. */
fun vaultStored(key: String): String = vaultStoreGet(key.toJsString()).toString()

/** Writes one sealed-vault record to local storage. */
fun vaultStore(key: String, value: String) {
  vaultStoreSet(key.toJsString(), value.toJsString())
}

/** Removes one sealed-vault record from local storage. */
fun vaultRemove(key: String) {
  vaultStoreRemove(key.toJsString())
}
