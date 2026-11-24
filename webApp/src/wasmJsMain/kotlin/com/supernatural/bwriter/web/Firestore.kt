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
