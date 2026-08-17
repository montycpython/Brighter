package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Input
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.CharacterEntity
import com.example.ui.theme.BookGold
import com.example.ui.theme.BookGoldDark
import com.example.ui.theme.BookGoldLight
import com.example.ui.theme.InkBlack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterProfilerDialog(
    manuscriptId: Long,
    characters: List<CharacterEntity>,
    onDismiss: () -> Unit,
    onSaveCharacter: (CharacterEntity) -> Unit,
    onDeleteCharacter: (Long) -> Unit,
    onInsertIntoPrompt: (CharacterEntity) -> Unit
) {
    var editingCharacter by remember { mutableStateOf<CharacterEntity?>(null) }
    var isCreatingNew by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF121216), // Dark background for crystal clear high contrast
            border = BorderStroke(1.dp, BookGoldDark.copy(alpha = 0.6f))
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = BookGoldLight,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Character Profiler & Dossier",
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color(0xFFF3EFE6)
                            )
                            Text(
                                text = "Profile personas for dynamic injection into CMOS chapter drafting",
                                fontSize = 11.sp,
                                color = Color(0xFFB0A89C)
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("btn_close_character_profiler")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (editingCharacter != null || isCreatingNew) {
                    // EDIT / CREATE FORM
                    val current = editingCharacter ?: CharacterEntity(
                        manuscriptId = manuscriptId,
                        name = ""
                    )
                    CharacterEditForm(
                        initial = current,
                        onCancel = {
                            editingCharacter = null
                            isCreatingNew = false
                        },
                        onSave = { saved ->
                            onSaveCharacter(saved)
                            editingCharacter = null
                            isCreatingNew = false
                        }
                    )
                } else {
                    // LIST VIEW
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Characters in Work (${characters.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = BookGoldLight
                        )
                        Button(
                            onClick = { isCreatingNew = true },
                            colors = ButtonDefaults.buttonColors(containerColor = BookGoldDark),
                            modifier = Modifier.testTag("btn_add_new_character")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New Character", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (characters.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "No characters profiled yet for this manuscript.",
                                    color = Color.Gray,
                                    fontSize = 13.sp,
                                    fontStyle = FontStyle.Italic
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { isCreatingNew = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = BookGoldDark)
                                ) {
                                    Text("Create First Character Profile")
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(characters) { char ->
                                CharacterCard(
                                    character = char,
                                    onEdit = { editingCharacter = char },
                                    onDelete = { onDeleteCharacter(char.id) },
                                    onInsert = {
                                        onInsertIntoPrompt(char)
                                        onDismiss()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CharacterCard(
    character: CharacterEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onInsert: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("character_card_${character.id}"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C24)),
        border = BorderStroke(1.dp, Color(0xFF33333E))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = character.name,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif,
                        fontSize = 16.sp,
                        color = Color(0xFFF3EFE6)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = BookGoldDark.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(0.5.dp, BookGoldDark)
                    ) {
                        Text(
                            text = character.role,
                            fontSize = 10.sp,
                            color = BookGoldLight,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.LightGray, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFE57373), modifier = Modifier.size(16.dp))
                    }
                }
            }

            if (character.physicalDescription.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Physical: ${character.physicalDescription}",
                    fontSize = 11.5.sp,
                    color = Color(0xFFD0CAC0),
                    lineHeight = 16.sp
                )
            }

            if (character.psychologicalDescription.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Psychology: ${character.psychologicalDescription}",
                    fontSize = 11.5.sp,
                    color = Color(0xFFD0CAC0),
                    lineHeight = 16.sp
                )
            }

            if (character.voiceAndMannerisms.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Voice & Habits: ${character.voiceAndMannerisms}",
                    fontSize = 11.sp,
                    color = Color(0xFFB0A89C),
                    fontStyle = FontStyle.Italic
                )
            }

            if (character.intertextualArchetype.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Archetype: ${character.intertextualArchetype}",
                    fontSize = 10.5.sp,
                    color = BookGoldLight
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = onInsert,
                colors = ButtonDefaults.buttonColors(containerColor = BookGoldDark),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
                    .testTag("btn_insert_char_${character.id}")
            ) {
                Icon(Icons.AutoMirrored.Filled.Input, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Insert Profile into AI Prompt Workshop", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun CharacterEditForm(
    initial: CharacterEntity,
    onCancel: () -> Unit,
    onSave: (CharacterEntity) -> Unit
) {
    var name by remember { mutableStateOf(initial.name) }
    var role by remember { mutableStateOf(initial.role) }
    var physicalDescription by remember { mutableStateOf(initial.physicalDescription) }
    var psychologicalDescription by remember { mutableStateOf(initial.psychologicalDescription) }
    var backstory by remember { mutableStateOf(initial.backstory) }
    var voiceAndMannerisms by remember { mutableStateOf(initial.voiceAndMannerisms) }
    var intertextualArchetype by remember { mutableStateOf(initial.intertextualArchetype) }
    var otherDetails by remember { mutableStateOf(initial.otherDetails) }

    val roles = listOf("Protagonist", "Antagonist", "Foil", "Deuteragonist", "Mentor", "Supporting", "Narrator")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = if (initial.id == 0L) "Create New Character Profile" else "Edit Character: ${initial.name}",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = BookGoldLight
        )
        Spacer(modifier = Modifier.height(10.dp))

        // Name
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Character Name *", color = BookGoldLight) },
            modifier = Modifier.fillMaxWidth().testTag("input_char_name"),
            textStyle = TextStyle(color = Color(0xFFF3EFE6)),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BookGold,
                unfocusedBorderColor = Color(0xFF444455),
                focusedContainerColor = Color(0xFF1A1A22),
                unfocusedContainerColor = Color(0xFF16161E)
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Role Selector Chips
        Text("Dramatic Role:", fontSize = 11.sp, color = Color(0xFFB0A89C))
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            roles.take(4).forEach { r ->
                FilterChip(
                    selected = role == r,
                    onClick = { role = r },
                    label = { Text(r, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = BookGoldDark,
                        selectedLabelColor = Color.White,
                        containerColor = Color(0xFF1F1F28),
                        labelColor = Color(0xFFD0CAC0)
                    )
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            roles.drop(4).forEach { r ->
                FilterChip(
                    selected = role == r,
                    onClick = { role = r },
                    label = { Text(r, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = BookGoldDark,
                        selectedLabelColor = Color.White,
                        containerColor = Color(0xFF1F1F28),
                        labelColor = Color(0xFFD0CAC0)
                    )
                )
            }
        }

        // Physical Description
        OutlinedTextField(
            value = physicalDescription,
            onValueChange = { physicalDescription = it },
            label = { Text("Physical Description (Features, Attire, Posture, Age)", color = BookGoldLight) },
            modifier = Modifier.fillMaxWidth().height(90.dp),
            textStyle = TextStyle(color = Color(0xFFF3EFE6), fontSize = 12.5.sp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BookGold,
                unfocusedBorderColor = Color(0xFF444455),
                focusedContainerColor = Color(0xFF1A1A22),
                unfocusedContainerColor = Color(0xFF16161E)
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Psychological Description
        OutlinedTextField(
            value = psychologicalDescription,
            onValueChange = { psychologicalDescription = it },
            label = { Text("Psychological Profile (Motivations, Flaws, Moral Code, Fears)", color = BookGoldLight) },
            modifier = Modifier.fillMaxWidth().height(90.dp),
            textStyle = TextStyle(color = Color(0xFFF3EFE6), fontSize = 12.5.sp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BookGold,
                unfocusedBorderColor = Color(0xFF444455),
                focusedContainerColor = Color(0xFF1A1A22),
                unfocusedContainerColor = Color(0xFF16161E)
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Backstory
        OutlinedTextField(
            value = backstory,
            onValueChange = { backstory = it },
            label = { Text("Backstory & Formative History (Origins, Traumas, Alliances)", color = BookGoldLight) },
            modifier = Modifier.fillMaxWidth().height(80.dp),
            textStyle = TextStyle(color = Color(0xFFF3EFE6), fontSize = 12.5.sp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BookGold,
                unfocusedBorderColor = Color(0xFF444455),
                focusedContainerColor = Color(0xFF1A1A22),
                unfocusedContainerColor = Color(0xFF16161E)
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Voice & Mannerisms
        OutlinedTextField(
            value = voiceAndMannerisms,
            onValueChange = { voiceAndMannerisms = it },
            label = { Text("Voice, Cadence & Mannerisms (Speech habits, Tics)", color = BookGoldLight) },
            modifier = Modifier.fillMaxWidth().height(70.dp),
            textStyle = TextStyle(color = Color(0xFFF3EFE6), fontSize = 12.5.sp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BookGold,
                unfocusedBorderColor = Color(0xFF444455),
                focusedContainerColor = Color(0xFF1A1A22),
                unfocusedContainerColor = Color(0xFF16161E)
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Intertextual Archetype
        OutlinedTextField(
            value = intertextualArchetype,
            onValueChange = { intertextualArchetype = it },
            label = { Text("Intertextual Parallel / Literary Archetype (e.g. Faustian seeker)", color = BookGoldLight) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = TextStyle(color = Color(0xFFF3EFE6), fontSize = 12.5.sp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BookGold,
                unfocusedBorderColor = Color(0xFF444455),
                focusedContainerColor = Color(0xFF1A1A22),
                unfocusedContainerColor = Color(0xFF16161E)
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Other Details
        OutlinedTextField(
            value = otherDetails,
            onValueChange = { otherDetails = it },
            label = { Text("Crucial Story Props, Secrets & Character Arc", color = BookGoldLight) },
            modifier = Modifier.fillMaxWidth().height(70.dp),
            textStyle = TextStyle(color = Color(0xFFF3EFE6), fontSize = 12.5.sp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BookGold,
                unfocusedBorderColor = Color(0xFF444455),
                focusedContainerColor = Color(0xFF1A1A22),
                unfocusedContainerColor = Color(0xFF16161E)
            )
        )

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f)
            ) {
                Text("Cancel", color = Color.LightGray)
            }
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(
                            initial.copy(
                                name = name.trim(),
                                role = role,
                                physicalDescription = physicalDescription.trim(),
                                psychologicalDescription = psychologicalDescription.trim(),
                                backstory = backstory.trim(),
                                voiceAndMannerisms = voiceAndMannerisms.trim(),
                                intertextualArchetype = intertextualArchetype.trim(),
                                otherDetails = otherDetails.trim()
                            )
                        )
                    }
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = BookGoldDark),
                modifier = Modifier.weight(1f).testTag("btn_save_character")
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Save Profile", fontWeight = FontWeight.Bold)
            }
        }
    }
}
