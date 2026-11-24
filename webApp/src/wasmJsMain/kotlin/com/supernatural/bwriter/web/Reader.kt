package com.supernatural.bwriter.web

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.WebElementView
import com.supernatural.bwriter.cmos.BookMarkup
import com.supernatural.bwriter.data.bisnFor
import com.supernatural.bwriter.cmos.MarkupSegment
import kotlinx.browser.document
import org.w3c.dom.HTMLElement

// The web reader: the same book the Android reader shows, from the same
// registry content document, cut into segments by the same shared parser
// (BookMarkup in :shared). Text, emphasis, code and footnotes are painted by
// Compose; display math is painted by KaTeX through MathRender, the web
// counterpart of Android's JLatexMath rendering. Inline math is shown in
// italic serif for now — Android paints it as a rendered formula, and the
// web build will match when inline HTML interop inside text is safe to use.

@Composable
fun ReaderScreen(book: RegistryBook, onBack: () -> Unit) {
  var parsed by remember(book.id) { mutableStateOf<ParsedBook?>(null) }
  LaunchedEffect(book.id) {
    FirestoreBooks.fetchBookPayload(book.id) { payload ->
      parsed = parseBook(payload)
    }
  }
  ReaderContent(book.title, book.authorName, book.birthId, parsed, onBack)
}

/** A book from the author's own cloud library: its text rides in the payload. */
@Composable
fun MyBookReaderScreen(book: MyBook, onBack: () -> Unit) {
  val parsed = remember(book.id) { parseBook(book.payload) }
  ReaderContent(book.title, book.authorName, book.birthId, parsed, onBack)
}

@Composable
private fun ReaderContent(
  title: String,
  authorName: String,
  birthId: String,
  parsed: ParsedBook?,
  onBack: () -> Unit
) {
  var showCast by remember { mutableStateOf(false) }
  val ready = parsed
  if (showCast && ready != null) {
    CastWorldScreen(
      title,
      ready.characters,
      ready.settings,
      onBack = { showCast = false }
    )
    return
  }
  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(AppleBackground)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      TextButton(onClick = onBack) {
        Text(text = "‹  Library", color = BwriterGold, fontSize = 16.sp)
      }
      Spacer(modifier = Modifier.weight(1f))
      if (parsed != null &&
        (parsed.characters.isNotEmpty() || parsed.settings.isNotEmpty())
      ) {
        TextButton(onClick = { showCast = true }) {
          Text(text = "Cast & World", color = BwriterGold, fontSize = 16.sp)
        }
      }
    }
    val current = parsed
    when {
      current == null -> {
        Box(
          modifier = Modifier.fillMaxSize(),
          contentAlignment = Alignment.Center
        ) {
          CircularProgressIndicator(color = BwriterGold)
        }
      }
      current.sections.isEmpty() && current.frontMatter.isEmpty() &&
        current.characters.isEmpty() && current.settings.isEmpty() -> {
        Box(
          modifier = Modifier.fillMaxSize(),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "This book's text could not be loaded.",
            fontSize = 16.sp,
            color = AppleSecondary
          )
        }
      }
      else -> {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
          item {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp, vertical = 12.dp)
            ) {
              Text(
                text = title.ifBlank { "Untitled" },
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = AppleInk
              )
              if (authorName.isNotBlank()) {
                Text(
                  text = "by ${authorName}",
                  fontSize = 15.sp,
                  color = AppleSecondary,
                  modifier = Modifier.padding(top = 4.dp)
                )
              }
              val imprint = listOf(
                current.frontMatter.publisher,
                current.frontMatter.edition,
                current.frontMatter.year
              ).filter { it.isNotBlank() }.joinToString(" · ")
              if (imprint.isNotBlank()) {
                Text(
                  text = imprint,
                  fontSize = 13.sp,
                  color = AppleSecondary,
                  modifier = Modifier.padding(top = 2.dp)
                )
              }
            }
          }
          if (!current.frontMatter.isEmpty()) {
            item { FrontMatterBlock(current.frontMatter, birthId) }
          }
          for (section in current.sections) {
            if (section.title.isNotBlank()) {
              item {
                Text(
                  text = section.title,
                  fontSize = 22.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = AppleInk,
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp, vertical = 10.dp)
                )
              }
            }
            items(buildBlocks(section.content)) { block -> BlockView(block) }
          }
          item { Spacer(modifier = Modifier.height(40.dp)) }
        }
      }
    }
  }
}

private sealed interface ReaderBlock {
  data class Paragraph(val text: AnnotatedString) : ReaderBlock
  data class Code(val content: String) : ReaderBlock
  data class Math(val tex: String) : ReaderBlock
}

/** Cuts one section's text into paragraphs, code blocks and math blocks. */
private fun buildBlocks(content: String): List<ReaderBlock> {
  val blocks = ArrayList<ReaderBlock>()
  var builder = AnnotatedString.Builder()
  var hasText = false

  fun flush() {
    if (hasText) {
      blocks.add(ReaderBlock.Paragraph(builder.toAnnotatedString()))
      builder = AnnotatedString.Builder()
      hasText = false
    }
  }

  fun appendStyled(seg: MarkupSegment, style: SpanStyle) {
    builder.pushStyle(style)
    builder.append(seg.content)
    builder.pop()
    hasText = true
  }

  for (seg in BookMarkup.parse(content)) {
    when (seg) {
      is MarkupSegment.Text -> {
        val parts = seg.content.split('\n')
        for ((index, part) in parts.withIndex()) {
          if (index > 0) flush()
          if (part.isNotEmpty()) {
            builder.append(part)
            hasText = true
          }
        }
      }
      is MarkupSegment.Bold ->
        appendStyled(seg, SpanStyle(fontWeight = FontWeight.Bold))
      is MarkupSegment.Italic ->
        appendStyled(seg, SpanStyle(fontStyle = FontStyle.Italic))
      is MarkupSegment.BoldItalic ->
        appendStyled(
          seg,
          SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)
        )
      is MarkupSegment.InlineCode ->
        appendStyled(
          seg,
          SpanStyle(
            fontFamily = FontFamily.Monospace,
            background = Color(0x14000000)
          )
        )
      is MarkupSegment.InlineMath ->
        appendStyled(seg, SpanStyle(fontStyle = FontStyle.Italic))
      is MarkupSegment.Footnote ->
        appendStyled(
          seg,
          SpanStyle(
            baselineShift = BaselineShift.Superscript,
            fontSize = 11.sp,
            color = BwriterGold
          )
        )
      is MarkupSegment.CodeBlock -> {
        flush()
        blocks.add(ReaderBlock.Code(seg.content))
      }
      is MarkupSegment.DisplayMath -> {
        flush()
        blocks.add(ReaderBlock.Math(seg.content))
      }
    }
  }
  flush()
  return blocks
}

@Composable
private fun BlockView(block: ReaderBlock) {
  when (block) {
    is ReaderBlock.Paragraph -> {
      Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter
      ) {
        Text(
          text = block.text,
          fontFamily = FontFamily.Serif,
          fontSize = 17.sp,
          lineHeight = 26.sp,
          color = AppleInk,
          modifier = Modifier
            .widthIn(max = 680.dp)
            .fillMaxWidth()
            .padding(horizontal = 28.dp, vertical = 6.dp)
        )
      }
    }
    is ReaderBlock.Code -> {
      Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter
      ) {
        Surface(
          color = Color(0xFFF0F0F2),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier
            .widthIn(max = 680.dp)
            .fillMaxWidth()
            .padding(horizontal = 28.dp, vertical = 6.dp)
        ) {
          Text(
            text = block.content,
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp,
            color = AppleInk,
            modifier = Modifier.padding(14.dp)
          )
        }
      }
    }
    is ReaderBlock.Math -> KatexBlock(block.tex)
  }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun KatexBlock(tex: String) {
  Box(
    modifier = Modifier.fillMaxWidth(),
    contentAlignment = Alignment.TopCenter
  ) {
    WebElementView(
      factory = { document.createElement("div") as HTMLElement },
      modifier = Modifier
        .widthIn(max = 680.dp)
        .fillMaxWidth()
        .height(64.dp)
        .padding(horizontal = 28.dp),
      update = { element ->
        element.innerHTML = MathRender.render(tex, displayMode = true)
      }
    )
  }
}

@Composable
private fun FrontMatterBlock(front: BookFrontMatter, fallbackBirthId: String) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 28.dp, vertical = 8.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    if (front.dedication.isNotBlank()) {
      Text(
        text = front.dedication,
        fontFamily = FontFamily.Serif,
        fontStyle = FontStyle.Italic,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        color = AppleInk,
        textAlign = TextAlign.Center,
        modifier = Modifier
          .widthIn(max = 520.dp)
          .padding(vertical = 16.dp)
      )
    }
    if (front.epigraphText.isNotBlank()) {
      Text(
        text = front.epigraphText,
        fontFamily = FontFamily.Serif,
        fontStyle = FontStyle.Italic,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        color = AppleInk,
        textAlign = TextAlign.Center,
        modifier = Modifier
          .widthIn(max = 520.dp)
          .padding(top = 16.dp)
      )
      if (front.epigraphAuthor.isNotBlank()) {
        Text(
          text = "— ${front.epigraphAuthor}",
          fontSize = 13.sp,
          color = AppleSecondary,
          textAlign = TextAlign.Center,
          modifier = Modifier.padding(top = 4.dp)
        )
      }
    }
    Box(
      modifier = Modifier
        .padding(vertical = 20.dp)
        .width(40.dp)
        .height(1.dp)
        .background(BwriterGold)
    )
    Column(
      modifier = Modifier
        .widthIn(max = 680.dp)
        .fillMaxWidth()
    ) {
      if (front.copyrightText.isNotBlank()) {
        Text(
          text = front.copyrightText,
          fontSize = 12.sp,
          lineHeight = 18.sp,
          color = AppleSecondary
        )
      }
      if (front.isbn.isNotBlank()) {
        Text(
          text = "ISBN ${front.isbn}",
          fontSize = 12.sp,
          color = AppleSecondary,
          modifier = Modifier.padding(top = 4.dp)
        )
      }
      val birthId = front.birthId.ifBlank { fallbackBirthId }
      if (birthId.isNotBlank()) {
        Text(
          text = "BISN ${bisnFor(birthId)}",
          fontSize = 12.sp,
          color = BwriterGold,
          modifier = Modifier.padding(top = 4.dp)
        )
      }
    }
  }
}

@Composable
private fun CastWorldScreen(
  bookTitle: String,
  characters: List<BookCharacter>,
  settings: List<BookSetting>,
  onBack: () -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(AppleBackground)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      TextButton(onClick = onBack) {
        Text(text = "‹  Book", color = BwriterGold, fontSize = 16.sp)
      }
    }
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 28.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      item {
        Column {
          Text(
            text = "Cast & World",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = AppleInk
          )
          if (bookTitle.isNotBlank()) {
            Text(
              text = bookTitle,
              fontSize = 15.sp,
              color = AppleSecondary,
              modifier = Modifier.padding(top = 2.dp)
            )
          }
        }
      }
      if (characters.isNotEmpty()) {
        item { WorldSectionHeader("Characters") }
        items(characters) { character -> CharacterCard(character) }
      }
      if (settings.isNotEmpty()) {
        item { WorldSectionHeader("Settings") }
        items(settings) { setting -> SettingCard(setting) }
      }
      item { Spacer(modifier = Modifier.height(24.dp)) }
    }
  }
}

@Composable
private fun WorldSectionHeader(text: String) {
  Text(
    text = text,
    fontSize = 20.sp,
    fontWeight = FontWeight.SemiBold,
    color = AppleInk,
    modifier = Modifier.padding(top = 10.dp)
  )
}

@Composable
private fun LabeledField(label: String, value: String) {
  if (value.isNotBlank()) {
    Column(modifier = Modifier.padding(top = 10.dp)) {
      Text(
        text = label.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = AppleSecondary
      )
      Text(
        text = value,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = AppleInk,
        modifier = Modifier.padding(top = 2.dp)
      )
    }
  }
}

@Composable
private fun CharacterCard(character: BookCharacter) {
  Surface(
    color = AppleSurface,
    shape = RoundedCornerShape(16.dp),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(20.dp)) {
      Text(
        text = character.name.ifBlank { "Unnamed" },
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold,
        color = AppleInk
      )
      if (character.role.isNotBlank()) {
        Text(
          text = character.role,
          fontSize = 13.sp,
          color = BwriterGold,
          modifier = Modifier.padding(top = 2.dp)
        )
      }
      LabeledField("Physical", character.physicalDescription)
      LabeledField("Mind", character.psychologicalDescription)
      LabeledField("Backstory", character.backstory)
      LabeledField("Voice & Mannerisms", character.voiceAndMannerisms)
      LabeledField("Archetype", character.intertextualArchetype)
      LabeledField("Other Details", character.otherDetails)
    }
  }
}

@Composable
private fun SettingCard(setting: BookSetting) {
  Surface(
    color = AppleSurface,
    shape = RoundedCornerShape(16.dp),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(20.dp)) {
      Text(
        text = setting.locationName.ifBlank { "Unnamed place" },
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold,
        color = AppleInk
      )
      if (setting.timePeriodOrEra.isNotBlank()) {
        Text(
          text = setting.timePeriodOrEra,
          fontSize = 13.sp,
          color = AppleSecondary,
          modifier = Modifier.padding(top = 2.dp)
        )
      }
      LabeledField("Atmosphere & Senses", setting.atmosphereAndSensory)
      LabeledField("Space & Architecture", setting.architecturalOrSpatialDetails)
      LabeledField("Historical & Cultural Context", setting.historicalOrCulturalContext)
      LabeledField("Touchstones", setting.targetIntertextualTouchstones)
      LabeledField("Other Notes", setting.otherNotes)
    }
  }
}
