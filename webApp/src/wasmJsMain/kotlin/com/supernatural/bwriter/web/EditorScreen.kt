package com.supernatural.bwriter.web

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.supernatural.bwriter.crypto.hexToBytes
import com.supernatural.bwriter.crypto.toHex
import com.supernatural.bwriter.currentTimeMillis

private val WORK_TYPES = listOf("NOVEL", "BIOGRAPHY", "DOCUMENTARY", "MANUAL")

/**
 * The book editor: front matter, sections, cast and world — saved with
 * the account's author key through the same stamp-and-submit path the
 * Android app uses. Monetary features are not here; they live on Android.
 */
@Composable
fun EditorScreen(
  email: String,
  displayName: String,
  initial: EditableBook,
  onBack: () -> Unit,
  onOpenKey: () -> Unit
) {
  var book by remember { mutableStateOf(initial) }
  var revision by remember { mutableStateOf(0) }
  var selectedSection by remember { mutableStateOf(0) }
  var notice by remember { mutableStateOf("") }
  var busy by remember { mutableStateOf(false) }
  var unlockedHex by remember { mutableStateOf("") }
  var unlockPass by remember { mutableStateOf("") }
  val hasKey = KeyVault.sealed(email) != null

  fun touch() {
    book.updatedAt = currentTimeMillis()
    revision++
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
        Text(text = "‹  My Books", color = BwriterGold, fontSize = 16.sp)
      }
      Spacer(modifier = Modifier.weight(1f))
      Button(
        onClick = {
          driveWarmToken()
          val scalar = unlockedHex.hexToBytes()
          if (scalar == null) {
            notice = "Unlock your author key to save."
          } else {
            busy = true
            notice = "Saving…"
            BookSync.save(email, displayName, book, scalar) { success, message ->
              busy = false
              notice = message
              if (success) revision++
            }
          }
        },
        enabled = !busy && unlockedHex.isNotBlank(),
        colors = ButtonDefaults.buttonColors(
          containerColor = AppleInk,
          contentColor = Color.White
        )
      ) {
        Text("Save & Submit")
      }
    }

    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 28.dp, vertical = 4.dp)
    ) {
      // Reading the revision state subscribes this form to every edit.
      val editFrame = revision
      Text(
        text = "Editing — ${book.totalWords()} words total",
        fontSize = 13.sp,
        color = AppleSecondary
      )
      if (notice.isNotBlank()) {
        Text(
          text = notice,
          fontSize = 14.sp,
          color = BwriterGold,
          modifier = Modifier.padding(top = 6.dp)
        )
      }

      if (!hasKey) {
        Surface(
          color = AppleSurface,
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp)
        ) {
          Column(modifier = Modifier.padding(20.dp)) {
            Text(
              text = "This account has no author key on this device yet. " +
                "Create or import one to save and submit books.",
              fontSize = 14.sp,
              color = AppleInk
            )
            Button(
              onClick = onOpenKey,
              colors = ButtonDefaults.buttonColors(
                containerColor = AppleInk,
                contentColor = Color.White
              ),
              modifier = Modifier.padding(top = 12.dp)
            ) {
              Text("Open the key vault")
            }
          }
        }
      } else if (unlockedHex.isBlank()) {
        Surface(
          color = AppleSurface,
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp)
        ) {
          Column(modifier = Modifier.padding(20.dp)) {
            Text(
              text = "Unlock your author key to save.",
              fontSize = 14.sp,
              color = AppleInk
            )
            Row(
              modifier = Modifier.padding(top = 10.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              OutlinedTextField(
                value = unlockPass,
                onValueChange = { unlockPass = it },
                label = { Text("Passphrase") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.weight(1f)
              )
              Spacer(modifier = Modifier.width(10.dp))
              Button(
                onClick = {
                  busy = true
                  KeyVault.unlock(email, unlockPass) { scalar ->
                    busy = false
                    if (scalar == null) {
                      notice = "That passphrase did not open the key."
                    } else {
                      unlockedHex = scalar.toHex()
                      notice = "Key unlocked."
                    }
                  }
                },
                enabled = !busy && unlockPass.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                  containerColor = AppleInk,
                  contentColor = Color.White
                )
              ) {
                Text("Unlock")
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))
      EditorField("Title", book.title) { book.title = it; touch() }
      EditorField("Subtitle", book.subtitle) { book.subtitle = it; touch() }
      EditorField("Pen name", book.authorPenName) { book.authorPenName = it; touch() }
      EditorField("Author name", book.authorName) { book.authorName = it; touch() }
      Row(modifier = Modifier.padding(top = 10.dp)) {
        Text(
          text = "Type: ${book.workType}",
          fontSize = 15.sp,
          color = AppleInk,
          modifier = Modifier.align(Alignment.CenterVertically)
        )
        TextButton(
          onClick = {
            val next = (WORK_TYPES.indexOf(book.workType) + 1) % WORK_TYPES.size
            book.workType = WORK_TYPES[next]
            touch()
          }
        ) {
          Text("Change", color = BwriterGold)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
          text = if (book.isPublicInCommunity) "Public in the library" else "Private",
          fontSize = 15.sp,
          color = AppleInk,
          modifier = Modifier.align(Alignment.CenterVertically)
        )
        TextButton(
          onClick = {
            book.isPublicInCommunity = !book.isPublicInCommunity
            touch()
          }
        ) {
          Text("Change", color = BwriterGold)
        }
      }

      Text(
        text = "FRONT MATTER",
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = AppleSecondary,
        modifier = Modifier.padding(top = 22.dp, bottom = 4.dp)
      )
      EditorField("Publisher", book.publisher) { book.publisher = it; touch() }
      EditorField("Edition", book.edition) { book.edition = it; touch() }
      EditorField("Year", book.year) { book.year = it; touch() }
      EditorField("ISBN", book.isbn) { book.isbn = it; touch() }
      EditorField("Dedication", book.dedication, tall = true) { book.dedication = it; touch() }
      EditorField("Epigraph", book.epigraphText, tall = true) { book.epigraphText = it; touch() }
      EditorField("Epigraph author", book.epigraphAuthor) { book.epigraphAuthor = it; touch() }
      EditorField("Copyright page text", book.copyrightText, tall = true) { book.copyrightText = it; touch() }

      Text(
        text = "SECTIONS",
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = AppleSecondary,
        modifier = Modifier.padding(top = 22.dp, bottom = 4.dp)
      )
      book.sections.forEachIndexed { index, section ->
        val isSelected = index == selectedSection
        Surface(
          color = if (isSelected) AppleSurface else AppleBackground,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clickable { selectedSection = index }
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "${index + 1}. ${section.title.ifBlank { "Untitled" }}",
              fontSize = 15.sp,
              fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
              color = AppleInk,
              modifier = Modifier.weight(1f)
            )
            Text(
              text = "${countWords(section.content)} words",
              fontSize = 13.sp,
              color = AppleSecondary
            )
          }
        }
      }
      Row(modifier = Modifier.padding(top = 6.dp)) {
        TextButton(
          onClick = {
            val next = EditableSection(
              id = currentTimeMillis() + book.sections.size,
              title = "Chapter ${book.sections.size + 1}",
              orderIndex = book.sections.size,
              assignedAuthor = displayName
            )
            book.sections = book.sections + next
            selectedSection = book.sections.lastIndex
            touch()
          }
        ) {
          Text("Add section", color = BwriterGold)
        }
        if (book.sections.size > 1 && selectedSection in book.sections.indices) {
          TextButton(
            onClick = {
              val removed = selectedSection
              book.sections = book.sections.filterIndexed { i, _ -> i != removed }
              book.sections.forEachIndexed { i, s -> s.orderIndex = i }
              selectedSection = minOf(removed, book.sections.lastIndex)
              touch()
            }
          ) {
            Text("Delete selected", color = AppleSecondary)
          }
          if (selectedSection > 0) {
            TextButton(
              onClick = {
                val list = book.sections.toMutableList()
                val item = list.removeAt(selectedSection)
                list.add(selectedSection - 1, item)
                list.forEachIndexed { i, s -> s.orderIndex = i }
                book.sections = list
                selectedSection -= 1
                touch()
              }
            ) {
              Text("Move up", color = AppleSecondary)
            }
          }
          if (selectedSection < book.sections.lastIndex) {
            TextButton(
              onClick = {
                val list = book.sections.toMutableList()
                val item = list.removeAt(selectedSection)
                list.add(selectedSection + 1, item)
                list.forEachIndexed { i, s -> s.orderIndex = i }
                book.sections = list
                selectedSection += 1
                touch()
              }
            ) {
              Text("Move down", color = AppleSecondary)
            }
          }
        }
      }

      val section = book.sections.getOrNull(selectedSection)
      if (section != null) {
        EditorField("Section title", section.title) { section.title = it; touch() }
        EditorField("Section subtitle", section.subtitle) { section.subtitle = it; touch() }
        Text(
          text = "CONTENT",
          fontSize = 12.sp,
          fontWeight = FontWeight.SemiBold,
          color = AppleSecondary,
          modifier = Modifier.padding(top = 14.dp, bottom = 4.dp)
        )
        OutlinedTextField(
          value = section.content,
          onValueChange = {
            section.content = it
            section.wordCount = countWords(it)
            touch()
          },
          modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
        )
      }

      Text(
        text = "CAST",
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = AppleSecondary,
        modifier = Modifier.padding(top = 22.dp, bottom = 4.dp)
      )
      book.characters.forEachIndexed { index, character ->
        Surface(
          color = AppleSurface,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            EditorField("Name", character.name) { character.name = it; touch() }
            EditorField("Role", character.role) { character.role = it; touch() }
            EditorField("Physical description", character.physicalDescription, tall = true) {
              character.physicalDescription = it; touch()
            }
            EditorField("Psychology", character.psychologicalDescription, tall = true) {
              character.psychologicalDescription = it; touch()
            }
            EditorField("Backstory", character.backstory, tall = true) {
              character.backstory = it; touch()
            }
            EditorField("Voice and mannerisms", character.voiceAndMannerisms, tall = true) {
              character.voiceAndMannerisms = it; touch()
            }
            EditorField("Archetype", character.intertextualArchetype) {
              character.intertextualArchetype = it; touch()
            }
            EditorField("Other details", character.otherDetails, tall = true) {
              character.otherDetails = it; touch()
            }
            TextButton(
              onClick = {
                book.characters = book.characters.filterIndexed { i, _ -> i != index }
                touch()
              }
            ) {
              Text("Remove character", color = AppleSecondary)
            }
          }
        }
      }
      TextButton(
        onClick = {
          book.characters = book.characters + EditableCharacter(
            id = currentTimeMillis() + book.characters.size
          )
          touch()
        }
      ) {
        Text("Add character", color = BwriterGold)
      }

      Text(
        text = "WORLD",
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = AppleSecondary,
        modifier = Modifier.padding(top = 22.dp, bottom = 4.dp)
      )
      book.settings.forEachIndexed { index, setting ->
        Surface(
          color = AppleSurface,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            EditorField("Location", setting.locationName) { setting.locationName = it; touch() }
            EditorField("Time period or era", setting.timePeriodOrEra) {
              setting.timePeriodOrEra = it; touch()
            }
            EditorField("Atmosphere and sensory", setting.atmosphereAndSensory, tall = true) {
              setting.atmosphereAndSensory = it; touch()
            }
            EditorField("Architecture and space", setting.architecturalOrSpatialDetails, tall = true) {
              setting.architecturalOrSpatialDetails = it; touch()
            }
            EditorField("Historical and cultural context", setting.historicalOrCulturalContext, tall = true) {
              setting.historicalOrCulturalContext = it; touch()
            }
            EditorField("Intertextual touchstones", setting.targetIntertextualTouchstones, tall = true) {
              setting.targetIntertextualTouchstones = it; touch()
            }
            EditorField("Other notes", setting.otherNotes, tall = true) {
              setting.otherNotes = it; touch()
            }
            TextButton(
              onClick = {
                book.settings = book.settings.filterIndexed { i, _ -> i != index }
                touch()
              }
            ) {
              Text("Remove setting", color = AppleSecondary)
            }
          }
        }
      }
      TextButton(
        onClick = {
          book.settings = book.settings + EditableSetting(
            id = currentTimeMillis() + book.settings.size
          )
          touch()
        }
      ) {
        Text("Add setting", color = BwriterGold)
      }

      Spacer(modifier = Modifier.height(48.dp))
    }
  }
}

@Composable
private fun EditorField(
  label: String,
  value: String,
  tall: Boolean = false,
  onChange: (String) -> Unit
) {
  OutlinedTextField(
    value = value,
    onValueChange = onChange,
    label = { Text(label) },
    modifier = Modifier
      .fillMaxWidth()
      .padding(top = 8.dp)
      .then(if (tall) Modifier.height(110.dp) else Modifier)
  )
}
