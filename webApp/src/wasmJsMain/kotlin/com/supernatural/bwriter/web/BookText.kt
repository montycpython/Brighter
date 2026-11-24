package com.supernatural.bwriter.web

// Reading a book's full-text payload. The payload is the manuscript JSON the
// Android build writes (buildManuscriptPayload in FirestoreNetworkService):
// a top-level object whose "sections" array holds one object per section
// with "title", "orderIndex" and "content" — the fields read here, by the
// same names. Sections are presented in orderIndex order, as in the book.

data class BookSection(
  val title: String,
  val orderIndex: Long,
  val content: String
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
