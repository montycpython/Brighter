package com.supernatural.bwriter.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.supernatural.bwriter.data.FirestoreNetworkService
import com.supernatural.bwriter.model.UserProfile
import com.supernatural.bwriter.model.WorkRole
import com.supernatural.bwriter.ui.components.RoleBadge
import com.supernatural.bwriter.ui.theme.BookGold
import com.supernatural.bwriter.ui.theme.BookGoldDark
import com.supernatural.bwriter.ui.theme.BookGoldLight
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthorProfileDialog(
    currentUser: UserProfile,
    onDismiss: () -> Unit,
    onSaveProfile: (name: String, penName: String, email: String, role: WorkRole, organization: String, cmosEdition: String) -> Unit,
    onOpenSubscription: () -> Unit = {},
    onOpenUserAgreement: () -> Unit = {},
    onOpenVersion: () -> Unit = {},
    onOpenAuthScreen: () -> Unit = {},
    onSignOut: () -> Unit = {},
    onRequestAccountDeletion: ((Boolean) -> Unit) -> Unit = {}
) {
    var legalName by remember { mutableStateOf(currentUser.name) }
    var penName by remember { mutableStateOf(currentUser.penName) }
    var email by remember { mutableStateOf(currentUser.email) }
    val selectedRole = if (com.supernatural.bwriter.data.StudioStanding.isEditorInChief) WorkRole.EDITOR else WorkRole.AUTHOR
    var organization by remember { mutableStateOf(currentUser.organization) }
    var cmosEdition by remember { mutableStateOf(currentUser.preferredCmosEdition) }

    var cmosDropdownExpanded by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var deletionSending by remember { mutableStateOf(false) }
    var deletionSent by remember { mutableStateOf(false) }
    var deletionFailed by remember { mutableStateOf(false) }
    val cmosOptions = listOf("17th Edition", "18th Edition", "16th Edition")

    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    val effectiveDisplayName = when {
        penName.isNotBlank() -> penName.trim()
        legalName.isNotBlank() -> legalName.trim()
        else -> email
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            border = BorderStroke(1.dp, BookGoldDark.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("author_profile_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(BookGoldDark),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Author Profile & Identity",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Customize your Byline, Pen Name, & Account",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("btn_close_profile_dialog")) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF161622)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, BookGoldDark.copy(alpha = 0.6f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "LIVE BYLINE & COPYRIGHT PREVIEW",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = BookGoldLight,
                                letterSpacing = 1.sp
                            )
                            RoleBadge(role = selectedRole, isEditorInChief = com.supernatural.bwriter.data.StudioStanding.isEditorInChief)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Byline: ",
                                fontSize = 13.sp,
                                color = Color.Gray,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = effectiveDisplayName,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF3EFE6)
                            )
                            if (penName.isNotBlank()) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = BookGoldDark.copy(alpha = 0.3f),
                                    border = BorderStroke(0.5.dp, BookGold)
                                ) {
                                    Text(
                                        text = "PEN NAME ACTIVE",
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BookGoldLight,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Legal Notice: © $currentYear ${if (penName.isNotBlank()) penName else legalName}. All rights reserved.",
                            fontSize = 11.sp,
                            color = Color(0xFFB0A898),
                            fontFamily = FontFamily.Serif
                        )
                        Text(
                            text = "Cloud Account: $email",
                            fontSize = 10.5.sp,
                            color = Color(0xFF8E8E9F)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = penName,
                    onValueChange = { penName = it },
                    label = { Text("Pen Name / Author Byline (Optional)") },
                    placeholder = { Text("e.g. J. D. Salinger, George Orwell") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.DriveFileRenameOutline,
                            contentDescription = "Pen Name",
                            tint = BookGoldDark
                        )
                    },
                    supportingText = {
                        Text("When set, your Pen Name takes precedence on all Book Covers, Title Pages, Leaf Headers, PDF Exports, and Community Index entries.")
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_profile_pen_name")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = legalName,
                    onValueChange = { legalName = it },
                    label = { Text("Author / Legal Name") },
                    placeholder = { Text("e.g. John Doe") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Legal Name",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    supportingText = {
                        Text("Your real or registered legal name for copyright documentation and contract administration.")
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_profile_legal_name")
                )

                Spacer(modifier = Modifier.height(8.dp))

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = organization,
                    onValueChange = { organization = it },
                    label = { Text("Publishing House / Studio Name") },
                    placeholder = { Text("e.g. Artistry Press") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Business,
                            contentDescription = "Publishing House",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_profile_organization")
                )

                Spacer(modifier = Modifier.height(12.dp))

                ExposedDropdownMenuBox(
                    expanded = cmosDropdownExpanded,
                    onExpandedChange = { cmosDropdownExpanded = !cmosDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = cmosEdition,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Preferred CMOS Edition") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = cmosDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = cmosDropdownExpanded,
                        onDismissRequest = { cmosDropdownExpanded = false }
                    ) {
                        cmosOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    cmosEdition = option
                                    cmosDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Workspace Role & Permissions",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RoleBadge(role = selectedRole, isEditorInChief = com.supernatural.bwriter.data.StudioStanding.isEditorInChief)
                    Text(
                        text = "Editor standing is granted by invitation.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        onSaveProfile(legalName, penName, email, selectedRole, organization, cmosEdition)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BookGoldDark),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_save_author_profile")
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save Profile Changes", fontWeight = FontWeight.Bold, fontSize = 14.5.sp)
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onSignOut()
                    },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("btn_profile_sign_out")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = "Log Out",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Log Out of Google Account (${currentUser.email})",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (!com.supernatural.bwriter.data.StudioStanding.isEditorInChief) {
                    OutlinedButton(
                        onClick = { showDeleteConfirm = true },
                        enabled = !deletionSending && !deletionSent,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("btn_profile_delete_account")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteForever,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when {
                                deletionSent -> "Deletion Requested"
                                deletionSending -> "Sending Request\u2026"
                                else -> "Delete Account"
                            },
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                    if (deletionFailed) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Request could not be sent. Check your connection and try again.",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        onClick = {
                            onDismiss()
                            onOpenSubscription()
                        },
                        modifier = Modifier.testTag("btn_profile_open_subscription")
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp), tint = BookGoldLight)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("AI Studio Pass", fontSize = 11.5.sp)
                    }

                    TextButton(
                        onClick = {
                            onDismiss()
                            onOpenUserAgreement()
                        },
                        modifier = Modifier.testTag("btn_profile_open_agreement")
                    ) {
                        Icon(Icons.Default.Gavel, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("User Agreement", fontSize = 11.5.sp)
                    }

                    TextButton(
                        onClick = {
                            onDismiss()
                            onOpenAuthScreen()
                        },
                        modifier = Modifier.testTag("btn_profile_switch_account")
                    ) {
                        Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Switch Account", fontSize = 11.5.sp)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    TextButton(
                        onClick = {
                            onDismiss()
                            onOpenVersion()
                        },
                        modifier = Modifier.testTag("btn_profile_open_version")
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("App Version", fontSize = 11.5.sp)
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Account?") },
            text = {
                Text("This sends a deletion request for your account. Your cloud data is deleted: back-room book copies, registry entries, in-app mail, and credit records. Books on this device and in your own Google Drive remain yours to delete. This cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        deletionSending = true
                        deletionFailed = false
                        onRequestAccountDeletion { success ->
                            deletionSending = false
                            if (success) deletionSent = true else deletionFailed = true
                        }
                    }
                ) {
                    Text("Request Deletion", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
