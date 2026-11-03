package com.supernatural.bwriter.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.supernatural.bwriter.model.AiTokenTransaction
import com.supernatural.bwriter.model.SubscriptionPlan
import com.supernatural.bwriter.model.UserAiSubscription
import com.supernatural.bwriter.ui.theme.BookGold
import com.supernatural.bwriter.ui.theme.BookGoldDark
import com.supernatural.bwriter.ui.theme.BookGoldLight
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SubscriptionPaywallDialog(
    subscription: UserAiSubscription,
    recentTransactions: List<AiTokenTransaction> = emptyList(),
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF101018),
            border = BorderStroke(1.dp, BookGoldDark),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFC5A059).copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, BookGold),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.WorkspacePremium,
                                    contentDescription = null,
                                    tint = BookGoldLight,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Bwriter AI Studio Pass",
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color(0xFFF3EFE6)
                            )
                            Text(
                                text = "Monthly Credits & CMOS Token Ledger",
                                fontSize = 11.sp,
                                color = BookGoldLight
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Current Wallet / Quota Card
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF181824),
                    border = BorderStroke(1.dp, Color(0xFF2C2C3C)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "CURRENT PLAN",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.LightGray
                                )
                                Text(
                                    text = subscription.plan.title,
                                    fontFamily = FontFamily.Serif,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color(subscription.plan.badgeColorHex)
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "REMAINING CREDITS",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.LightGray
                                )
                                Text(
                                    text = if (subscription.plan == SubscriptionPlan.SUPERUSER_UNLIMITED) "∞ Unlimited" else "${subscription.creditsRemaining} Generations",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (subscription.creditsRemaining > 0 || subscription.plan == SubscriptionPlan.SUPERUSER_UNLIMITED) BookGoldLight else Color(0xFFFF6B6B)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Stats pill row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Total Tokens Burned: ${"%,d".format(subscription.totalTokensUsed)}",
                                fontSize = 10.5.sp,
                                color = Color(0xFFB0A89C)
                            )
                            Text(
                                text = "Total Runs: ${subscription.totalGenerationsCount}",
                                fontSize = 10.5.sp,
                                color = Color(0xFFB0A89C)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tabs: Plans vs Token Audit Ledger
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color(0xFF14141E),
                    contentColor = BookGoldLight
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Subscription Plans", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Token Ledger (${recentTransactions.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (selectedTab == 0) {
                    // Coming-soon preview of the paid structure
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Coming in the next update.",
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = BookGoldLight
                        )

                        Text(
                            text = "Go ad-free for $2.99, billed through Google Play. AI generations run on prepaid tokens — 20 for $0.99, credited to your account by the Editor in Chief. A token is spent only when you generate. Nothing is charged in this version.",
                            fontSize = 11.5.sp,
                            color = Color(0xFFC8C2B7)
                        )

                        ComingSoonCard(
                            title = "Ad-Free — $2.99",
                            detail = "No ads anywhere in the app."
                        )

                        ComingSoonCard(
                            title = "AI Tokens — 20 for $0.99",
                            detail = "Prepaid generations, credited by the Editor in Chief."
                        )
                    }
                } else {
                    // Token Ledger / Audit Tab
                    if (recentTransactions.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(36.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("No token transactions recorded yet.", color = Color.LightGray, fontSize = 12.sp)
                                Text("Generations will audit prompt & completion tokens here.", color = Color.Gray, fontSize = 10.sp)
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(280.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(recentTransactions) { tx ->
                                TokenTransactionRow(tx)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ComingSoonCard(
    title: String,
    detail: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF14141C)
        ),
        border = BorderStroke(1.dp, Color(0xFF2C2C3A))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color(0xFFF3EFE6)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = detail,
                fontSize = 10.5.sp,
                color = Color(0xFFC8C2B7)
            )
        }
    }
}

@Composable
private fun TokenTransactionRow(tx: AiTokenTransaction) {
    val dateFmt = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
    val formattedTime = dateFmt.format(Date(tx.timestamp))

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF161620),
        border = BorderStroke(0.5.dp, Color(0xFF2A2A38)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tx.sectionTitle.ifBlank { "Chapter Section" },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF3EFE6)
                )
                Text(
                    text = "${tx.modelUsed} • $formattedTime",
                    fontSize = 9.5.sp,
                    color = Color.Gray
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${tx.totalTokens} tokens",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = BookGoldLight
                )
                Text(
                    text = "Prompt: ${tx.promptTokens} | Out: ${tx.completionTokens}",
                    fontSize = 9.sp,
                    color = Color(0xFF9E9EAE)
                )
            }
        }
    }
}
