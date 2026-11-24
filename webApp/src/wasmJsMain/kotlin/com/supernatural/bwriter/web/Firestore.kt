package com.supernatural.bwriter.web

import kotlin.js.JsAny
import kotlin.js.JsString
import kotlin.js.js
import kotlin.js.toJsString

// Hand-written interop to Cloud Firestore through the Firebase web SDK.
// There are no official Firebase bindings for Kotlin/Wasm, so each snippet
// below maps onto documented modular SDK calls (firebase/app and
// firebase/firestore, v11):
//
//   getApp()                       -> window.BwriterFirebase.getApp
//   getFirestore(app)              -> window.BwriterFirestore.getFirestore
//   collection(db, "registry")     -> window.BwriterFirestore.collection
//   getDocs(query)                 -> window.BwriterFirestore.getDocs
//
// index.html loads the SDK from the Firebase CDN and exposes those functions
// before this module starts. The app instance comes from getApp(): the auth
// layer has already initialized the default app by the time any screen that
// reads Firestore can appear (those screens require a signed-in user).
//
// The registry document fields read here are the exact fields the Android
// build writes (registryToMap in FirestoreNetworkService): title, subtitle,
// authorName, workType, wordCount, birthId. The document id is the fileId.

private fun firestoreDb(): JsAny =
  js("window.BwriterFirestore.getFirestore(window.BwriterFirebase.getApp())")

private fun firestoreFetchRegistry(db: JsAny, callback: (JsString) -> Unit): Unit =
  js("{ const F = window.BwriterFirestore; F.getDocs(F.collection(db, 'registry')).then((snap) => { const rows = []; snap.forEach((d) => { const v = d.data(); rows.push({ id: d.id, title: v.title || '', subtitle: v.subtitle || '', authorName: v.authorName || '', workType: v.workType || '', wordCount: v.wordCount || 0, birthId: v.birthId || '' }); }); callback(JSON.stringify(rows)); }).catch((e) => { console.error('Bwriter registry fetch failed', e); callback('[]'); }); }")


private fun firestoreFetchBookText(db: JsAny, fileId: JsString, callback: (JsString) -> Unit): Unit =
  js("{ const F = window.BwriterFirestore; const ref = F.doc(db, 'registry', fileId, 'content', 'fullText'); F.getDoc(ref).then((snap) => { const v = snap.exists() ? snap.data() : null; callback(v && v.payload ? v.payload : ''); }).catch((e) => { console.error('Bwriter book fetch failed', e); callback(''); }); }")


private fun firestoreFetchMyBooks(db: JsAny, email: JsString, callback: (JsString) -> Unit): Unit =
  js("{ const F = window.BwriterFirestore; const q = F.query(F.collection(db, 'cloudBooks'), F.where('ownerEmail', '==', email)); F.getDocs(q).then((snap) => { const rows = []; snap.forEach((d) => { const v = d.data(); rows.push({ id: d.id, birthId: v.birthId || '', payload: v.payload || '', updatedAt: v.updatedAt || 0 }); }); callback(JSON.stringify(rows)); }).catch((e) => { console.error('Bwriter my-books fetch failed', e); callback('[]'); }); }")

/**
 * One of the signed-in author's own cloud books (the cloudBooks collection
 * the Android build syncs to). The book's text rides in [payload], the same
 * manuscript JSON the registry content documents carry.
 */
data class MyBook(
  val id: String,
  val birthId: String,
  val title: String,
  val subtitle: String,
  val authorName: String,
  val wordCount: Long,
  val updatedAt: Long,
  val payload: String
)


private fun firestoreFetchWallet(db: JsAny, email: JsString, callback: (JsString) -> Unit): Unit =
  js("{ const F = window.BwriterFirestore; const q = (col, field) => F.getDocs(F.query(F.collection(db, col), F.where(field, '==', email))); Promise.all([q('creditClaims', 'authorEmail'), q('creditIssuances', 'authorEmail'), q('creditTransfers', 'fromEmail'), q('creditTransfers', 'toEmail'), q('creditSpends', 'fromEmail'), q('shareUnits', 'holderEmail')]).then(([claims, issuances, sent, received, spends, units]) => { let claimedSum = 0; const claimedIds = new Set(); claims.forEach((d) => { const v = d.data(); claimedSum += v.amount || 0; claimedIds.add(v.issuanceHash); }); const unclaimed = []; issuances.forEach((d) => { const v = d.data(); if (!claimedIds.has(v.issuanceHash)) unclaimed.push({ amount: v.amount || 0, kind: v.kind || '', bonus: !!v.bonus }); }); let sentSum = 0; sent.forEach((d) => { sentSum += d.data().amount || 0; }); let receivedSum = 0; received.forEach((d) => { receivedSum += d.data().amount || 0; }); let spentSum = 0; spends.forEach((d) => { spentSum += d.data().amount || 0; }); const unitRows = []; units.forEach((d) => { const v = d.data(); unitRows.push({ role: v.role || '', seasonYear: v.seasonYear || 0, vested: !!v.vested }); }); callback(JSON.stringify({ claimed: claimedSum, unclaimed: unclaimed, sent: sentSum, received: receivedSum, spent: spentSum, units: unitRows })); }).catch((e) => { console.error('Bwriter wallet fetch failed', e); callback(''); }); }")

/** An issuance the author has not yet mined into claimed credit. */
data class UnclaimedCredit(
  val amount: Long,
  val kind: String,
  val bonus: Boolean
)

/** One share unit held by the author (one shareUnits document). */
data class UnitGrantView(
  val role: String,
  val seasonYear: Long,
  val vested: Boolean
)

/**
 * The author's wallet, computed exactly as the Android build computes it:
 * balance = claimed - sent + received - spent, over the creditClaims,
 * creditTransfers and creditSpends collections; units from shareUnits.
 */
data class WalletData(
  val claimed: Long,
  val unclaimed: List<UnclaimedCredit>,
  val sent: Long,
  val received: Long,
  val spent: Long,
  val units: List<UnitGrantView>
) {
  val balance: Long get() = claimed - sent + received - spent
}


private fun firestoreFetchMail(db: JsAny, email: JsString, callback: (JsString) -> Unit): Unit =
  js("{ const F = window.BwriterFirestore; const q = F.query(F.collection(db, 'mail'), F.where('recipientEmail', '==', email)); F.getDocs(q).then((snap) => { const rows = []; snap.forEach((d) => { const v = d.data(); rows.push({ id: d.id, senderName: v.senderName || '', senderEmail: v.senderEmail || '', subject: v.subject || '', body: v.body || '', timestamp: v.timestamp || 0, isRead: !!v.isRead, manuscriptTitle: v.manuscriptTitle || '', messageType: v.messageType || '' }); }); callback(JSON.stringify(rows)); }).catch((e) => { console.error('Bwriter mail fetch failed', e); callback(''); }); }")

private fun firestoreMarkMailRead(db: JsAny, id: JsString): Unit =
  js("{ const F = window.BwriterFirestore; F.updateDoc(F.doc(db, 'mail', id), { isRead: true }).catch((e) => console.error('Bwriter mail mark-read failed', e)); }")

/** One message in the author's in-app mailbox (the mail collection). */
data class MailMessage(
  val id: String,
  val senderName: String,
  val senderEmail: String,
  val subject: String,
  val body: String,
  val timestamp: Long,
  val isRead: Boolean,
  val manuscriptTitle: String,
  val messageType: String
)


private fun firestoreRegisterAuthorKey(db: JsAny, email: JsString, address: JsString, pubHex: JsString): Unit =
  js("{ const F = window.BwriterFirestore; const ref = F.doc(db, 'users', email); F.setDoc(ref, { email: email, authorAddress: address, authorPubKey: pubHex }, { merge: true }).catch((e) => console.error('Bwriter key registration failed', e)); }")


private fun firestoreDocText(db: JsAny, path: JsString, callback: (JsString) -> Unit): Unit =
  js("{ const F = window.BwriterFirestore; const ref = F.doc(db, ...path.split('/')); F.getDoc(ref).then((snap) => callback(snap.exists() ? JSON.stringify(snap.data()) : '')).catch(() => callback('ERR')); }")

private fun firestoreFindIndexEntry(db: JsAny, collection: JsString, manuscriptId: JsString, emailLower: JsString, callback: (JsString) -> Unit): Unit =
  js("{ const F = window.BwriterFirestore; const q = F.query(F.collection(db, collection), F.where('manuscriptId', '==', Number(manuscriptId)), F.where('authorEmailLower', '==', emailLower)); F.getDocs(q).then((snap) => { if (snap.empty) { callback(''); } else { const d = snap.docs[0]; callback(JSON.stringify(Object.assign({ __id: d.id }, d.data()))); } }).catch(() => callback('ERR')); }")

private fun syncSaveAll(opsJson: JsString, callback: (JsString) -> Unit): Unit =
  js("{ window.BwriterSync.saveAll(opsJson).then(() => callback('1')).catch((e) => { console.error('Bwriter sync failed', e); callback(''); }); }")

/** One row of the public registry, as written by the Android build. */
data class RegistryBook(
  val id: String,
  val title: String,
  val subtitle: String,
  val authorName: String,
  val workType: String,
  val wordCount: Long,
  val birthId: String
)

object FirestoreBooks {


  /** Registers the account's author key binding (address + public key). */
  fun registerAuthorKey(email: String, address: String, pubHex: String) {
    firestoreRegisterAuthorKey(
      firestoreDb(),
      email.lowercase().trim().toJsString(),
      address.toJsString(),
      pubHex.toJsString()
    )
  }


  /**
   * Reads one document as JSON text: the document's fields when it exists,
   * "" when it does not, and "ERR" when the read itself failed — the three
   * cases Android's sync distinguishes.
   */
  fun fetchDocText(path: String, onDone: (String) -> Unit) {
    firestoreDocText(firestoreDb(), path.toJsString()) { text ->
      onDone(text.toString())
    }
  }

  /** Finds an index entry by manuscript id and author, as Android does. */
  fun findIndexEntry(collection: String, manuscriptId: Long, email: String, onDone: (String) -> Unit) {
    firestoreFindIndexEntry(
      firestoreDb(),
      collection.toJsString(),
      manuscriptId.toString().toJsString(),
      email.lowercase().trim().toJsString()
    ) { text ->
      onDone(text.toString())
    }
  }

  /** Performs the sync's writes in order; delivers true when all landed. */
  fun saveAll(opsJson: String, onDone: (Boolean) -> Unit) {
    syncSaveAll(opsJson.toJsString()) { result ->
      onDone(result.toString() == "1")
    }
  }

  /** Fetches every registry document once and delivers the parsed rows. */
  fun fetchRegistry(onResult: (List<RegistryBook>) -> Unit) {
    firestoreFetchRegistry(firestoreDb()) { json ->
      onResult(parseRegistryBooks(json.toString()))
    }
  }

  /** Fetches one book's full-text payload (the manuscript JSON string). */
  fun fetchBookPayload(fileId: String, onResult: (String) -> Unit) {
    firestoreFetchBookText(firestoreDb(), fileId.toJsString()) { payload ->
      onResult(payload.toString())
    }
  }

  /** Fetches the author's own cloud books, newest sync first. */
  fun fetchMyBooks(email: String, onResult: (List<MyBook>) -> Unit) {
    firestoreFetchMyBooks(firestoreDb(), email.lowercase().trim().toJsString()) { json ->
      onResult(parseMyBooks(json.toString()))
    }
  }

  /** Fetches the author's wallet: credit balance, unclaimed credit, units. */
  fun fetchWallet(email: String, onResult: (WalletData?) -> Unit) {
    firestoreFetchWallet(firestoreDb(), email.lowercase().trim().toJsString()) { json ->
      onResult(parseWallet(json.toString()))
    }
  }

  /** Fetches the author's mailbox, newest first. Null on failure. */
  fun fetchMail(email: String, onResult: (List<MailMessage>?) -> Unit) {
    firestoreFetchMail(firestoreDb(), email.lowercase().trim().toJsString()) { json ->
      val text = json.toString()
      onResult(if (text.isBlank()) null else parseMail(text))
    }
  }

  /** Marks one message read, as opening it on Android does. */
  fun markMailRead(id: String) {
    firestoreMarkMailRead(firestoreDb(), id.toJsString())
  }
}

internal fun parseMyBooks(json: String): List<MyBook> {
  val books = ArrayList<MyBook>()
  var i = 0
  while (i < json.length) {
    if (json[i] == '{') {
      val end = jsonObjectEnd(json, i)
      if (end < 0) break
      val obj = json.substring(i, end + 1)
      val payload = jsonStringField(obj, "payload")
      books.add(
        MyBook(
          id = jsonStringField(obj, "id"),
          birthId = jsonStringField(obj, "birthId"),
          title = jsonStringField(payload, "title"),
          subtitle = jsonStringField(payload, "subtitle"),
          authorName = jsonStringField(payload, "authorName"),
          wordCount = parseBookSections(payload).sumOf { it.wordCount },
          updatedAt = jsonLongField(obj, "updatedAt"),
          payload = payload
        )
      )
      i = end + 1
    } else {
      i++
    }
  }
  return books.sortedByDescending { it.updatedAt }
}

internal fun parseWallet(json: String): WalletData? {
  if (json.isBlank()) return null
  return WalletData(
    claimed = jsonLongField(json, "claimed"),
    unclaimed = jsonObjectArray(json, "unclaimed").map { obj ->
      UnclaimedCredit(
        amount = jsonLongField(obj, "amount"),
        kind = jsonStringField(obj, "kind"),
        bonus = jsonBoolField(obj, "bonus")
      )
    },
    sent = jsonLongField(json, "sent"),
    received = jsonLongField(json, "received"),
    spent = jsonLongField(json, "spent"),
    units = jsonObjectArray(json, "units").map { obj ->
      UnitGrantView(
        role = jsonStringField(obj, "role"),
        seasonYear = jsonLongField(obj, "seasonYear"),
        vested = jsonBoolField(obj, "vested")
      )
    }
  )
}

/** The boolean value of "key" in a flat JSON object, or false when absent. */
internal fun jsonBoolField(obj: String, key: String): Boolean {
  val valueStart = jsonValueStart(obj, key) ?: return false
  return obj.startsWith("true", valueStart)
}

internal fun parseMail(json: String): List<MailMessage> {
  val out = ArrayList<MailMessage>()
  var i = 0
  while (i < json.length) {
    if (json[i] == '{') {
      val end = jsonObjectEnd(json, i)
      if (end < 0) break
      val obj = json.substring(i, end + 1)
      out.add(
        MailMessage(
          id = jsonStringField(obj, "id"),
          senderName = jsonStringField(obj, "senderName"),
          senderEmail = jsonStringField(obj, "senderEmail"),
          subject = jsonStringField(obj, "subject"),
          body = jsonStringField(obj, "body"),
          timestamp = jsonLongField(obj, "timestamp"),
          isRead = jsonBoolField(obj, "isRead"),
          manuscriptTitle = jsonStringField(obj, "manuscriptTitle"),
          messageType = jsonStringField(obj, "messageType")
        )
      )
      i = end + 1
    } else {
      i++
    }
  }
  return out.sortedByDescending { it.timestamp }
}

// Minimal JSON reading for exactly the shape firestoreFetchRegistry builds:
// an array of flat objects whose values are strings or one integer. String
// values are read with full JSON escape handling, so titles containing
// quotes, braces or backslashes cannot break the parse.

internal fun parseRegistryBooks(json: String): List<RegistryBook> {
  val books = ArrayList<RegistryBook>()
  var i = 0
  while (i < json.length) {
    if (json[i] == '{') {
      val end = jsonObjectEnd(json, i)
      if (end < 0) break
      val obj = json.substring(i, end + 1)
      books.add(
        RegistryBook(
          id = jsonStringField(obj, "id"),
          title = jsonStringField(obj, "title"),
          subtitle = jsonStringField(obj, "subtitle"),
          authorName = jsonStringField(obj, "authorName"),
          workType = jsonStringField(obj, "workType"),
          wordCount = jsonLongField(obj, "wordCount"),
          birthId = jsonStringField(obj, "birthId")
        )
      )
      i = end + 1
    } else {
      i++
    }
  }
  return books
}

/** Index of the '}' closing the object that starts at [start], or -1. */
internal fun jsonObjectEnd(json: String, start: Int): Int {
  var depth = 0
  var inString = false
  var escaped = false
  var i = start
  while (i < json.length) {
    val c = json[i]
    if (inString) {
      if (escaped) escaped = false
      else if (c == '\\') escaped = true
      else if (c == '"') inString = false
    } else {
      when (c) {
        '"' -> inString = true
        '{' -> depth++
        '}' -> {
          depth--
          if (depth == 0) return i
        }
      }
    }
    i++
  }
  return -1
}

/** The string value of "key" in a flat JSON object, or "" when absent. */
internal fun jsonStringField(obj: String, key: String): String {
  val valueStart = jsonValueStart(obj, key) ?: return ""
  if (valueStart >= obj.length || obj[valueStart] != '"') return ""
  val sb = StringBuilder()
  var i = valueStart + 1
  while (i < obj.length) {
    val c = obj[i]
    when {
      c == '\\' && i + 1 < obj.length -> {
        when (obj[i + 1]) {
          '"' -> sb.append('"')
          '\\' -> sb.append('\\')
          '/' -> sb.append('/')
          'n' -> sb.append('\n')
          't' -> sb.append('\t')
          'r' -> sb.append('\r')
          'b' -> sb.append('\b')
          'f' -> sb.append('\u000C')
          'u' -> {
            if (i + 5 < obj.length) {
              val code = obj.substring(i + 2, i + 6).toIntOrNull(16)
              if (code != null) sb.append(code.toChar())
              i += 4
            }
          }
          else -> sb.append(obj[i + 1])
        }
        i += 2
      }
      c == '"' -> return sb.toString()
      else -> {
        sb.append(c)
        i++
      }
    }
  }
  return sb.toString()
}

/** The integer value of "key" in a flat JSON object, or 0 when absent. */
internal fun jsonLongField(obj: String, key: String): Long {
  val valueStart = jsonValueStart(obj, key) ?: return 0L
  var i = valueStart
  val sb = StringBuilder()
  if (i < obj.length && obj[i] == '-') {
    sb.append('-')
    i++
  }
  while (i < obj.length && obj[i].isDigit()) {
    sb.append(obj[i])
    i++
  }
  return sb.toString().toLongOrNull() ?: 0L
}

/** Index just past the ':' of "key", or null when the key is absent. */
internal fun jsonValueStart(obj: String, key: String): Int? {
  val needle = "\"" + key + "\""
  var from = 0
  while (true) {
    val at = obj.indexOf(needle, from)
    if (at < 0) return null
    var i = at + needle.length
    while (i < obj.length && (obj[i] == ' ' || obj[i] == '\t')) i++
    if (i < obj.length && obj[i] == ':') {
      i++
      while (i < obj.length && (obj[i] == ' ' || obj[i] == '\t')) i++
      return i
    }
    from = at + needle.length
  }
}

/** A scalar field read as text: strings unescaped, numbers as written. */
internal fun jsonScalarField(obj: String, key: String): String {
  val valueStart = jsonValueStart(obj, key) ?: return ""
  if (valueStart >= obj.length) return ""
  if (obj[valueStart] == '"') return jsonStringField(obj, key)
  var i = valueStart
  while (i < obj.length && obj[i] != ',' && obj[i] != '}' && obj[i] != ']') i++
  val raw = obj.substring(valueStart, i).trim()
  return if (raw == "null") "" else raw
}
