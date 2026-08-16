package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.ServerlessMailMessage
import com.example.model.UserProfile
import com.example.ui.theme.BookGold
import com.example.ui.theme.BookGoldDark
import com.example.ui.theme.BookGoldLight
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MailboxInboxScreen(
    currentUser: UserProfile,
    messages: List<ServerlessMailMessage>,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onMarkAsRead: (messageId: String) -> Unit
) {
    var selectedMessage by remember { mutableStateOf<ServerlessMailMessage?>(null) }
    val dateFormat = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = BookGoldDark.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, BookGoldDark),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Mail, contentDescription = null, tint = BookGoldLight, modifier = Modifier.size(18.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Editorial Mailbox",
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color(0xFFF3EFE6)
                            )
                            Text(
                                text = "Shared Drive Inbox • ${currentUser.email}",
                                fontSize = 10.5.sp,
                                color = BookGoldLight
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("btn_mailbox_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFFF3EFE6))
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh, modifier = Modifier.testTag("btn_mailbox_refresh")) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = BookGoldLight)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0E0E14),
                    titleContentColor = Color(0xFFF3EFE6)
                )
            )
        },
        containerColor = Color(0xFF0A0A0E)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(14.dp)
        ) {
            val unreadCount = messages.count { !it.isRead }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF14141C),
                border = BorderStroke(1.dp, Color(0xFF2C2C38)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Mailbox Location: /Mailboxes/${currentUser.email}/messages.json",
                        fontSize = 11.sp,
                        color = Color(0xFFB0A89C),
                        fontFamily = FontFamily.Monospace
                    )
                    if (unreadCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = BookGoldDark
                        ) {
                            Text(
                                text = "$unreadCount New",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (messages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.MarkEmailRead, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Your editorial mailbox is empty.",
                            color = Color.Gray,
                            fontSize = 13.sp,
                            fontStyle = FontStyle.Italic
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(messages) { msg ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedMessage = msg
                                    if (!msg.isRead) {
                                        onMarkAsRead(msg.id)
                                    }
                                }
                                .testTag("mailbox_msg_${msg.id}"),
                            colors = CardDefaults.cardColors(
                                containerColor = if (!msg.isRead) Color(0xFF1B1B26) else Color(0xFF14141C)
                            ),
                            border = BorderStroke(1.dp, if (!msg.isRead) BookGoldDark else Color(0xFF2C2C38))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            when (msg.messageType) {
                                                "GOVERNANCE_ALERT" -> Icons.Default.Gavel
                                                "EDITORIAL_REVISION" -> Icons.Default.EditNote
                                                else -> Icons.Default.Mail
                                            },
                                            contentDescription = null,
                                            tint = when (msg.messageType) {
                                                "GOVERNANCE_ALERT" -> Color(0xFFE57373)
                                                "EDITORIAL_REVISION" -> BookGoldLight
                                                else -> Color(0xFF81C784)
                                            },
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = msg.senderName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = BookGoldLight
                                        )
                                    }

                                    Text(
                                        text = try { dateFormat.format(Date(msg.timestamp)) } catch (e: Exception) { "" },
                                        fontSize = 10.sp,
                                        color = Color(0xFF7E7E8E)
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = msg.subject,
                                    fontFamily = FontFamily.Serif,
                                    fontWeight = if (!msg.isRead) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 14.sp,
                                    color = Color(0xFFF3EFE6)
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = msg.body,
                                    fontSize = 12.sp,
                                    color = Color(0xFFB0A89C),
                                    maxLines = 2,
                                    lineHeight = 16.sp
                                )

                                if (msg.manuscriptTitle != null) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.AutoStories, contentDescription = null, tint = BookGoldLight, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Ref: ${msg.manuscriptTitle}",
                                            fontSize = 10.5.sp,
                                            color = BookGoldLight,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Message Detail Dialog
    selectedMessage?.let { msg ->
        Dialog(onDismissRequest = { selectedMessage = null }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF14141C),
                border = BorderStroke(1.dp, BookGoldDark)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = msg.subject,
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color(0xFFF3EFE6)
                            )
                            Text(
                                text = "From: ${msg.senderName} (${msg.senderEmail})",
                                fontSize = 11.sp,
                                color = BookGoldLight
                            )
                        }
                        IconButton(onClick = { selectedMessage = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF0E0E14),
                        border = BorderStroke(1.dp, Color(0xFF2C2C38)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = msg.body,
                            fontSize = 12.5.sp,
                            color = Color(0xFFE2DDD5),
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(14.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { selectedMessage = null },
                        colors = ButtonDefaults.buttonColors(containerColor = BookGoldDark),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Close Message", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
