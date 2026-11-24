package com.supernatural.bwriter.web

// Reading a book's full-text payload. The payload is the manuscript JSON the
// Android build writes (buildManuscriptPayload in FirestoreNetworkService):
// a top-level object with the book's front-matter fields and a "sections"
// array whose objects carry "title", "orderIndex" and "content" — the
// fields read here, by the same names. Sections are presented in
// orderIndex order, as in the book.

data class BookSection(
  val title: String,
  val orderIndex: Long,
  val content: String,
  val wordCount: Long
)

/** The book's title-page and copyright-page facts, as the author set them. */
data class BookFrontMatter(
  val subtitle: String,
  val authorName: String,
  val publisher: String,
  val edition: String,
  val year: String,
  val isbn: String,
  val copyrightText: String,
  val dedication: String,
  val epigraphText: String,
  val epigraphAuthor: String,
  val birthId: String
) {
  fun isEmpty(): Boolean =
    subtitle.isBlank() && authorName.isBlank() && publisher.isBlank() &&
      edition.isBlank() && year.isBlank() && isbn.isBlank() &&
      copyrightText.isBlank() && dedication.isBlank() &&
      epigraphText.isBlank() && epigraphAuthor.isBlank() && birthId.isBlank()
}

/** One of the book's characters, with the author's own notes. */
data class BookCharacter(
  val name: String,
  val role: String,
  val physicalDescription: String,
  val psychologicalDescription: String,
  val backstory: String,
  val voiceAndMannerisms: String,
  val intertextualArchetype: String,
  val otherDetails: String
)

/** One of the book's settings — a place in the world of the book. */
data class BookSetting(
  val locationName: String,
  val timePeriodOrEra: String,
  val atmosphereAndSensory: String,
  val architecturalOrSpatialDetails: String,
  val historicalOrCulturalContext: String,
  val targetIntertextualTouchstones: String,
  val otherNotes: String
)

data class ParsedBook(
  val frontMatter: BookFrontMatter,
  val sections: List<BookSection>,
  val characters: List<BookCharacter>,
  val settings: List<BookSetting>
)

/** Parses the whole payload: front matter, sections, characters, settings. */
fun parseBook(payload: String): ParsedBook =
  ParsedBook(
    parseFrontMatter(payload),
    parseBookSections(payload),
    parseCharacters(payload),
    parseSettings(payload)
  )

fun parseCharacters(payload: String): List<BookCharacter> =
  jsonObjectArray(payload, "characters").map { obj ->
    BookCharacter(
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

fun parseSettings(payload: String): List<BookSetting> =
  jsonObjectArray(payload, "settings").map { obj ->
    BookSetting(
      locationName = jsonStringField(obj, "locationName"),
      timePeriodOrEra = jsonStringField(obj, "timePeriodOrEra"),
      atmosphereAndSensory = jsonStringField(obj, "atmosphereAndSensory"),
      architecturalOrSpatialDetails = jsonStringField(obj, "architecturalOrSpatialDetails"),
      historicalOrCulturalContext = jsonStringField(obj, "historicalOrCulturalContext"),
      targetIntertextualTouchstones = jsonStringField(obj, "targetIntertextualTouchstones"),
      otherNotes = jsonStringField(obj, "otherNotes")
    )
  }

/** The object substrings of a top-level JSON array field, in order. */
internal fun jsonObjectArray(payload: String, key: String): List<String> {
  val arrayStart = jsonValueStart(payload, key) ?: return emptyList()
  if (arrayStart >= payload.length || payload[arrayStart] != '[') return emptyList()
  val out = ArrayList<String>()
  var i = arrayStart + 1
  while (i < payload.length) {
    when (payload[i]) {
      '{' -> {
        val end = jsonObjectEnd(payload, i)
        if (end < 0) break
        out.add(payload.substring(i, end + 1))
        i = end + 1
      }
      ']' -> break
      else -> i++
    }
  }
  return out
}

fun parseFrontMatter(payload: String): BookFrontMatter =
  BookFrontMatter(
    subtitle = jsonStringField(payload, "subtitle"),
    authorName = jsonStringField(payload, "authorName"),
    publisher = jsonStringField(payload, "publisher"),
    edition = jsonScalarField(payload, "edition"),
    year = jsonScalarField(payload, "year"),
    isbn = jsonStringField(payload, "isbn"),
    copyrightText = jsonStringField(payload, "copyrightText"),
    dedication = jsonStringField(payload, "dedication"),
    epigraphText = jsonStringField(payload, "epigraphText"),
    epigraphAuthor = jsonStringField(payload, "epigraphAuthor"),
    birthId = jsonStringField(payload, "birthId")
  )

/** The book's sections in reading order; empty when the payload has none. */
fun parseBookSections(payload: String): List<BookSection> =
  jsonObjectArray(payload, "sections").map { obj ->
    BookSection(
      title = jsonStringField(obj, "title"),
      orderIndex = jsonLongField(obj, "orderIndex"),
      content = jsonStringField(obj, "content"),
      wordCount = jsonLongField(obj, "wordCount")
    )
  }.sortedBy { it.orderIndex }
