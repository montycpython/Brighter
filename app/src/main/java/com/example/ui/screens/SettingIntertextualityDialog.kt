package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.example.model.StorySettingEntity
import com.example.ui.theme.BookGold
import com.example.ui.theme.BookGoldDark
import com.example.ui.theme.BookGoldLight

@Composable
fun SettingIntertextualityDialog(
    manuscriptId: Long,
    settings: List<StorySettingEntity>,
    onDismiss: () -> Unit,
    onSaveSetting: (StorySettingEntity) -> Unit,
    onDeleteSetting: (Long) -> Unit,
    onInsertSetting: (StorySettingEntity) -> Unit
) {
    var editingSetting by remember { mutableStateOf<StorySettingEntity?>(null) }
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
            color = Color(0xFF121216), // Dark background for high-contrast visibility
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
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = BookGoldLight,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Settings & Intertextuality Studio",
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color(0xFFF3EFE6)
                            )
                            Text(
                                text = "Worldbuilding environments and literary touchstone conversations",
                                fontSize = 11.sp,
                                color = Color(0xFFB0A89C)
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("btn_close_settings_dialog")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (editingSetting != null || isCreatingNew) {
                    val current = editingSetting ?: StorySettingEntity(
                        manuscriptId = manuscriptId,
                        locationName = ""
                    )
                    SettingEditForm(
                        initial = current,
                        onCancel = {
                            editingSetting = null
                            isCreatingNew = false
                        },
                        onSave = { saved ->
                            onSaveSetting(saved)
                            editingSetting = null
                            isCreatingNew = false
                        }
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Configured Settings (${settings.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = BookGoldLight
                        )
                        Button(
                            onClick = { isCreatingNew = true },
                            colors = ButtonDefaults.buttonColors(containerColor = BookGoldDark),
                            modifier = Modifier.testTag("btn_add_new_setting")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New Setting", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (settings.isEmpty()) {
                        Box(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "No story settings defined yet for this work.",
                                    color = Color.Gray,
                                    fontSize = 13.sp,
                                    fontStyle = FontStyle.Italic
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { isCreatingNew = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = BookGoldDark)
                                ) {
                                    Text("Add First Setting Profile")
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(settings) { set ->
                                SettingCard(
                                    setting = set,
                                    onEdit = { editingSetting = set },
                                    onDelete = { onDeleteSetting(set.id) },
                                    onInsert = {
                                        onInsertSetting(set)
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
fun SettingCard(
    setting: StorySettingEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onInsert: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("setting_card_${setting.id}"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C24)),
        border = BorderStroke(1.dp, Color(0xFF33333E))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = setting.locationName,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif,
                        fontSize = 15.sp,
                        color = Color(0xFFF3EFE6)
                    )
                    if (setting.timePeriodOrEra.isNotBlank()) {
                        Text(
                            text = setting.timePeriodOrEra,
                            fontSize = 11.sp,
                            color = BookGoldLight
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

            if (setting.atmosphereAndSensory.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Atmosphere: ${setting.atmosphereAndSensory}",
                    fontSize = 11.5.sp,
                    color = Color(0xFFD0CAC0),
                    lineHeight = 16.sp
                )
            }

            if (setting.architecturalOrSpatialDetails.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Architecture/Space: ${setting.architecturalOrSpatialDetails}",
                    fontSize = 11.sp,
                    color = Color(0xFFB0A89C)
                )
            }

            if (setting.targetIntertextualTouchstones.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoStories, contentDescription = null, tint = BookGoldLight, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Intertextuality: ${setting.targetIntertextualTouchstones}",
                        fontSize = 11.sp,
                        color = BookGoldLight,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = onInsert,
                colors = ButtonDefaults.buttonColors(containerColor = BookGoldDark),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
                    .testTag("btn_insert_setting_${setting.id}")
            ) {
                Icon(Icons.AutoMirrored.Filled.Input, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Insert Setting & Touchstones into Prompt", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SettingEditForm(
    initial: StorySettingEntity,
    onCancel: () -> Unit,
    onSave: (StorySettingEntity) -> Unit
) {
    var locationName by remember { mutableStateOf(initial.locationName) }
    var timePeriodOrEra by remember { mutableStateOf(initial.timePeriodOrEra) }
    var atmosphereAndSensory by remember { mutableStateOf(initial.atmosphereAndSensory) }
    var architecturalOrSpatialDetails by remember { mutableStateOf(initial.architecturalOrSpatialDetails) }
    var historicalOrCulturalContext by remember { mutableStateOf(initial.historicalOrCulturalContext) }
    var targetIntertextualTouchstones by remember { mutableStateOf(initial.targetIntertextualTouchstones) }
    var otherNotes by remember { mutableStateOf(initial.otherNotes) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = if (initial.id == 0L) "Define New Story Setting" else "Edit Setting: ${initial.locationName}",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = BookGoldLight
        )
        Spacer(modifier = Modifier.height(10.dp))

        // Location Name
        OutlinedTextField(
            value = locationName,
            onValueChange = { locationName = it },
            label = { Text("Setting / Location Name *", color = BookGoldLight) },
            modifier = Modifier.fillMaxWidth().testTag("input_setting_name"),
            textStyle = TextStyle(color = Color(0xFFF3EFE6)),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BookGold,
                unfocusedBorderColor = Color(0xFF444455),
                focusedContainerColor = Color(0xFF1A1A22),
                unfocusedContainerColor = Color(0xFF16161E)
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Era / Time Period
        OutlinedTextField(
            value = timePeriodOrEra,
            onValueChange = { timePeriodOrEra = it },
            label = { Text("Time Period / Season / Era (e.g. Autumn 1888 Chicago)", color = BookGoldLight) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = TextStyle(color = Color(0xFFF3EFE6)),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BookGold,
                unfocusedBorderColor = Color(0xFF444455),
                focusedContainerColor = Color(0xFF1A1A22),
                unfocusedContainerColor = Color(0xFF16161E)
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Atmosphere & Sensory Details
        OutlinedTextField(
            value = atmosphereAndSensory,
            onValueChange = { atmosphereAndSensory = it },
            label = { Text("Atmosphere & Sensory Details (Smells, Lighting, Weather, Acoustics)", color = BookGoldLight) },
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

        // Spatial & Architectural Details
        OutlinedTextField(
            value = architecturalOrSpatialDetails,
            onValueChange = { architecturalOrSpatialDetails = it },
            label = { Text("Architectural & Spatial Details (Furniture, Materials, Scale)", color = BookGoldLight) },
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

        // Intertextual Touchstones
        OutlinedTextField(
            value = targetIntertextualTouchstones,
            onValueChange = { targetIntertextualTouchstones = it },
            label = { Text("Intertextual Touchstones (Books/Authors this setting communicates with)", color = BookGoldLight) },
            modifier = Modifier.fillMaxWidth().height(80.dp),
            textStyle = TextStyle(color = Color(0xFFF3EFE6), fontSize = 12.5.sp),
            placeholder = { Text("e.g. Theodore Dreiser's 'Sister Carrie', Umberto Eco's 'The Name of the Rose'", color = Color.Gray, fontSize = 11.5.sp) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BookGold,
                unfocusedBorderColor = Color(0xFF444455),
                focusedContainerColor = Color(0xFF1A1A22),
                unfocusedContainerColor = Color(0xFF16161E)
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Historical / Cultural Context
        OutlinedTextField(
            value = historicalOrCulturalContext,
            onValueChange = { historicalOrCulturalContext = it },
            label = { Text("Historical & Cultural Context", color = BookGoldLight) },
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
                    if (locationName.isNotBlank()) {
                        onSave(
                            initial.copy(
                                locationName = locationName.trim(),
                                timePeriodOrEra = timePeriodOrEra.trim(),
                                atmosphereAndSensory = atmosphereAndSensory.trim(),
                                architecturalOrSpatialDetails = architecturalOrSpatialDetails.trim(),
                                historicalOrCulturalContext = historicalOrCulturalContext.trim(),
                                targetIntertextualTouchstones = targetIntertextualTouchstones.trim(),
                                otherNotes = otherNotes.trim()
                            )
                        )
                    }
                },
                enabled = locationName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = BookGoldDark),
                modifier = Modifier.weight(1f).testTag("btn_save_setting")
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Save Setting", fontWeight = FontWeight.Bold)
            }
        }
    }
}
