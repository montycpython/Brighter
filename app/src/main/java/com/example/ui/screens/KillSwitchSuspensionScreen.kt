package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwitchAccount
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.GoogleDriveSyncService
import com.example.model.SuspendedUserEntry
import com.example.model.UserProfile
import com.example.ui.theme.BookGold
import com.example.ui.theme.BookGoldDark
import com.example.ui.theme.BookGoldLight
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun KillSwitchSuspensionScreen(
    userProfile: UserProfile,
    suspension: SuspendedUserEntry,
    onRetrySync: () -> Unit,
    onSwitchUser: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
    val suspendedDateStr = try {
        dateFormat.format(Date(suspension.suspendedAt))
    } catch (e: Exception) {
        "Recent"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0D))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("kill_switch_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF14141A)),
            border = BorderStroke(1.5.dp, Color(0xFFE53935).copy(alpha = 0.8f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Warning Icon
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFE53935).copy(alpha = 0.15f),
                    border = BorderStroke(2.dp, Color(0xFFE53935)),
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = "Account Locked",
                            tint = Color(0xFFE53935),
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Account Suspended",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = Color(0xFFF3EFE6)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Administrative Hard-Lock by Editor in Chief",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFE57373),
                    letterSpacing = 0.5.sp
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Detail Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1B1B24),
                    border = BorderStroke(1.dp, Color(0xFF2C2C38))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Gavel, contentDescription = null, tint = BookGoldLight, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Suspension Notice",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = BookGoldLight
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Account: ${userProfile.email}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFE2DDD5)
                        )
                        Text(
                            text = "Effective Date: $suspendedDateStr",
                            fontSize = 11.sp,
                            color = Color(0xFFB0A89C)
                        )
                        Text(
                            text = "Enforced By: ${suspension.suspendedBy}",
                            fontSize = 11.sp,
                            color = Color(0xFFB0A89C)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Reason:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp,
                            color = Color(0xFFE57373)
                        )
                        Text(
                            text = suspension.reason,
                            fontSize = 12.sp,
                            color = Color(0xFFF3EFE6),
                            lineHeight = 17.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "Local manuscript editing and Google Drive sync have been locked. To appeal this governance action or request reinstatement, contact the Editor in Chief.",
                    fontSize = 11.5.sp,
                    color = Color(0xFFB0A89C),
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(22.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onRetrySync,
                        border = BorderStroke(1.dp, Color(0xFF444455)),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("btn_check_restoration")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.LightGray)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Check Status", fontSize = 11.5.sp, color = Color.LightGray)
                    }

                    Button(
                        onClick = onSwitchUser,
                        colors = ButtonDefaults.buttonColors(containerColor = BookGoldDark),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("btn_switch_account")
                    ) {
                        Icon(Icons.Default.SwitchAccount, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Switch User", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
