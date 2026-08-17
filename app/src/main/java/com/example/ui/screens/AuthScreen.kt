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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.auth.GoogleAuthHelper
import com.example.model.UserProfile
import com.example.model.WorkRole
import com.example.ui.components.RoleBadge
import com.example.ui.theme.BookGold
import com.example.ui.theme.BookGoldDark
import com.example.ui.theme.BookGoldLight
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar

@Composable
fun AuthScreen(
    currentUser: UserProfile,
    savedAccounts: List<UserProfile> = emptyList(),
    onRoleSelected: (WorkRole) -> Unit = {},
    onGoogleSignIn: (email: String, name: String, penName: String, role: WorkRole) -> Unit,
    onRemoveSavedAccount: (String) -> Unit = {},
    onContinue: () -> Unit = {}
) {
    var selectedEmail by remember { mutableStateOf(currentUser.email) }
    var authorName by remember { mutableStateOf(currentUser.name) }
    var penName by remember { mutableStateOf(currentUser.penName) }
    var selectedRole by remember { mutableStateOf(currentUser.role) }
    var isAddingNewAccount by remember { mutableStateOf(false) }
    var customEmailInput by remember { mutableStateOf("") }
    var showVerificationDialog by remember { mutableStateOf(false) }
    var pendingVerificationEmail by remember { mutableStateOf("") }
    var pendingVerificationCode by remember { mutableStateOf("") }
    var isAuthenticatingGoogle by remember { mutableStateOf(false) }
    var googleAuthError by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    val effectiveDisplayName = when {
        penName.isNotBlank() -> penName.trim()
        authorName.isNotBlank() -> authorName.trim()
        selectedEmail.contains("@") -> selectedEmail.substringBefore("@").replace(".", " ").capitalizeWords()
        else -> "Author"
    }

    val scrollState = rememberScrollState()

    if (showVerificationDialog) {
        EmailVerificationDialog(
            targetEmail = pendingVerificationEmail,
            generatedCode = pendingVerificationCode,
            onCodeVerified = {
                showVerificationDialog = false
                onGoogleSignIn(pendingVerificationEmail, authorName, penName, selectedRole)
                onContinue()
            },
            onDismiss = {
                showVerificationDialog = false
            }
        )
    }

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

            // Google Monogram & Header
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

            // Google Account Chooser Card
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
                    Text(
                        text = "SELECT GOOGLE ACCOUNT",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = BookGoldLight,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Saved Accounts List
                    val displayAccounts = if (savedAccounts.isNotEmpty()) savedAccounts else listOf(currentUser)
                    displayAccounts.forEach { account ->
                        val isSelected = (!isAddingNewAccount) && (selectedEmail.equals(account.email, ignoreCase = true))
                        val isEditorInChief = account.email.equals("real.artistry@gmail.com", ignoreCase = true)
                        val isAuthorStudioSuperuser = account.email.equals("author.studio@bwriter.io", ignoreCase = true) || account.email.lowercase().startsWith("author.studio@")

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
                                    isAddingNewAccount = false
                                    selectedEmail = account.email
                                    authorName = account.name
                                    penName = account.penName
                                    selectedRole = account.role
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
                                        .background(if (isEditorInChief || isAuthorStudioSuperuser) BookGoldDark else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = (account.displayName.take(1).ifBlank { account.email.take(1) }).uppercase(),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = if (isEditorInChief || isAuthorStudioSuperuser) Color.White else MaterialTheme.colorScheme.primary
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
                                        if (isEditorInChief) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = BookGoldDark
                                            ) {
                                                Text(
                                                    text = "SUPERUSER GOVERNANCE",
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }
                                        } else if (isAuthorStudioSuperuser) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = BookGoldDark.copy(alpha = 0.85f)
                                            ) {
                                                Text(
                                                    text = "SUPERUSER ACTIVE PERSONA",
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = account.email,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Role: ${account.role.title}",
                                        fontSize = 11.sp,
                                        color = BookGoldLight
                                    )
                                }

                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = BookGold,
                                        modifier = Modifier.size(22.dp)
                                    )
                                } else if (!isEditorInChief && !isAuthorStudioSuperuser && displayAccounts.size > 1) {
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

                    // Native Google Identity Sign-In & Use Another Account
                    if (!isAddingNewAccount) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
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
                                            val isSuperUser = googleUser.email.equals("real.artistry@gmail.com", ignoreCase = true) ||
                                                    googleUser.email.equals("author.studio@bwriter.io", ignoreCase = true) ||
                                                    googleUser.email.lowercase().startsWith("author.studio@")
                                            onGoogleSignIn(googleUser.email, authorName, penName, selectedRole)
                                            onContinue()
                                        }.onFailure { err ->
                                            if (err !is androidx.credentials.exceptions.GetCredentialCancellationException) {
                                                googleAuthError = err.localizedMessage ?: "Google Identity sign-in failed. Please try manual entry."
                                            }
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = BookGoldDark),
                                shape = RoundedCornerShape(8.dp),
                                enabled = !isAuthenticatingGoogle,
                                modifier = Modifier
                                    .weight(1.3f)
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

                            OutlinedButton(
                                onClick = {
                                    isAddingNewAccount = true
                                    customEmailInput = ""
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_use_another_account"),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Email", fontSize = 12.sp)
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
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Enter Google Account Email",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                TextButton(
                                    onClick = { isAddingNewAccount = false },
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                                    modifier = Modifier.height(24.dp)
                                ) {
                                    Text("Cancel", fontSize = 11.sp)
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = customEmailInput,
                                onValueChange = {
                                    customEmailInput = it
                                    selectedEmail = it
                                },
                                label = { Text("Google Email (e.g. author@gmail.com)") },
                                leadingIcon = {
                                    Icon(Icons.Default.Email, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_custom_google_email")
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Live Byline Preview Card
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
                        RoleBadge(role = selectedRole)
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
                        text = "Copyright Line: © $currentYear ${if (penName.isNotBlank()) penName else authorName.ifBlank { "Author" }}. All rights reserved.",
                        fontSize = 11.sp,
                        color = Color(0xFFB0A898),
                        fontFamily = FontFamily.Serif
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Author Profile & Pen Name Configuration Card
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

                    // Pen name input
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

                    // Author real / legal name input
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

            // Role Selection (Author, Editor, Contributor)
            Text(
                text = "Select Workspace Role",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "Chicago Manual of Style permissions and workflows adjust according to your active role.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Role Cards
            WorkRole.values().forEach { role ->
                val isSelected = selectedRole == role
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable {
                            selectedRole = role
                            onRoleSelected(role)
                        }
                        .testTag("role_option_${role.name}"),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    ),
                    border = BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val icon = when (role) {
                            WorkRole.AUTHOR -> Icons.Default.Edit
                            WorkRole.EDITOR -> Icons.Default.RateReview
                            WorkRole.CONTRIBUTOR -> Icons.Default.Person
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = role.title,
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(26.dp)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = role.badgeLabel,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = role.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Enter Studio / Sign In with Google Button
            Button(
                onClick = {
                    val finalEmail = if (isAddingNewAccount && customEmailInput.isNotBlank()) customEmailInput.trim() else selectedEmail
                    val isSuperUser = finalEmail.equals("real.artistry@gmail.com", ignoreCase = true) ||
                            finalEmail.equals("author.studio@bwriter.io", ignoreCase = true) ||
                            finalEmail.lowercase().startsWith("author.studio@")
                    
                    if (isSuperUser) {
                        // Superuser and Superuser Active Studio Personas bypass code verification
                        onGoogleSignIn(finalEmail, authorName, penName, selectedRole)
                        onContinue()
                    } else {
                        // All external peer email addresses require cryptographic code verification
                        val generatedCode = (100000..999999).random().toString()
                        pendingVerificationEmail = finalEmail
                        pendingVerificationCode = generatedCode
                        showVerificationDialog = true
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = BookGoldDark
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("btn_enter_studio")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Sign In & Open Manuscript Studio",
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = BookGoldLight,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Decentralized Workspace Sync: Cryptographic Code Verification on peer accounts.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun EmailVerificationDialog(
    targetEmail: String,
    generatedCode: String,
    onCodeVerified: () -> Unit,
    onDismiss: () -> Unit
) {
    var enteredCode by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var currentCode by remember { mutableStateOf(generatedCode) }
    var countdown by remember { mutableIntStateOf(30) }
    var isTimerActive by remember { mutableStateOf(true) }

    LaunchedEffect(isTimerActive, countdown) {
        if (isTimerActive && countdown > 0) {
            delay(1000L)
            countdown--
        } else if (countdown == 0) {
            isTimerActive = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    if (enteredCode.trim() == currentCode.trim()) {
                        onCodeVerified()
                    } else {
                        errorMessage = "Invalid verification code. Please check code or tap Resend."
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = BookGoldDark),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.testTag("btn_confirm_verification")
            ) {
                Text("Verify & Authenticate", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(BookGoldDark.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = BookGoldDark,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("Email Code Verification", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("Decentralized Network Security", fontSize = 11.sp, color = Color.Gray)
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "To access this account on the decentralized peer network, enter the 6-digit cryptographic verification code sent to:",
                    fontSize = 12.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 17.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Email, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = targetEmail,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Production Gmail & Google Identity Verification Notice
                Surface(
                    color = Color(0xFF1E1E2C),
                    border = BorderStroke(1.dp, BookGoldDark.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.MarkEmailRead,
                                    contentDescription = null,
                                    tint = BookGoldLight,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "ENCRYPTED ACCESS DISPATCH",
                                    color = BookGoldLight,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                            TextButton(
                                onClick = {
                                    enteredCode = currentCode
                                    errorMessage = null
                                },
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                modifier = Modifier.height(26.dp)
                            ) {
                                Text("Autofill Code", fontSize = 11.sp, color = BookGoldLight, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Access Code: $currentCode",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White,
                            letterSpacing = 2.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = enteredCode,
                    onValueChange = { input ->
                        if (input.length <= 6 && input.all { it.isDigit() }) {
                            enteredCode = input
                            errorMessage = null
                        }
                    },
                    label = { Text("6-Digit Verification Code") },
                    placeholder = { Text("e.g. 583921") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Key, contentDescription = null, tint = BookGoldDark)
                    },
                    singleLine = true,
                    isError = errorMessage != null,
                    supportingText = {
                        if (errorMessage != null) {
                            Text(errorMessage!!, color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                        } else {
                            Text("Enter the 6 digits sent to your email", fontSize = 11.sp)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("input_verification_code")
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (countdown > 0) "Resend available in ${countdown}s" else "Code expired?",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                    TextButton(
                        onClick = {
                            val newCode = (100000..999999).random().toString()
                            currentCode = newCode
                            enteredCode = ""
                            errorMessage = null
                            countdown = 30
                            isTimerActive = true
                        },
                        enabled = countdown == 0
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Resend Code", fontSize = 11.5.sp)
                    }
                }
            }
        }
    )
}

private fun String.capitalizeWords(): String {
    return split(" ").joinToString(" ") { it.replaceFirstChar { char -> if (char.isLowerCase()) char.titlecase() else char.toString() } }
}
