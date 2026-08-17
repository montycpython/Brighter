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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.AiTokenTransaction
import com.example.model.SubscriptionPlan
import com.example.model.UserAiSubscription
import com.example.ui.theme.BookGold
import com.example.ui.theme.BookGoldDark
import com.example.ui.theme.BookGoldLight
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SubscriptionPaywallDialog(
    subscription: UserAiSubscription,
    recentTransactions: List<AiTokenTransaction> = emptyList(),
    onDismiss: () -> Unit,
    onSelectPlan: (SubscriptionPlan) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var chosenPlan by remember { mutableStateOf(SubscriptionPlan.STANDARD_AUTHOR) }
    var isProcessingPurchase by remember { mutableStateOf(false) }

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
                    // Plans Selection Column
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Standard Plan Card
                        PlanOptionCard(
                            plan = SubscriptionPlan.STANDARD_AUTHOR,
                            isSelected = chosenPlan == SubscriptionPlan.STANDARD_AUTHOR,
                            badge = "POPULAR FOR NOVELISTS",
                            features = listOf(
                                "500 AI Chapter Generations / month",
                                "~500,000 CMOS formatted tokens",
                                "Gemini 2.5 Flash Prose Synthesis",
                                "Character & Setting context injection",
                                "Chicago Manual §6, §13 punctuation adherence"
                            ),
                            onClick = { chosenPlan = SubscriptionPlan.STANDARD_AUTHOR }
                        )

                        // Pro Plan Card
                        PlanOptionCard(
                            plan = SubscriptionPlan.PRO_IMPRINT,
                            isSelected = chosenPlan == SubscriptionPlan.PRO_IMPRINT,
                            badge = "BEST FOR PUBLISHERS & STUDIOS",
                            features = listOf(
                                "2,500 AI Chapter Generations / month",
                                "~2,500,000 CMOS formatted tokens",
                                "Gemini 2.5 Pro Priority Access",
                                "Multi-character dialogue & dialect tuning",
                                "Unlimited Intertextuality Masterwork analysis",
                                "Decentralized Google Drive Master Sync"
                            ),
                            onClick = { chosenPlan = SubscriptionPlan.PRO_IMPRINT }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Purchase / Upgrade Action Button
                    Button(
                        onClick = {
                            isProcessingPurchase = true
                            onSelectPlan(chosenPlan)
                        },
                        enabled = !isProcessingPurchase,
                        colors = ButtonDefaults.buttonColors(containerColor = BookGoldDark),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("btn_confirm_subscription")
                    ) {
                        if (isProcessingPurchase) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Processing via Google Play...", fontSize = 12.sp)
                        } else {
                            Icon(Icons.Default.MonetizationOn, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Subscribe ${chosenPlan.title} (${chosenPlan.priceMonthly})",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Google Play In-App Subscription • Billed monthly • Cancel anytime in Google Play Store.",
                        fontSize = 10.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
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
private fun PlanOptionCard(
    plan: SubscriptionPlan,
    isSelected: Boolean,
    badge: String,
    features: List<String>,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("plan_card_${plan.name}"),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF1E1E2C) else Color(0xFF14141C)
        ),
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) BookGold else Color(0xFF2C2C3A)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = BookGoldDark.copy(alpha = 0.25f),
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    Text(
                        text = badge,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = BookGoldLight,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Text(
                    text = plan.priceMonthly,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = BookGoldLight
                )
            }

            Text(
                text = plan.title,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color(0xFFF3EFE6)
            )

            Spacer(modifier = Modifier.height(6.dp))

            features.forEach { feature ->
                Row(
                    modifier = Modifier.padding(vertical = 1.5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = BookGold, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = feature, fontSize = 10.5.sp, color = Color(0xFFC8C2B7))
                }
            }
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
