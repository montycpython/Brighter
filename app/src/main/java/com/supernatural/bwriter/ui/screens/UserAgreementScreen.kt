package com.supernatural.bwriter.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.supernatural.bwriter.ui.theme.BookGold
import com.supernatural.bwriter.ui.theme.BookGoldDark
import com.supernatural.bwriter.ui.theme.BookGoldLight

const val CURRENT_TERMS_VERSION = "0.1.2"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserAgreementScreen(
    isMandatoryOnboarding: Boolean = false,
    onAccept: () -> Unit,
    onDecline: (() -> Unit)? = null
) {
    var agreedToTerms by remember { mutableStateOf(false) }
    var agreedToPrivacyAndAi by remember { mutableStateOf(false) }

    val canProceed = agreedToTerms && agreedToPrivacyAndAi
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = BookGoldDark.copy(alpha = 0.25f),
                            border = BorderStroke(1.dp, BookGold),
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.VerifiedUser,
                                    contentDescription = null,
                                    tint = BookGoldLight,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Bwriter User Agreement",
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color(0xFFF3EFE6)
                            )
                            Text(
                                text = "Terms of Service, Privacy Policy & Governance • v$CURRENT_TERMS_VERSION",
                                fontSize = 10.sp,
                                color = BookGoldLight
                            )
                        }
                    }
                },
                navigationIcon = {
                    if (!isMandatoryOnboarding && onDecline != null) {
                        IconButton(onClick = onDecline) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFFF3EFE6))
                        }
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
                .padding(horizontal = 18.dp, vertical = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
            ) {
                Spacer(modifier = Modifier.height(6.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF14141C),
                    border = BorderStroke(1.dp, BookGoldDark.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Welcome to Bwriter Press",
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = Color(0xFFF3EFE6)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Please review and accept our End User Agreement before publishing, typesetting, or syncing manuscripts with the Bwriter studio.",
                            fontSize = 12.sp,
                            color = Color(0xFFC8C2B7),
                            lineHeight = 17.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                AgreementSectionCard(
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    title = "1. Intellectual Property & Manuscript Ownership",
                    content = """
                    • You retain 100% of the copyright, intellectual property rights, and commercial ownership of all original text, titles, character rosters, world settings, and published works created in Bwriter.
                    • Bwriter does not claim any royalty, licensing rights, or ownership stake in your manuscripts or exported PDF print files.
                    • By choosing to sync a manuscript as Public, you publish its full text to the community registry, where other signed-in Bwriter users can read the whole book. Choosing Private keeps the book out of the community catalog.
                    """.trimIndent()
                )

                Spacer(modifier = Modifier.height(12.dp))

                AgreementSectionCard(
                    icon = Icons.Default.CloudDone,
                    title = "2. Data Privacy & Storage",
                    content = """
                    • Local-First Storage: Your drafts, editorial comments, and character notes are stored on your device in the app's local database.
                    • Cloud Copies: When you sync a manuscript, the complete book is stored in the Bwriter back room (Google Firebase) under your account, and a copy is saved to your own Google Drive, in a folder named Bwriter. These copies are how your books are restored when you reinstall the app or change devices.
                    • Private vs Public Visibility: A Private book is not shown in the community catalog, and its text can be read only by you and the Editor-in-Chief. A Public book's full text can be read by any signed-in Bwriter user.
                    • Account Data: Signing in provides your name and email address, used to identify your books, your mail, and your community access.
                    """.trimIndent()
                )

                Spacer(modifier = Modifier.height(12.dp))

                AgreementSectionCard(
                    icon = Icons.Default.AutoAwesome,
                    title = "3. Generative AI Prose Assistance",
                    content = """
                    • Assistive Drafting: Gemini AI prose generation is provided as a creative writing assistant. You are responsible for reviewing, verifying, and editing all AI-suggested text.
                    • CMOS Typographic Standards: AI generation is formatted to adhere to the Chicago Manual of Style (17th Edition) for dialogue punctuation, paragraph indents, and block quotes.
                    • Content Policy: You agree not to generate hate speech, harassment, malware instructions, or illegal material through the AI drafting tools. In this version, drafting assistance is provided by the app's built-in engine; Gemini-powered generation arrives in the next update.
                    """.trimIndent()
                )

                Spacer(modifier = Modifier.height(12.dp))

                AgreementSectionCard(
                    icon = Icons.Default.Gavel,
                    title = "4. Community Governance & Kill-Switch Policy",
                    content = """
                    • Code of Conduct: Users participating in the community book registry and editorial mailbox must maintain civil, professional literary discourse.
                    • Administrative Suspensions: The Editor-in-Chief reserves the right to suspend community access for accounts engaging in abusive conduct, spamming, or intellectual property infringement.
                    • Account Restoration: While an account is suspended, access to the app is paused; drafts remain stored on the device and in the writer's own Google Drive, and governance actions may be appealed directly to the Editor-in-Chief.
                    """.trimIndent()
                )

                Spacer(modifier = Modifier.height(12.dp))

                AgreementSectionCard(
                    icon = Icons.Default.CloudDone,
                    title = "5. Advertising",
                    content = """
                    • Ads: Bwriter shows banner ads provided by Google AdMob. Google may use your device's advertising ID to serve and measure ads, as described in Google's privacy policy.
                    • Ad-Free Choice: You may watch a full rewarded ad to remove banner ads for 24 hours. The ad-free period is stored only on your device.
                    """.trimIndent()
                )

                Spacer(modifier = Modifier.height(12.dp))

                AgreementSectionCard(
                    icon = Icons.Default.CloudDone,
                    title = "6. In-App Credit",
                    content = """
                    Bwriter includes an in-app credit system. Credit is earned when a book is approved for the registry, and editors of record on a book are issued the same amount as its author at the moment the book is approved. Amounts follow a schedule that decreases as an author accumulates approved books. Claiming earned credit requires a proof-of-work computation performed by your own device.

                    Credit is recorded using cryptographic signatures and hash-linked records, technology adapted from a separate ledger system. Credit is not Soul Bank, Soul Entrepreneur, or any other product. It is not a cryptocurrency, not money, and holds no cash value; it may be sent to other authors inside this app; it cannot be sold or redeemed outside this app. A future version may develop this system into a real cryptocurrency; this version makes no such promise.

                    Each person may hold exactly one account. Your author key is created and kept on your device. An optional backup, sealed with a passphrase known only to you, may be stored in your own Google Drive; the passphrase is never collected or stored by the app. If you lose both your device and your passphrase, access to your credit identity cannot be restored.
                    """.trimIndent()
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF14141C),
                border = BorderStroke(1.dp, Color(0xFF2C2C38)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("checkbox_agree_terms"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = agreedToTerms,
                            onCheckedChange = { agreedToTerms = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = BookGold,
                                uncheckedColor = Color(0xFF7E7E8E)
                            )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "I have read and agree to the Terms of Service, EULA, and Intellectual Property Guidelines.",
                            fontSize = 11.5.sp,
                            color = Color(0xFFE2DDD5),
                            lineHeight = 15.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("checkbox_agree_privacy"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = agreedToPrivacyAndAi,
                            onCheckedChange = { agreedToPrivacyAndAi = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = BookGold,
                                uncheckedColor = Color(0xFF7E7E8E)
                            )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "I consent to the Privacy Policy, cloud storage, and AI Assistive Prose terms.",
                            fontSize = 11.5.sp,
                            color = Color(0xFFE2DDD5),
                            lineHeight = 15.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (onDecline != null && !isMandatoryOnboarding) {
                            OutlinedButton(
                                onClick = onDecline,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Cancel", color = Color.LightGray, fontSize = 12.sp)
                            }
                        }

                        Button(
                            onClick = onAccept,
                            enabled = canProceed,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BookGoldDark,
                                disabledContainerColor = Color(0xFF2C2C38)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(if (onDecline != null && !isMandatoryOnboarding) 1.5f else 1f)
                                .height(46.dp)
                                .testTag("btn_accept_user_agreement")
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Accept & Enter Studio",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AgreementSectionCard(
    icon: ImageVector,
    title: String,
    content: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF14141C)),
        border = BorderStroke(1.dp, Color(0xFF2C2C38))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = BookGoldDark.copy(alpha = 0.2f),
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = BookGoldLight, modifier = Modifier.size(15.dp))
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    fontSize = 13.5.sp,
                    color = Color(0xFFF3EFE6)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = content,
                fontSize = 11.5.sp,
                color = Color(0xFFC8C2B7),
                lineHeight = 16.5.sp
            )
        }
    }
}
