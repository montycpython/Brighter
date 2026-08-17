package com.example.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.GoogleDriveSyncService
import com.example.model.ManuscriptEntity
import com.example.model.UserProfile
import com.example.ui.theme.BookGold
import com.example.ui.theme.BookGoldDark
import com.example.ui.theme.BookGoldLight

@Composable
fun SyncToGoogleDriveDialog(
    manuscript: ManuscriptEntity,
    currentUser: UserProfile,
    isSyncing: Boolean,
    onDismiss: () -> Unit,
    onConfirmSync: (isPublicInCommunity: Boolean) -> Unit
) {
    var isPublicInCommunity by remember { mutableStateOf(true) }

    Dialog(onDismissRequest = { if (!isSyncing) onDismiss() }) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("dialog_sync_drive"),
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFF14141C),
            border = BorderStroke(1.2.dp, BookGoldDark)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = BookGoldDark.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, BookGoldDark),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, tint = BookGoldLight, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Sync to Google Drive",
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFFF3EFE6)
                        )
                        Text(
                            text = "Decentralized JSON Backup & Shared Drive Registry",
                            fontSize = 10.5.sp,
                            color = BookGoldLight
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Manuscript Target Card
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF1B1B26),
                    border = BorderStroke(1.dp, Color(0xFF2C2C3A)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = manuscript.title,
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFFF3EFE6),
                                modifier = Modifier.weight(1f)
                            )
                            Surface(
                                color = BookGoldDark.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = manuscript.edition.ifBlank { "v1.0" },
                                    color = BookGoldLight,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Format: Chicago Manual of Style JSON • Account: ${currentUser.email}",
                            fontSize = 10.5.sp,
                            color = Color(0xFFB0A89C)
                        )
                        Text(
                            text = "Status: ${manuscript.manuscriptStatus} • Sync creates an immutable backup record",
                            fontSize = 10.sp,
                            color = BookGoldLight
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Privacy & Discovery Switch
                Text(
                    text = "Global Book Index Visibility",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = BookGoldLight
                )
                Spacer(modifier = Modifier.height(6.dp))

                // Option 1: Public
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isPublicInCommunity) Color(0xFF1F2922) else Color(0xFF161620),
                    border = BorderStroke(1.dp, if (isPublicInCommunity) Color(0xFF4CAF50) else Color(0xFF2C2C38)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isPublicInCommunity = true }
                        .padding(vertical = 3.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isPublicInCommunity,
                            onClick = { isPublicInCommunity = true },
                            colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF81C784))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Public, contentDescription = null, tint = Color(0xFF81C784), modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Public in Community Index", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFFF3EFE6))
                            }
                            Text(
                                "Other authors using Bwriter Press can discover your title in the community index.",
                                fontSize = 10.sp,
                                color = Color(0xFFB0A89C),
                                lineHeight = 13.sp
                            )
                        }
                    }
                }

                // Option 2: Hidden / Private
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (!isPublicInCommunity) Color(0xFF2E2218) else Color(0xFF161620),
                    border = BorderStroke(1.dp, if (!isPublicInCommunity) BookGold else Color(0xFF2C2C38)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isPublicInCommunity = false }
                        .padding(vertical = 3.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = !isPublicInCommunity,
                            onClick = { isPublicInCommunity = false },
                            colors = RadioButtonDefaults.colors(selectedColor = BookGold)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = BookGoldLight, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Hide (Private to Author & Editor in Chief)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFFF3EFE6))
                            }
                            Text(
                                "Your visibility will be strictly limited to yourself and the Editor in Chief (${GoogleDriveSyncService.EDITOR_IN_CHIEF_EMAIL}).",
                                fontSize = 10.sp,
                                color = Color(0xFFB0A89C),
                                lineHeight = 13.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Auto-share Hook notice
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF0E0E14),
                    border = BorderStroke(1.dp, Color(0xFF2C2C38)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = BookGoldLight, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Auto-Share Hook: Uploading grants ${GoogleDriveSyncService.EDITOR_IN_CHIEF_EMAIL} writer/editorial access automatically.",
                            fontSize = 10.sp,
                            color = Color(0xFFB0A89C),
                            lineHeight = 13.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        enabled = !isSyncing,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = Color.LightGray)
                    }

                    Button(
                        onClick = { onConfirmSync(isPublicInCommunity) },
                        enabled = !isSyncing,
                        colors = ButtonDefaults.buttonColors(containerColor = BookGoldDark),
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("btn_start_drive_sync")
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Syncing...", fontSize = 11.5.sp)
                        } else {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sync to Drive", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
