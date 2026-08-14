package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.cmos.CmosFormatter
import com.example.model.WorkType
import com.example.ui.components.WorkTypeIcon
import com.example.ui.theme.BookGoldDark

@Composable
fun NewWorkDialog(
    initialAuthorName: String,
    initialPenName: String = "",
    onDismiss: () -> Unit,
    onCreate: (
        title: String,
        subtitle: String,
        workType: WorkType,
        author: String,
        penName: String,
        publisher: String,
        year: String,
        dedication: String,
        epigraph: String
    ) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var subtitle by remember { mutableStateOf("") }
    var selectedWorkType by remember { mutableStateOf(WorkType.NOVEL) }
    var authorName by remember { mutableStateOf(initialAuthorName) }
    var authorPenName by remember { mutableStateOf(initialPenName) }
    var publisher by remember { mutableStateOf("Bwriter Editions") }
    var year by remember { mutableStateOf("2026") }
    var dedication by remember { mutableStateOf("") }
    var epigraph by remember { mutableStateOf("") }

    val effectiveByline = when {
        authorPenName.isNotBlank() -> authorPenName.trim()
        authorName.isNotBlank() -> authorName.trim()
        else -> "Author"
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("dialog_new_work")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Create New Manuscript",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Text(
                    text = "Scaffolds Front Matter, Body Matter, and Back Matter with Chicago Manual of Style recto/verso leaf placement.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Work Type Selection
                Text(
                    text = "Work Classification",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    WorkType.values().forEach { type ->
                        val isSelected = selectedWorkType == type
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedWorkType = type }
                                .testTag("work_type_${type.name}"),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                }
                            ),
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                WorkTypeIcon(
                                    workType = type,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = type.displayName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = type.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Manuscript Title *") },
                    placeholder = { Text("e.g. The Architecture of Prose") },
                    singleLine = true,
                    trailingIcon = {
                        if (title.isNotBlank()) {
                            Text(
                                text = "CMOS Case",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .clickable {
                                        title = CmosFormatter.toChicagoHeadlineCase(title)
                                    }
                                    .padding(8.dp)
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_work_title")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Subtitle
                OutlinedTextField(
                    value = subtitle,
                    onValueChange = { subtitle = it },
                    label = { Text("Subtitle (Optional)") },
                    placeholder = { Text("e.g. A Treatise on Typographical Structure") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_work_subtitle")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Author Legal Name & Pen Name
                OutlinedTextField(
                    value = authorName,
                    onValueChange = { authorName = it },
                    label = { Text("Author Legal Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = authorPenName,
                    onValueChange = { authorPenName = it },
                    label = { Text("Pen Name / Byline (Optional)") },
                    placeholder = { Text("Overrides author name for book byline & copyright") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.DriveFileRenameOutline,
                            contentDescription = "Pen Name",
                            tint = BookGoldDark
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Year & Publisher
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = year,
                        onValueChange = { year = it },
                        label = { Text("Publication Year") },
                        singleLine = true,
                        modifier = Modifier.weight(0.8f)
                    )
                    OutlinedTextField(
                        value = publisher,
                        onValueChange = { publisher = it },
                        label = { Text("Publisher / Imprint") },
                        singleLine = true,
                        modifier = Modifier.weight(1.2f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Dynamic Copyright Preview Box
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Dynamic Copyright Notice Preview (Verso p. iv):",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Copyright © ${year.ifBlank { "2026" }} by $effectiveByline.\nAll rights reserved.\nPublished by ${publisher.ifBlank { "Bwriter Editions" }} in accordance with The Chicago Manual of Style.",
                            fontFamily = FontFamily.Serif,
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = dedication,
                    onValueChange = { dedication = it },
                    label = { Text("Dedication Leaf (Optional)") },
                    placeholder = { Text("To those who cherish the printed word...") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = epigraph,
                    onValueChange = { epigraph = it },
                    label = { Text("Epigraph Quotation (Optional)") },
                    placeholder = { Text("“Style is the dress of thought...”") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                onCreate(
                                    title.trim(),
                                    subtitle.trim(),
                                    selectedWorkType,
                                    authorName.trim(),
                                    authorPenName.trim(),
                                    publisher.trim(),
                                    year.trim(),
                                    dedication.trim(),
                                    epigraph.trim()
                                )
                            }
                        },
                        enabled = title.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = BookGoldDark),
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("btn_submit_create_work")
                    ) {
                        Text("Create Manuscript")
                    }
                }
            }
        }
    }
}
