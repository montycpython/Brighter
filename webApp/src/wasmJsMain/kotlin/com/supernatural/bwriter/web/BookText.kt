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
  val content: String
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

data class ParsedBook(
  val frontMatter: BookFrontMatter,
  val sections: List<BookSection>
)

/** Parses the whole payload: front matter plus sections in reading order. */
fun parseBook(payload: String): ParsedBook =
  ParsedBook(parseFrontMatter(payload), parseBookSections(payload))

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
fun parseBookSections(payload: String): List<BookSection> {
  val arrayStart = jsonValueStart(payload, "sections") ?: return emptyList()
  if (arrayStart >= payload.length || payload[arrayStart] != '[') return emptyList()
  val sections = ArrayList<BookSection>()
  var i = arrayStart + 1
  while (i < payload.length) {
    when (payload[i]) {
      '{' -> {
        val end = jsonObjectEnd(payload, i)
        if (end < 0) break
        val obj = payload.substring(i, end + 1)
        sections.add(
          BookSection(
            title = jsonStringField(obj, "title"),
            orderIndex = jsonLongField(obj, "orderIndex"),
            content = jsonStringField(obj, "content")
          )
        )
        i = end + 1
      }
      ']' -> break
      else -> i++
    }
  }
  return sections.sortedBy { it.orderIndex }
}
