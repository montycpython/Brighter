package com.supernatural.bwriter.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.supernatural.bwriter.auth.GoogleAuthHelper
import com.supernatural.bwriter.auth.FirebaseAuthBridge
import com.supernatural.bwriter.model.UserProfile
import com.supernatural.bwriter.model.WorkRole
import com.supernatural.bwriter.ui.components.RoleBadge
import com.supernatural.bwriter.ui.theme.BookGold
import com.supernatural.bwriter.ui.theme.BookGoldDark
import com.supernatural.bwriter.ui.theme.BookGoldLight
import kotlinx.coroutines.launch
import java.util.Calendar

@Composable
fun AuthScreen(
    currentUser: UserProfile,
    savedAccounts: List<UserProfile> = emptyList(),
    onGoogleSignIn: (email: String, name: String, penName: String, role: WorkRole) -> Unit,
    onRemoveSavedAccount: (String) -> Unit = {},
    onContinue: () -> Unit = {}
) {
    var selectedEmail by remember { mutableStateOf(currentUser.email) }
    var authorName by remember { mutableStateOf(currentUser.name) }
    var penName by remember { mutableStateOf(currentUser.penName) }
    var sessionEmail by remember { mutableStateOf<String?>(null) }
    val sessionIsEditorInChief = com.supernatural.bwriter.data.StudioStanding.isEditorInChief
    val selectedRole = if (sessionIsEditorInChief) WorkRole.EDITOR else WorkRole.AUTHOR
    var isAuthenticatingGoogle by remember { mutableStateOf(false) }
    var googleAuthError by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    val effectiveDisplayName = when {
        penName.isNotBlank() -> penName.trim()
        authorName.isNotBlank() -> authorName.trim()
        selectedEmail.contains("@") -> selectedEmail.substringBefore("@").replace(".", " ").capitalizeWords()
        else -> ""
    }

    val scrollState = rememberScrollState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(BookGoldDark),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "B",
                    fontSize = 38.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Sign in with Google",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "Choose a Google account to enter Bwriter Studio",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("google_account_picker_card"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val displayAccounts = savedAccounts
                    if (displayAccounts.isNotEmpty()) {
                        Text(
                            text = "SELECT GOOGLE ACCOUNT",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = BookGoldLight,
                            letterSpacing = 1.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                    }
                    displayAccounts.forEach { account ->
                        val isSelected = selectedEmail.equals(account.email, ignoreCase = true)

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = BorderStroke(
                                if (isSelected) 2.dp else 1.dp,
                                if (isSelected) BookGold else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    selectedEmail = account.email
                                    sessionEmail = account.email
                                    authorName = account.name
                                    penName = account.penName
                                }
                                .testTag("account_item_${account.email}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = (account.displayName.take(1).ifBlank { account.email.take(1) }).uppercase(),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = account.displayName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }
                                    Text(
                                        text = account.email,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = BookGold,
                                        modifier = Modifier.size(22.dp)
                                    )
                                } else if (displayAccounts.size > 1) {
                                    IconButton(
                                        onClick = { onRemoveSavedAccount(account.email) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Remove Account",
                                            tint = Color.Gray,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            isAuthenticatingGoogle = true
                            googleAuthError = null
                            coroutineScope.launch {
                                val result = GoogleAuthHelper.signInWithGoogle(context)
                                isAuthenticatingGoogle = false
                                result.onSuccess { googleUser ->
                                    if (googleUser.displayName.isNotBlank()) {
                                        authorName = googleUser.displayName
                                    }
                                    selectedEmail = googleUser.email
                                    sessionEmail = googleUser.email
                                    val bridgeResult = FirebaseAuthBridge.signInWithGoogleIdToken(googleUser.idToken)
                                    if (bridgeResult.isSuccess) {
                                        val standingRole = WorkRole.AUTHOR
                                        onGoogleSignIn(googleUser.email, authorName, penName, standingRole)
                                        onContinue()
                                    } else {
                                        googleAuthError = bridgeResult.exceptionOrNull()?.localizedMessage ?: "Sign-in could not be completed. Please try again."
                                    }
                                }.onFailure { err ->
                                    if (err !is androidx.credentials.exceptions.GetCredentialCancellationException) {
                                        googleAuthError = err.localizedMessage ?: "Google Identity sign-in failed. Please try again."
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BookGoldDark),
                        shape = RoundedCornerShape(8.dp),
                        enabled = !isAuthenticatingGoogle,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_native_google_identity")
                    ) {
                        if (isAuthenticatingGoogle) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Verifying...", fontSize = 12.sp)
                        } else {
                            Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sign In with Google", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (googleAuthError != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = googleAuthError!!,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF161622)
                ),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, BookGoldDark.copy(alpha = 0.7f))
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
                        RoleBadge(role = selectedRole, isEditorInChief = sessionIsEditorInChief)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Author Byline: ", fontSize = 12.5.sp, color = Color.Gray)
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
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Copyright Line: © $currentYear ${if (penName.isNotBlank()) penName else authorName}. All rights reserved.",
                        fontSize = 11.sp,
                        color = Color(0xFFB0A898),
                        fontFamily = FontFamily.Serif
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("google_auth_card"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "AUTHOR PROFILE & PEN NAME (OPTIONAL)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = penName,
                        onValueChange = { penName = it },
                        label = { Text("Pen Name / Author Byline (Optional)") },
                        placeholder = { Text("e.g. J. D. Cross, George Orwell") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.DriveFileRenameOutline,
                                contentDescription = "Pen Name",
                                tint = BookGoldDark
                            )
                        },
                        supportingText = {
                            Text("Used on Book Covers, Title Pages, Running Heads, and Directory listings.")
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_pen_name")
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = authorName,
                        onValueChange = { authorName = it },
                        label = { Text("Author / Legal Name") },
                        placeholder = { Text("e.g. Jane Doe") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Legal Name",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        supportingText = {
                            Text("Your real name for copyright records and administrative documentation.")
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_author_name")
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Spacer(modifier = Modifier.height(24.dp))

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}


private fun String.capitalizeWords(): String {
    return split(" ").joinToString(" ") { it.replaceFirstChar { char -> if (char.isLowerCase()) char.titlecase() else char.toString() } }
}
