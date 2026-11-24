package com.supernatural.bwriter.web

import com.supernatural.bwriter.crypto.Secp256k1
import com.supernatural.bwriter.data.UnitChain
import com.supernatural.bwriter.data.UnitGrant
import com.supernatural.bwriter.data.UnitRoles
import kotlin.js.JsString
import kotlin.js.js
import kotlin.js.toJsString
import com.supernatural.bwriter.currentTimeMillis
import com.supernatural.bwriter.randomUuidString

/**
 * Save & submit, mirroring the Android sync step for step: standing is
 * proven first, the stamp is computed over the payload built from the
 * book's current (previous) stamp fields, the chain advances, and the
 * writes land in the same order — cloud backup, the index entry in its
 * routed collection, the full text, a version snapshot, and the studio
 * notification. Routing follows Android's: a book already pending stays
 * pending, an approved book whose content changed returns to pending
 * for re-approval, the Editor in Chief's books go straight to the
 * registry, and new books go to pending.
 */
object BookSync {

  private const val STUDIO_EMAIL = "author.studio@bwriter.io"
  private const val COL_PENDING = "registryPending"
  private const val COL_REGISTRY = "registry"

  /** Serializes one Firestore data map with the payload escaper. */
  private fun dataJson(fields: List<Pair<String, Any>>): String {
    val sb = StringBuilder("{")
    fields.forEachIndexed { index, (key, value) ->
      if (index > 0) sb.append(',')
      sb.append(orgJsonQuote(key)).append(':')
      when (value) {
        is String -> sb.append(orgJsonQuote(value))
        is Boolean -> sb.append(if (value) "true" else "false")
        is Long -> sb.append(value.toString())
        is Int -> sb.append(value.toString())
        else -> sb.append(orgJsonQuote(value.toString()))
      }
    }
    return sb.append('}').toString()
  }

  private fun opJson(path: String, data: String): String =
    "{\"path\":${orgJsonQuote(path)},\"data\":$data}"

  private fun addOpJson(collection: String, data: String): String =
    "{\"add\":true,\"path\":${orgJsonQuote(collection)},\"data\":$data}"

  fun save(
    email: String,
    displayName: String,
    book: EditableBook,
    scalar: ByteArray,
    onDone: (success: Boolean, message: String) -> Unit
  ) {
    val normEmail = email.lowercase().trim()
    FirestoreBooks.fetchDocText("suspended/$normEmail") { suspensionText ->
      if (suspensionText == "ERR") {
        onDone(false, "Account standing could not be verified. Sync stopped.")
        return@fetchDocText
      }
      if (suspensionText.isNotBlank()) {
        val entryEmail = jsonStringField(suspensionText, "email")
        val locked = entryEmail.isNotBlank() &&
          !suspensionText.contains("\"isLockedOut\":false")
        if (locked) {
          val reason = jsonStringField(suspensionText, "reason")
          onDone(false, "Account suspended: $reason")
          return@fetchDocText
        }
      }
      FirestoreBooks.fetchDocText("studioConfig/main") { standingText ->
        val isEic = standingText.isNotBlank() && standingText != "ERR"
        stampAndRoute(email, normEmail, displayName, book, scalar, isEic, onDone)
      }
    }
  }

  private fun stampAndRoute(
    email: String,
    normEmail: String,
    displayName: String,
    book: EditableBook,
    scalar: ByteArray,
    isEic: Boolean,
    onDone: (Boolean, String) -> Unit
  ) {
    val address = Secp256k1.addressFromPrivateKey(scalar)
    val pubHex = Secp256k1.publicKeyHex(scalar)
    if (book.birthId.isBlank()) {
      book.birthId = randomUuidString()
    }
    book.authorAddress = address
    book.authorPubKey = pubHex
    val payloadJson = buildPayload(book, email, displayName)
    val prevStampHash = book.stampHash
    val newStampHash = sha256Hex("$address|${book.birthId}|$prevStampHash|$payloadJson")
    val signature = KeyVault.sign(scalar, newStampHash)
    if (signature.isBlank()) {
      onDone(false, "The key could not sign the stamp.")
      return
    }
    FirestoreBooks.fetchDocText("cloudBooks/${normEmail}_${book.birthId}") { cloudText ->
      if (cloudText == "ERR") {
        onDone(false, "The cloud copy could not be checked. Sync stopped.")
        return@fetchDocText
      }
      if (cloudText.isNotBlank()) {
        val cloudUpdatedAt = jsonLongField(cloudText, "updatedAt")
        if (cloudUpdatedAt > book.updatedAt) {
          onDone(
            false,
            "Cloud copy of '${book.title}' is newer than this browser copy. " +
              "Reopen the book before saving."
          )
          return@fetchDocText
        }
      }
      FirestoreBooks.findIndexEntry(COL_PENDING, book.id, email) { pendingText ->
        val pendingId = if (pendingText.isNotBlank() && pendingText != "ERR") {
          jsonStringField(pendingText, "__id")
        } else {
          ""
        }
        if (pendingId.isNotBlank()) {
          writeAll(
            email, normEmail, displayName, book, payloadJson,
            prevStampHash, newStampHash, signature, isEic,
            target = COL_PENDING, fileId = pendingId, onDone = onDone
          )
        } else {
          FirestoreBooks.findIndexEntry(COL_REGISTRY, book.id, email) { registryText ->
            val registryId = if (registryText.isNotBlank() && registryText != "ERR") {
              jsonStringField(registryText, "__id")
            } else {
              ""
            }
            if (registryId.isBlank()) {
              val target = if (isEic) COL_REGISTRY else COL_PENDING
              val fileId = "book_" + sha256Hex("$address|${book.birthId}").take(48)
              writeAll(
                email, normEmail, displayName, book, payloadJson,
                prevStampHash, newStampHash, signature, isEic,
                target = target, fileId = fileId, onDone = onDone
              )
            } else if (isEic) {
              writeAll(
                email, normEmail, displayName, book, payloadJson,
                prevStampHash, newStampHash, signature, isEic,
                target = COL_REGISTRY, fileId = registryId, onDone = onDone
              )
            } else {
              FirestoreBooks.fetchDocText("registry/$registryId/content/fullText") { contentText ->
                val storedPayload = if (contentText.isNotBlank() && contentText != "ERR") {
                  jsonStringField(contentText, "payload")
                } else {
                  ""
                }
                val differs = storedPayload.isBlank() ||
                  canonicalPayload(storedPayload) != canonicalPayload(payloadJson)
                writeAll(
                  email, normEmail, displayName, book, payloadJson,
                  prevStampHash, newStampHash, signature, isEic,
                  target = if (differs) COL_PENDING else COL_REGISTRY,
                  fileId = registryId, onDone = onDone
                )
              }
            }
          }
        }
      }
    }
  }

  private fun writeAll(
    email: String,
    normEmail: String,
    displayName: String,
    book: EditableBook,
    payloadJson: String,
    prevStampHash: String,
    newStampHash: String,
    signature: String,
    isEic: Boolean,
    target: String,
    fileId: String,
    onDone: (Boolean, String) -> Unit
  ) {
    val now = currentTimeMillis()
    val totalWords = book.totalWords()
    val totalLeaves = maxOf(1, totalWords / 250)
    val resolvedName = book.resolvedAuthorName(displayName)
    val ops = mutableListOf<String>()

    ops.add(
      opJson(
        "cloudBooks/${normEmail}_${book.birthId}",
        dataJson(
          listOf(
            "ownerEmail" to normEmail,
            "manuscriptId" to book.id,
            "birthId" to book.birthId,
            "payload" to payloadJson,
            "updatedAt" to now
          )
        )
      )
    )
    ops.add(
      opJson(
        "$target/$fileId",
        dataJson(
          listOf(
            "fileId" to fileId,
            "manuscriptId" to book.id,
            "title" to book.title,
            "subtitle" to book.subtitle,
            "authorName" to resolvedName,
            "authorEmail" to email,
            "authorEmailLower" to normEmail,
            "workType" to book.workType,
            "wordCount" to totalWords,
            "totalLeaves" to totalLeaves,
            "lastSyncedTimestamp" to now,
            "isPublicInCommunity" to book.isPublicInCommunity,
            "editorInChief" to isEic,
            "version" to book.edition.ifBlank { "1.0" },
            "authorAddress" to book.authorAddress,
            "authorPubKey" to book.authorPubKey,
            "birthId" to book.birthId,
            "stampHash" to newStampHash,
            "prevStampHash" to prevStampHash,
            "stampSignature" to signature,
            "hasFullText" to true
          )
        )
      )
    )
    ops.add(
      opJson(
        "$target/$fileId/content/fullText",
        dataJson(
          listOf(
            "payload" to payloadJson,
            "authorEmail" to email,
            "authorEmailLower" to normEmail,
            "isPublicInCommunity" to book.isPublicInCommunity,
            "authorAddress" to book.authorAddress,
            "authorPubKey" to book.authorPubKey,
            "birthId" to book.birthId,
            "stampHash" to newStampHash,
            "prevStampHash" to prevStampHash,
            "stampSignature" to signature,
            "updatedAt" to now
          )
        )
      )
    )
    ops.add(
      addOpJson(
        "versionSnapshots",
        dataJson(
          listOf(
            "snapshotId" to randomUuidString(),
            "manuscriptId" to book.id,
            "title" to book.title,
            "versionTag" to book.edition.ifBlank { "v1.0" },
            "changeSummary" to
              "Cloud backup & sync (${book.sections.size} divisions, $totalWords words)",
            "authorPenName" to resolvedName,
            "authorEmail" to email,
            "authorEmailLower" to normEmail,
            "wordCount" to totalWords,
            "totalLeaves" to totalLeaves,
            "timestamp" to now,
            "driveFileId" to fileId
          )
        )
      )
    )
    if (!isEic) {
      val mailId = randomUuidString()
      val visibility = if (book.isPublicInCommunity) "Public Global Registry" else "Private"
      val body = "Author $displayName ($email) synced '${book.title}' to the cloud registry." +
        "\n\nVisibility: $visibility" +
        "\nStatus: ${book.manuscriptStatus}" +
        "\nWords: $totalWords" +
        "\nFile ID: $fileId"
      ops.add(
        opJson(
          "mail/$mailId",
          dataJson(
            listOf(
              "id" to mailId,
              "recipientEmail" to STUDIO_EMAIL,
              "recipientEmailDisplay" to STUDIO_EMAIL,
              "senderEmail" to email,
              "senderName" to displayName,
              "subject" to "Manuscript Synced: ${book.title}",
              "body" to body,
              "timestamp" to now,
              "isRead" to false,
              "messageType" to "EDITORIAL_REVISION",
              "manuscriptId" to book.id,
              "manuscriptTitle" to book.title
            )
          )
        )
      )
    }

    val opsJson = ops.joinToString(",", "[", "]")
    FirestoreBooks.saveAll(opsJson) { saved ->
      if (!saved) {
        onDone(false, "The save did not land. Nothing was submitted.")
        return@saveAll
      }
      book.prevStampHash = prevStampHash
      book.stampHash = newStampHash
      book.stampSignature = signature
      val baseMessage = if (target == COL_PENDING) {
        "Saved and submitted for approval."
      } else {
        "Saved and published to the registry."
      }
      val afterUnits: (String) -> Unit = { unitsNote ->
        saveDriveBackup(book, email, displayName) { driveError ->
          val driveNote = if (driveError.isBlank()) {
            " Drive backup saved."
          } else {
            " Drive backup did not land: $driveError"
          }
          onDone(true, baseMessage + unitsNote + driveNote)
        }
      }
      if (isEic && target == COL_REGISTRY) {
        recordApprovalUnits(email, fileId, email) { result ->
          afterUnits(
            when (result) {
              "WRITTEN" -> " Units recorded."
              "SEASON_CAP_REACHED" -> " The season's author-unit cap is reached; units were not recorded."
              "FAILED" -> " Units could not be recorded."
              else -> ""
            }
          )
        }
      } else {
        afterUnits("")
      }
    }
  }
}

/**
 * The Drive leg of the save, run after the back-room writes land — the
 * same order the Android sync uses. The Drive payload is rebuilt from
 * the rotated book, so it carries the new stamp, as Android's does.
 */
fun driveWarmToken(): Unit =
  js("{ if (window.BwriterDrive) { window.BwriterDrive.warm(); } }")

private fun driveSaveBook(
  birthId: JsString,
  title: JsString,
  payload: JsString,
  callback: (JsString) -> Unit
): Unit =
  js("{ window.BwriterDrive.saveBook(birthId, title, payload).then((e) => callback(e)).catch(() => callback('Drive request failed')); }")

/** Saves the book's Drive backup; delivers "" on success or the failure. */
fun saveDriveBackup(book: EditableBook, email: String, displayName: String, onDone: (String) -> Unit) {
  val payload = buildPayload(book, email, displayName)
  driveSaveBook(book.birthId.toJsString(), book.title.toJsString(), payload.toJsString()) { error ->
    onDone(error.toString())
  }
}

/**
 * The chair's unit recording for his own books, mirroring
 * recordApprovalUnits: author, approved editors of record, and chair
 * grants, idempotent per book, chained over the latest grant, with the
 * season's author cap honored. Uses the shared UnitChain, so the hashes
 * and season are the Android ones by construction.
 */
fun recordApprovalUnits(
  chairEmail: String,
  fileId: String,
  authorEmail: String,
  onDone: (String) -> Unit
) {
  val chair = chairEmail.lowercase().trim()
  FirestoreBooks.fetchCollectionText("shareUnits") { unitsText ->
    if (unitsText == "ERR" || unitsText.isBlank()) {
      onDone("FAILED")
      return@fetchCollectionText
    }
    FirestoreBooks.fetchQueryText("editorAssignments", "fileId", fileId) { assignText ->
      val now = currentTimeMillis()
      val seasonYear = UnitChain.seasonYearFor(now)
      val allUnits = jsonObjectArray("{\"x\":$unitsText}", "x").map { obj ->
        UnitGrant(
          id = jsonStringField(obj, "id"),
          holderEmail = jsonStringField(obj, "holderEmail"),
          role = jsonStringField(obj, "role"),
          fileId = jsonStringField(obj, "fileId"),
          seasonYear = jsonLongField(obj, "seasonYear").toInt(),
          issuedAtMillis = jsonLongField(obj, "issuedAtMillis"),
          vested = jsonBoolField(obj, "vested"),
          prevGrantHash = jsonStringField(obj, "prevGrantHash"),
          grantHash = jsonStringField(obj, "grantHash")
        )
      }
      val grants = mutableListOf<Pair<String, String>>()
      grants.add(authorEmail.lowercase().trim() to UnitRoles.AUTHOR)
      if (assignText != "ERR" && assignText.isNotBlank()) {
        jsonObjectArray("{\"x\":$assignText}", "x")
          .filter { jsonStringField(it, "status") == "APPROVED" }
          .map { jsonStringField(it, "editorEmail").lowercase().trim() }
          .filter { it.isNotBlank() }
          .distinct()
          .forEach { grants.add(it to UnitRoles.EDITOR) }
      }
      grants.add(chair to UnitRoles.CHAIR)

      val recordedKeys = allUnits
        .filter { it.fileId == fileId }
        .map { it.holderEmail to it.role }
        .toSet()
      val missing = grants.filter { it !in recordedKeys }
      if (missing.isEmpty()) {
        onDone("ALREADY_RECORDED")
        return@fetchQueryText
      }
      if (missing.any { it.second == UnitRoles.AUTHOR }) {
        val seasonAuthorGrants = allUnits.count {
          it.role == UnitRoles.AUTHOR && it.seasonYear == seasonYear
        }
        if (seasonAuthorGrants >= UnitChain.SEASON_AUTHOR_CAP) {
          onDone("SEASON_CAP_REACHED")
          return@fetchQueryText
        }
      }
      var prevHash = allUnits
        .maxWithOrNull(compareBy<UnitGrant> { it.issuedAtMillis }.thenBy { it.grantHash })
        ?.grantHash ?: ""
      val ops = mutableListOf<String>()
      for ((holderEmail, role) in missing) {
        val hash = UnitChain.grantHashFor(prevHash, holderEmail, role, fileId, now)
        ops.add(
          "{\"path\":${orgJsonQuote("shareUnits/$hash")},\"data\":{" +
            "\"id\":${orgJsonQuote(hash)}," +
            "\"holderEmail\":${orgJsonQuote(holderEmail)}," +
            "\"role\":${orgJsonQuote(role)}," +
            "\"fileId\":${orgJsonQuote(fileId)}," +
            "\"seasonYear\":$seasonYear," +
            "\"issuedAtMillis\":$now," +
            "\"vested\":false," +
            "\"prevGrantHash\":${orgJsonQuote(prevHash)}," +
            "\"grantHash\":${orgJsonQuote(hash)}}}"
        )
        prevHash = hash
      }
      FirestoreBooks.saveAll(ops.joinToString(",", "[", "]")) { saved ->
        onDone(if (saved) "WRITTEN" else "FAILED")
      }
    }
  }
}
