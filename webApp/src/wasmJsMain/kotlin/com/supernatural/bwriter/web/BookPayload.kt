package com.supernatural.bwriter.web

import com.supernatural.bwriter.crypto.toHex
import com.supernatural.bwriter.sha256Digest

/**
 * The manuscript payload, mirrored field for field from the Android
 * build's buildManuscriptPayload — same keys, same order, same types,
 * and strings quoted exactly as Android's org.json quotes them (its
 * JSONStringer escapes every forward slash, a detail the stamp hash
 * depends on). A payload built here is byte-identical to the payload
 * Android builds from the same book, so a web stamp verifies there.
 */

fun orgJsonQuote(value: String): String {
  val sb = StringBuilder(value.length + 2)
  sb.append('"')
  for (c in value) {
    when (c) {
      '"', '\\', '/' -> sb.append('\\').append(c)
      '\t' -> sb.append("\\t")
      '\b' -> sb.append("\\b")
      '\n' -> sb.append("\\n")
      '\r' -> sb.append("\\r")
      '\u000C' -> sb.append("\\f")
      else -> if (c <= '\u001F') {
        sb.append("\\u")
        val hex = c.code.toString(16)
        for (i in hex.length until 4) sb.append('0')
        sb.append(hex)
      } else {
        sb.append(c)
      }
    }
  }
  sb.append('"')
  return sb.toString()
}

/** Counts words exactly as the Android sync does. */
fun countWords(content: String): Int =
  content.split(Regex("\\s+")).count { it.isNotBlank() }

/** SHA-256 of the UTF-8 text, lowercase hex — the stamp hash's digest. */
fun sha256Hex(text: String): String =
  sha256Digest(text.encodeToByteArray()).toHex()

data class EditableSection(
  var id: Long = 0,
  var matterType: String = "TEXT_BODY",
  var sectionType: String = "CHAPTER",
  var title: String = "",
  var subtitle: String = "",
  var orderIndex: Int = 0,
  var content: String = "",
  var aiDraftPrompt: String = "",
  var contributorNotes: String = "",
  var assignedAuthor: String = "",
  var assignedRole: String = "AUTHOR",
  var status: String = "DRAFT",
  var startOnRecto: Boolean = true,
  var headerIllustrationUri: String = "",
  var headerIllustrationCaption: String = "",
  var tailIllustrationUri: String = "",
  var tailIllustrationCaption: String = "",
  var wordCount: Int = 0,
  var hasPendingRevision: Boolean = false,
  var pendingEditedContent: String = "",
  var originalAuthorContent: String = "",
  var revisionAuthorPenName: String = "",
  var revisionAuthorEmail: String = "",
  var revisionAuthorRole: String = "",
  var revisionTimestamp: Long = 0,
  var revisionDeltaWords: Int = 0
)

data class EditableCharacter(
  var id: Long = 0,
  var name: String = "",
  var role: String = "Protagonist",
  var physicalDescription: String = "",
  var psychologicalDescription: String = "",
  var backstory: String = "",
  var voiceAndMannerisms: String = "",
  var intertextualArchetype: String = "",
  var otherDetails: String = ""
)

data class EditableSetting(
  var id: Long = 0,
  var locationName: String = "",
  var timePeriodOrEra: String = "",
  var atmosphereAndSensory: String = "",
  var architecturalOrSpatialDetails: String = "",
  var historicalOrCulturalContext: String = "",
  var targetIntertextualTouchstones: String = "",
  var otherNotes: String = ""
)

/** Editorial comments are carried through a save untouched. */
data class EditableComment(
  var id: Long = 0,
  var sectionId: Long = 0,
  var authorName: String = "",
  var authorRole: String = "AUTHOR",
  var commentText: String = "",
  var cmosRuleReference: String = "",
  var isResolved: Boolean = false,
  var timestamp: Long = 0
)

data class EditableBook(
  var id: Long = 0,
  var title: String = "",
  var subtitle: String = "",
  var authorName: String = "",
  var authorPenName: String = "",
  var editorName: String = "",
  var publisher: String = "Bwriter Editions",
  var edition: String = "First Edition",
  var year: String = "",
  var isbn: String = "",
  var copyrightText: String = "",
  var dedication: String = "",
  var epigraphText: String = "",
  var epigraphAuthor: String = "",
  var targetPageSize: String = "Trade 6\" x 9\"",
  var workType: String = "NOVEL",
  var manuscriptStatus: String = "DRAFT",
  var acknowledgmentsJson: String = "[]",
  var authorAddress: String = "",
  var authorPubKey: String = "",
  var birthId: String = "",
  var updatedAt: Long = 0,
  var stampHash: String = "",
  var prevStampHash: String = "",
  var stampSignature: String = "",
  var isPublicInCommunity: Boolean = false,
  var sections: List<EditableSection> = emptyList(),
  var characters: List<EditableCharacter> = emptyList(),
  var settings: List<EditableSetting> = emptyList(),
  var comments: List<EditableComment> = emptyList()
) {
  /** The byline rule, as Android resolves it: pen name, name, account name. */
  fun resolvedAuthorName(displayName: String): String = when {
    authorPenName.isNotBlank() -> authorPenName.trim()
    authorName.isNotBlank() -> authorName.trim()
    else -> displayName
  }

  fun totalWords(): Int = sections.sumOf {
    if (it.wordCount > 0) it.wordCount else countWords(it.content)
  }
}

/**
 * org.json's optString rule, mirrored: the default applies only when
 * the key is absent — a present-but-empty value stays empty.
 */
private fun fieldOr(obj: String, key: String, default: String): String =
  if (jsonValueStart(obj, key) != null) jsonStringField(obj, key) else default

private fun boolFieldOr(obj: String, key: String, default: Boolean): Boolean =
  if (jsonValueStart(obj, key) != null) jsonBoolField(obj, key) else default

/** Parses a stored payload into the editable mirror, without loss. */
fun parseEditableBook(payload: String): EditableBook? {
  if (!payload.trimStart().startsWith("{")) return null
  val book = EditableBook(
    id = jsonLongField(payload, "id"),
    title = jsonStringField(payload, "title"),
    subtitle = jsonStringField(payload, "subtitle"),
    authorName = jsonStringField(payload, "authorName"),
    authorPenName = jsonStringField(payload, "authorPenName"),
    editorName = jsonStringField(payload, "editorName"),
    publisher = fieldOr(payload, "publisher", "Bwriter Editions"),
    edition = fieldOr(payload, "edition", "First Edition"),
    year = fieldOr(payload, "year", "2026"),
    isbn = jsonStringField(payload, "isbn"),
    copyrightText = jsonStringField(payload, "copyrightText"),
    dedication = jsonStringField(payload, "dedication"),
    epigraphText = jsonStringField(payload, "epigraphText"),
    epigraphAuthor = jsonStringField(payload, "epigraphAuthor"),
    targetPageSize = fieldOr(payload, "targetPageSize", "Trade 6\" x 9\""),
    workType = fieldOr(payload, "workType", "NOVEL"),
    manuscriptStatus = fieldOr(payload, "manuscriptStatus", "DRAFT"),
    acknowledgmentsJson = fieldOr(payload, "acknowledgmentsJson", "[]"),
    authorAddress = jsonStringField(payload, "authorAddress"),
    authorPubKey = jsonStringField(payload, "authorPubKey"),
    birthId = jsonStringField(payload, "birthId"),
    updatedAt = jsonLongField(payload, "updatedAt"),
    stampHash = jsonStringField(payload, "stampHash"),
    prevStampHash = jsonStringField(payload, "prevStampHash"),
    stampSignature = jsonStringField(payload, "stampSignature"),
    isPublicInCommunity = jsonBoolField(payload, "isPublicInCommunity")
  )
  book.sections = jsonObjectArray(payload, "sections").mapIndexed { position, obj ->
    EditableSection(
      id = jsonLongField(obj, "id"),
      matterType = fieldOr(obj, "matterType", "TEXT_BODY"),
      sectionType = fieldOr(obj, "sectionType", "CHAPTER"),
      title = fieldOr(obj, "title", "Chapter"),
      subtitle = jsonStringField(obj, "subtitle"),
      orderIndex = if (jsonValueStart(obj, "orderIndex") != null) {
        jsonLongField(obj, "orderIndex").toInt()
      } else {
        position + 1
      },
      content = jsonStringField(obj, "content"),
      aiDraftPrompt = jsonStringField(obj, "aiDraftPrompt"),
      contributorNotes = jsonStringField(obj, "contributorNotes"),
      assignedAuthor = fieldOr(obj, "assignedAuthor", "Author"),
      assignedRole = fieldOr(obj, "assignedRole", "AUTHOR"),
      status = fieldOr(obj, "status", "DRAFT"),
      startOnRecto = boolFieldOr(obj, "startOnRecto", true),
      headerIllustrationUri = jsonStringField(obj, "headerIllustrationUri"),
      headerIllustrationCaption = jsonStringField(obj, "headerIllustrationCaption"),
      tailIllustrationUri = jsonStringField(obj, "tailIllustrationUri"),
      tailIllustrationCaption = jsonStringField(obj, "tailIllustrationCaption"),
      wordCount = jsonLongField(obj, "wordCount").toInt(),
      hasPendingRevision = jsonBoolField(obj, "hasPendingRevision"),
      pendingEditedContent = jsonStringField(obj, "pendingEditedContent"),
      originalAuthorContent = jsonStringField(obj, "originalAuthorContent"),
      revisionAuthorPenName = jsonStringField(obj, "revisionAuthorPenName"),
      revisionAuthorEmail = jsonStringField(obj, "revisionAuthorEmail"),
      revisionAuthorRole = jsonStringField(obj, "revisionAuthorRole"),
      revisionTimestamp = jsonLongField(obj, "revisionTimestamp"),
      revisionDeltaWords = jsonLongField(obj, "revisionDeltaWords").toInt()
    )
  }.sortedBy { it.orderIndex }
  book.characters = jsonObjectArray(payload, "characters").map { obj ->
    EditableCharacter(
      id = jsonLongField(obj, "id"),
      name = jsonStringField(obj, "name"),
      role = jsonStringField(obj, "role"),
      physicalDescription = jsonStringField(obj, "physicalDescription"),
      psychologicalDescription = jsonStringField(obj, "psychologicalDescription"),
      backstory = jsonStringField(obj, "backstory"),
      voiceAndMannerisms = jsonStringField(obj, "voiceAndMannerisms"),
      intertextualArchetype = jsonStringField(obj, "intertextualArchetype"),
      otherDetails = jsonStringField(obj, "otherDetails")
    )
  }
  book.settings = jsonObjectArray(payload, "settings").map { obj ->
    EditableSetting(
      id = jsonLongField(obj, "id"),
      locationName = jsonStringField(obj, "locationName"),
      timePeriodOrEra = jsonStringField(obj, "timePeriodOrEra"),
      atmosphereAndSensory = jsonStringField(obj, "atmosphereAndSensory"),
      architecturalOrSpatialDetails = jsonStringField(obj, "architecturalOrSpatialDetails"),
      historicalOrCulturalContext = jsonStringField(obj, "historicalOrCulturalContext"),
      targetIntertextualTouchstones = jsonStringField(obj, "targetIntertextualTouchstones"),
      otherNotes = jsonStringField(obj, "otherNotes")
    )
  }
  book.comments = jsonObjectArray(payload, "comments").map { obj ->
    EditableComment(
      id = jsonLongField(obj, "id"),
      sectionId = jsonLongField(obj, "sectionId"),
      authorName = jsonStringField(obj, "authorName"),
      authorRole = fieldOr(obj, "authorRole", "AUTHOR"),
      commentText = jsonStringField(obj, "commentText"),
      cmosRuleReference = jsonStringField(obj, "cmosRuleReference"),
      isResolved = jsonBoolField(obj, "isResolved"),
      timestamp = jsonLongField(obj, "timestamp")
    )
  }
  return book
}

private class PayloadWriter {
  val sb = StringBuilder()
  private var first = true

  fun openObject() { sb.append('{'); first = true }
  fun closeObject() { sb.append('}') }
  private fun key(name: String) {
    if (!first) sb.append(',')
    first = false
    sb.append(orgJsonQuote(name)).append(':')
  }
  fun str(name: String, value: String) { key(name); sb.append(orgJsonQuote(value)) }
  fun num(name: String, value: Long) { key(name); sb.append(value.toString()) }
  fun num(name: String, value: Int) { key(name); sb.append(value.toString()) }
  fun bool(name: String, value: Boolean) { key(name); sb.append(if (value) "true" else "false") }
  fun raw(name: String, json: String) { key(name); sb.append(json) }
}

/** Builds the payload string exactly as Android's buildManuscriptPayload. */
fun buildPayload(book: EditableBook, authorEmail: String, displayName: String): String {
  val w = PayloadWriter()
  w.openObject()
  w.num("id", book.id)
  w.str("title", book.title)
  w.str("subtitle", book.subtitle)
  w.str("authorName", book.resolvedAuthorName(displayName))
  w.str("authorPenName", book.authorPenName)
  w.str("authorEmail", authorEmail)
  w.str("editorName", book.editorName)
  w.str("publisher", book.publisher)
  w.str("edition", book.edition)
  w.str("year", book.year)
  w.str("isbn", book.isbn)
  w.str("copyrightText", book.copyrightText)
  w.str("dedication", book.dedication)
  w.str("epigraphText", book.epigraphText)
  w.str("epigraphAuthor", book.epigraphAuthor)
  w.str("targetPageSize", book.targetPageSize)
  w.str("workType", book.workType)
  w.str("manuscriptStatus", book.manuscriptStatus)
  w.str("acknowledgmentsJson", book.acknowledgmentsJson)
  w.str("authorAddress", book.authorAddress)
  w.str("authorPubKey", book.authorPubKey)
  w.str("birthId", book.birthId)
  w.num("updatedAt", book.updatedAt)
  w.str("stampHash", book.stampHash)
  w.str("prevStampHash", book.prevStampHash)
  w.str("stampSignature", book.stampSignature)
  w.bool("isPublicInCommunity", book.isPublicInCommunity)

  val sectionsJson = book.sections.joinToString(",", "[", "]") { sec ->
    val s = PayloadWriter()
    s.openObject()
    s.num("id", sec.id)
    s.num("manuscriptId", book.id)
    s.str("matterType", sec.matterType)
    s.str("sectionType", sec.sectionType)
    s.str("title", sec.title)
    s.str("subtitle", sec.subtitle)
    s.num("orderIndex", sec.orderIndex)
    s.str("content", sec.content)
    s.str("aiDraftPrompt", sec.aiDraftPrompt)
    s.str("contributorNotes", sec.contributorNotes)
    s.str("assignedAuthor", sec.assignedAuthor)
    s.str("assignedRole", sec.assignedRole)
    s.str("status", sec.status)
    s.bool("startOnRecto", sec.startOnRecto)
    s.str("headerIllustrationUri", sec.headerIllustrationUri)
    s.str("headerIllustrationCaption", sec.headerIllustrationCaption)
    s.str("tailIllustrationUri", sec.tailIllustrationUri)
    s.str("tailIllustrationCaption", sec.tailIllustrationCaption)
    s.num("wordCount", sec.wordCount)
    s.bool("hasPendingRevision", sec.hasPendingRevision)
    s.str("pendingEditedContent", sec.pendingEditedContent)
    s.str("originalAuthorContent", sec.originalAuthorContent)
    s.str("revisionAuthorPenName", sec.revisionAuthorPenName)
    s.str("revisionAuthorEmail", sec.revisionAuthorEmail)
    s.str("revisionAuthorRole", sec.revisionAuthorRole)
    s.num("revisionTimestamp", sec.revisionTimestamp)
    s.num("revisionDeltaWords", sec.revisionDeltaWords)
    s.closeObject()
    s.sb.toString()
  }
  w.raw("sections", sectionsJson)

  val charactersJson = book.characters.joinToString(",", "[", "]") { ch ->
    val s = PayloadWriter()
    s.openObject()
    s.num("id", ch.id)
    s.str("name", ch.name)
    s.str("role", ch.role)
    s.str("physicalDescription", ch.physicalDescription)
    s.str("psychologicalDescription", ch.psychologicalDescription)
    s.str("backstory", ch.backstory)
    s.str("voiceAndMannerisms", ch.voiceAndMannerisms)
    s.str("intertextualArchetype", ch.intertextualArchetype)
    s.str("otherDetails", ch.otherDetails)
    s.closeObject()
    s.sb.toString()
  }
  w.raw("characters", charactersJson)

  val settingsJson = book.settings.joinToString(",", "[", "]") { st ->
    val s = PayloadWriter()
    s.openObject()
    s.num("id", st.id)
    s.str("locationName", st.locationName)
    s.str("timePeriodOrEra", st.timePeriodOrEra)
    s.str("atmosphereAndSensory", st.atmosphereAndSensory)
    s.str("architecturalOrSpatialDetails", st.architecturalOrSpatialDetails)
    s.str("historicalOrCulturalContext", st.historicalOrCulturalContext)
    s.str("targetIntertextualTouchstones", st.targetIntertextualTouchstones)
    s.str("otherNotes", st.otherNotes)
    s.closeObject()
    s.sb.toString()
  }
  w.raw("settings", settingsJson)

  val commentsJson = book.comments.joinToString(",", "[", "]") { cm ->
    val s = PayloadWriter()
    s.openObject()
    s.num("id", cm.id)
    s.num("sectionId", cm.sectionId)
    s.str("authorName", cm.authorName)
    s.str("authorRole", cm.authorRole)
    s.str("commentText", cm.commentText)
    s.str("cmosRuleReference", cm.cmosRuleReference)
    s.bool("isResolved", cm.isResolved)
    s.num("timestamp", cm.timestamp)
    s.closeObject()
    s.sb.toString()
  }
  w.raw("comments", commentsJson)

  w.closeObject()
  return w.sb.toString()
}

// The re-approval change test, mirroring Android's canonicalPayload:
// keys sorted, volatile keys dropped at every level, org.json quoting.

private val VOLATILE_KEYS = setOf(
  "updatedAt", "timestamp", "stampHash", "prevStampHash", "stampSignature", "revisionTimestamp"
)

private sealed class JsonVal {
  data class Obj(val entries: List<Pair<String, JsonVal>>) : JsonVal()
  data class Arr(val items: List<JsonVal>) : JsonVal()
  data class Str(val value: String) : JsonVal()
  data class Num(val raw: String) : JsonVal()
  data class Bool(val value: Boolean) : JsonVal()
  object Null : JsonVal()
}

private class JsonParser(private val text: String) {
  private var i = 0

  fun parse(): JsonVal? {
    skipWs()
    val value = readValue() ?: return null
    skipWs()
    return if (i == text.length) value else null
  }

  private fun skipWs() {
    while (i < text.length && text[i] <= ' ') i++
  }

  private fun readValue(): JsonVal? {
    if (i >= text.length) return null
    return when (text[i]) {
      '{' -> readObject()
      '[' -> readArray()
      '"' -> readString()?.let { JsonVal.Str(it) }
      't' -> if (text.startsWith("true", i)) { i += 4; JsonVal.Bool(true) } else null
      'f' -> if (text.startsWith("false", i)) { i += 5; JsonVal.Bool(false) } else null
      'n' -> if (text.startsWith("null", i)) { i += 4; JsonVal.Null } else null
      else -> readNumber()
    }
  }

  private fun readObject(): JsonVal? {
    i++
    skipWs()
    val entries = mutableListOf<Pair<String, JsonVal>>()
    if (i < text.length && text[i] == '}') { i++; return JsonVal.Obj(entries) }
    while (true) {
      skipWs()
      val key = readString() ?: return null
      skipWs()
      if (i >= text.length || text[i] != ':') return null
      i++
      skipWs()
      val value = readValue() ?: return null
      entries.add(key to value)
      skipWs()
      if (i >= text.length) return null
      when (text[i]) {
        ',' -> i++
        '}' -> { i++; return JsonVal.Obj(entries) }
        else -> return null
      }
    }
  }

  private fun readArray(): JsonVal? {
    i++
    skipWs()
    val items = mutableListOf<JsonVal>()
    if (i < text.length && text[i] == ']') { i++; return JsonVal.Arr(items) }
    while (true) {
      skipWs()
      val value = readValue() ?: return null
      items.add(value)
      skipWs()
      if (i >= text.length) return null
      when (text[i]) {
        ',' -> i++
        ']' -> { i++; return JsonVal.Arr(items) }
        else -> return null
      }
    }
  }

  private fun readString(): String? {
    if (i >= text.length || text[i] != '"') return null
    i++
    val sb = StringBuilder()
    while (i < text.length) {
      val c = text[i]
      when {
        c == '"' -> { i++; return sb.toString() }
        c == '\\' -> {
          if (i + 1 >= text.length) return null
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
              if (i + 5 >= text.length) return null
              val code = text.substring(i + 2, i + 6).toIntOrNull(16) ?: return null
              sb.append(code.toChar())
              i += 4
            }
            else -> return null
          }
          i += 2
        }
        else -> { sb.append(c); i++ }
      }
    }
    return null
  }

  private fun readNumber(): JsonVal? {
    val start = i
    while (i < text.length && text[i] in "-+0123456789.eE") i++
    if (i == start) return null
    return JsonVal.Num(text.substring(start, i))
  }
}

private fun canonOf(value: JsonVal): String = when (value) {
  is JsonVal.Obj -> value.entries
    .filter { it.first !in VOLATILE_KEYS }
    .sortedBy { it.first }
    .joinToString(",", "{", "}") { orgJsonQuote(it.first) + ":" + canonOf(it.second) }
  is JsonVal.Arr -> value.items.joinToString(",", "[", "]") { canonOf(it) }
  is JsonVal.Str -> orgJsonQuote(value.value)
  is JsonVal.Num -> value.raw.toLongOrNull()?.toString() ?: value.raw
  is JsonVal.Bool -> if (value.value) "true" else "false"
  JsonVal.Null -> "null"
}

/** Canonical form for the change test; unparseable text compares as itself. */
fun canonicalPayload(json: String): String {
  val parsed = try {
    JsonParser(json).parse()
  } catch (e: Exception) {
    null
  }
  return parsed?.let { canonOf(it) } ?: json
}
