package com.supernatural.bwriter.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.supernatural.bwriter.data.PayoutMethods
import com.supernatural.bwriter.data.PayoutRequest
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
    creditBalance: Int = 0,
    unclaimedCredit: Int = 0,
    claimFailed: Boolean = false,
    onClaimCredit: () -> Unit = {},
    onSendCredit: (String, Int) -> Unit = { _, _ -> },
    adFreePrice: String = "",
    tokenPackPrice: String = "",
    tokenBulkPrice: String = "",
    onBuyAdFree: () -> Unit = {},
    onBuyTokens: () -> Unit = {},
    onBuyTokens100: () -> Unit = {},
    payoutValueCents: Long = 0L,
    myPayoutRequests: List<PayoutRequest> = emptyList(),
    onRequestPayout: (String, String) -> Unit = { _, _ -> },
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

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "In-app credit: $creditBalance",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = BookGoldLight
                            )
                            if (unclaimedCredit > 0) {
                                Text(
                                    text = if (claimFailed) "Mining did not finish \u2014 tap to retry" else "Unclaimed: $unclaimedCredit \u2014 tap to mine",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BookGoldLight,
                                    modifier = Modifier.clickable { onClaimCredit() }
                                )
                            }
                        }

                        var showSendForm by remember { mutableStateOf(false) }
                        var sendTo by remember { mutableStateOf("") }
                        var sendAmount by remember { mutableStateOf("") }
                        var showSendConfirm by remember { mutableStateOf(false) }
                        val sendAmountValue = sendAmount.toIntOrNull()
                        val canSend = sendTo.contains("@") && sendAmountValue != null && sendAmountValue in 1..creditBalance

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Send credit",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = BookGoldLight,
                            modifier = Modifier.clickable { showSendForm = !showSendForm }
                        )
                        if (showSendForm) {
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = sendTo,
                                onValueChange = { sendTo = it },
                                label = { Text("Recipient email", fontSize = 11.sp) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = sendAmount,
                                onValueChange = { sendAmount = it.filter { ch -> ch.isDigit() } },
                                label = { Text("Amount", fontSize = 11.sp) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedButton(
                                onClick = { showSendConfirm = true },
                                enabled = canSend,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Review Send", fontSize = 11.sp)
                            }
                        }
                        if (showSendConfirm) {
                            Dialog(onDismissRequest = { showSendConfirm = false }) {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color(0xFF1C1C26),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(18.dp)) {
                                        Text(
                                            text = "Send $sendAmountValue credits?",
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Serif,
                                            fontSize = 15.sp,
                                            color = BookGoldLight
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "$sendAmountValue credits will move from your claimed balance to $sendTo. This cannot be undone.",
                                            fontSize = 12.5.sp,
                                            color = Color(0xFFC8C2B7)
                                        )
                                        Spacer(modifier = Modifier.height(14.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            OutlinedButton(
                                                onClick = { showSendConfirm = false },
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("Cancel", color = Color.LightGray)
                                            }
                                            OutlinedButton(
                                                onClick = {
                                                    val amountToSend = sendAmountValue ?: 0
                                                    showSendConfirm = false
                                                    if (amountToSend > 0) {
                                                        onSendCredit(sendTo.trim(), amountToSend)
                                                        sendTo = ""
                                                        sendAmount = ""
                                                        showSendForm = false
                                                    }
                                                },
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("Send", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if (payoutValueCents > 0L) {
                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = Color(0xFF2C2C3C))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Payout",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = BookGoldLight
                            )
                            Text(
                                text = "Share value: ${formatCents(payoutValueCents)}",
                                fontSize = 10.5.sp,
                                color = Color(0xFFB0A89C)
                            )
                            var payoutDestination by remember { mutableStateOf("") }
                            var payoutMethod by remember { mutableStateOf(PayoutMethods.PAYPAL) }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    text = "PayPal",
                                    fontSize = 11.sp,
                                    fontWeight = if (payoutMethod == PayoutMethods.PAYPAL) FontWeight.Bold else FontWeight.Normal,
                                    color = if (payoutMethod == PayoutMethods.PAYPAL) BookGoldLight else Color(0xFFB0A89C),
                                    modifier = Modifier.clickable { payoutMethod = PayoutMethods.PAYPAL }
                                )
                                Text(
                                    text = "Stripe",
                                    fontSize = 11.sp,
                                    fontWeight = if (payoutMethod == PayoutMethods.STRIPE) FontWeight.Bold else FontWeight.Normal,
                                    color = if (payoutMethod == PayoutMethods.STRIPE) BookGoldLight else Color(0xFFB0A89C),
                                    modifier = Modifier.clickable { payoutMethod = PayoutMethods.STRIPE }
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = payoutDestination,
                                onValueChange = { payoutDestination = it },
                                label = { Text("Payout destination", fontSize = 11.sp) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedButton(
                                onClick = {
                                    onRequestPayout(payoutMethod, payoutDestination.trim())
                                    payoutDestination = ""
                                },
                                enabled = payoutDestination.isNotBlank(),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Request Payout", fontSize = 11.sp)
                            }
                            myPayoutRequests.forEach { request ->
                                Text(
                                    text = "${request.method} \u00b7 ${formatCents(request.amountCents)} \u00b7 ${request.status}",
                                    fontSize = 10.5.sp,
                                    color = Color(0xFFB0A89C)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

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
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Billed through Google Play.",
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = BookGoldLight
                        )

                        Text(
                            text = "Go ad-free, billed through Google Play. AI generations run on prepaid tokens, credited to your account when your purchase lands. A token is spent only when you generate.",
                            fontSize = 11.5.sp,
                            color = Color(0xFFC8C2B7)
                        )

                        val tokenBalanceLabel = if (subscription.plan == SubscriptionPlan.SUPERUSER_UNLIMITED) {
                            "Balance: unlimited"
                        } else {
                            "Balance: ${subscription.creditsRemaining} generations"
                        }

                        PurchaseCard(
                            title = "Ad-Free",
                            detail = "No ads anywhere in the app.",
                            price = adFreePrice.ifBlank { "$2.99" },
                            onBuy = onBuyAdFree
                        )

                        PurchaseCard(
                            title = "AI Tokens — 20",
                            detail = "Prepaid generations. $tokenBalanceLabel",
                            price = tokenPackPrice.ifBlank { "$0.99" },
                            onBuy = onBuyTokens
                        )

                        PurchaseCard(
                            title = "AI Tokens — 100",
                            detail = "Prepaid generations, bulk pack. $tokenBalanceLabel",
                            price = tokenBulkPrice.ifBlank { "$3.99" },
                            onBuy = onBuyTokens100
                        )
                    }
                } else {
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
private fun PurchaseCard(
    title: String,
    detail: String,
    price: String,
    onBuy: () -> Unit
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

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onBuy,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Buy — $price", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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

private fun formatCents(cents: Long): String {
    val sign = if (cents < 0L) "-" else ""
    val abs = kotlin.math.abs(cents)
    return "${sign}\$${abs / 100}.${(abs % 100).toString().padStart(2, '0')}"
}
