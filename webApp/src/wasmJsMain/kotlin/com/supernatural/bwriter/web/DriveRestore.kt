package com.supernatural.bwriter.web

import kotlin.js.JsString
import kotlin.js.js
import kotlin.js.toJsString

// The Drive restore bridges: the book payloads and the key backup that
// live in the author's own "Bwriter" Drive folder, mirroring the
// Android client's download paths.

private fun driveDownloadAll(callback: (JsString) -> Unit): Unit =
  js("{ window.BwriterDriveRestore.downloadAllPayloads().then((arr) => callback(JSON.stringify(arr))).catch(() => callback('[]')); }")

private fun driveDownloadKey(callback: (JsString) -> Unit): Unit =
  js("{ window.BwriterDriveRestore.downloadKeyBackup().then((t) => callback(t || '')).catch(() => callback('')); }")

private fun driveUploadKey(json: JsString, callback: (JsString) -> Unit): Unit =
  js("{ window.BwriterDriveRestore.uploadKeyBackup(json).then((e) => callback(e)).catch(() => callback('Drive request failed')); }")

/** Every book payload stored in the Drive folder, as raw JSON texts. */
fun downloadDrivePayloads(onDone: (List<String>) -> Unit) {
  driveDownloadAll { text ->
    onDone(parseJsonStringArray(text.toString()))
  }
}

/** The sealed key backup from Drive, or "" when there is none. */
fun downloadDriveKeyBackup(onDone: (String) -> Unit) {
  driveDownloadKey { text -> onDone(text.toString()) }
}

/** Saves the sealed key backup to Drive; delivers "" or the failure. */
fun uploadDriveKeyBackup(json: String, onDone: (String) -> Unit) {
  driveUploadKey(json.toJsString()) { error -> onDone(error.toString()) }
}

/** Parses a JSON array whose elements are all strings. */
fun parseJsonStringArray(text: String): List<String> {
  val result = mutableListOf<String>()
  var i = 0
  fun skipWs() {
    while (i < text.length && text[i] <= ' ') i++
  }
  skipWs()
  if (i >= text.length || text[i] != '[') return result
  i++
  skipWs()
  if (i < text.length && text[i] == ']') return result
  while (i < text.length) {
    skipWs()
    if (i >= text.length || text[i] != '"') break
    i++
    val sb = StringBuilder()
    var closed = false
    while (i < text.length) {
      val c = text[i]
      when {
        c == '"' -> {
          i++
          closed = true
          break
        }
        c == '\\' && i + 1 < text.length -> {
          when (text[i + 1]) {
            '"' -> sb.append('"')
            '\\' -> sb.append('\\')
            '/' -> sb.append('/')
            'b' -> sb.append('\b')
            'f' -> sb.append('\u000C')
            'n' -> sb.append('\n')
            'r' -> sb.append('\r')
            't' -> sb.append('\t')
            'u' -> {
              if (i + 5 < text.length) {
                val code = text.substring(i + 2, i + 6).toIntOrNull(16)
                if (code != null) sb.append(code.toChar())
                i += 4
              }
            }
            else -> sb.append(text[i + 1])
          }
          i += 2
        }
        else -> {
          sb.append(c)
          i++
        }
      }
    }
    if (!closed) break
    result.add(sb.toString())
    skipWs()
    if (i < text.length && text[i] == ',') {
      i++
    } else {
      break
    }
  }
  return result
}

/**
 * Restores Drive books into the cloud library wherever the cloud copy
 * is missing or older than the Drive copy, and reports how many books
 * were restored. The cloud remains the working library; Drive is the
 * author's own backup of it.
 */
fun restoreFromDrive(email: String, onDone: (Int) -> Unit) {
  val normEmail = email.lowercase().trim()
  downloadDrivePayloads { payloads ->
    var index = 0
    var restored = 0
    fun next() {
      if (index >= payloads.size) {
        onDone(restored)
        return
      }
      val payload = payloads[index]
      index++
      val birthId = jsonStringField(payload, "birthId")
      if (birthId.isBlank()) {
        next()
        return
      }
      FirestoreBooks.fetchDocText("cloudBooks/${normEmail}_$birthId") { cloudText ->
        val driveUpdated = jsonLongField(payload, "updatedAt")
        val shouldWrite = when {
          cloudText == "ERR" -> false
          cloudText.isBlank() -> true
          else -> {
            val cloudPayload = jsonStringField(cloudText, "payload")
            driveUpdated > jsonLongField(cloudPayload, "updatedAt")
          }
        }
        if (!shouldWrite) {
          next()
        } else {
          val manuscriptId = jsonLongField(payload, "id")
          val op = "{\"path\":${orgJsonQuote("cloudBooks/${normEmail}_$birthId")},\"data\":{" +
            "\"ownerEmail\":${orgJsonQuote(normEmail)}," +
            "\"manuscriptId\":$manuscriptId," +
            "\"birthId\":${orgJsonQuote(birthId)}," +
            "\"payload\":${orgJsonQuote(payload)}," +
            "\"updatedAt\":$driveUpdated}}"
          FirestoreBooks.saveAll("[$op]") { saved ->
            if (saved) restored++
            next()
          }
        }
      }
    }
    next()
  }
}
