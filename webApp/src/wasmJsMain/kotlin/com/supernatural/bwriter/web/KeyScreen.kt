package com.supernatural.bwriter.web

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.supernatural.bwriter.crypto.Secp256k1
import com.supernatural.bwriter.crypto.hexToBytes

/**
 * The author key screen: create, import, unlock and export the key that
 * signs the account's books. The key exists only inside the account's
 * sign-in, sealed under a passphrase in the Android backup format.
 */
@Composable
fun KeyScreen(email: String, onBack: () -> Unit) {
  var hasKey by remember(email) { mutableStateOf(KeyVault.sealed(email) != null) }
  var address by remember(email) { mutableStateOf(KeyVault.storedAddress(email)) }
  var unlockedHex by remember { mutableStateOf("") }
  var busy by remember { mutableStateOf(false) }
  var notice by remember { mutableStateOf("") }
  var createPass by remember { mutableStateOf("") }
  var createPass2 by remember { mutableStateOf("") }
  var unlockPass by remember { mutableStateOf("") }
  var importPass by remember { mutableStateOf("") }
  var importJson by remember { mutableStateOf("") }
  var testResult by remember { mutableStateOf("") }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(AppleBackground)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      TextButton(onClick = onBack) {
        Text(text = "‹  Library", color = BwriterGold, fontSize = 16.sp)
      }
    }
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 28.dp, vertical = 8.dp)
    ) {
      Text(
        text = "Author Key",
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
        color = AppleInk
      )
      Text(
        text = "Bound to $email. This key signs your books; it never leaves " +
          "this device unsealed, and its sealed form is the same backup " +
          "format the Android app uses.",
        fontSize = 14.sp,
        color = AppleSecondary,
        modifier = Modifier.padding(top = 6.dp)
      )
      Spacer(modifier = Modifier.height(18.dp))

      if (notice.isNotBlank()) {
        Text(
          text = notice,
          fontSize = 14.sp,
          color = BwriterGold,
          modifier = Modifier.padding(bottom = 12.dp)
        )
      }

      if (!hasKey) {
        Surface(
          color = AppleSurface,
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(20.dp)) {
            Text(
              text = "Create my key",
              fontSize = 17.sp,
              fontWeight = FontWeight.SemiBold,
              color = AppleInk
            )
            OutlinedTextField(
              value = createPass,
              onValueChange = { createPass = it },
              label = { Text("Passphrase (8+ characters)") },
              visualTransformation = PasswordVisualTransformation(),
              singleLine = true,
              modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
            )
            OutlinedTextField(
              value = createPass2,
              onValueChange = { createPass2 = it },
              label = { Text("Passphrase again") },
              visualTransformation = PasswordVisualTransformation(),
              singleLine = true,
              modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
            )
            Button(
              onClick = {
                if (createPass != createPass2) {
                  notice = "The two passphrases do not match."
                } else {
                  busy = true
                  notice = ""
                  KeyVault.create(email, createPass) { created ->
                    busy = false
                    if (created.isBlank()) {
                      notice = "The key could not be created."
                    } else {
                      hasKey = true
                      address = created
                      notice = "Your key is created and bound to this account."
                    }
                  }
                }
              },
              enabled = !busy && createPass.length >= 8,
              colors = ButtonDefaults.buttonColors(
                containerColor = AppleInk,
                contentColor = Color.White
              ),
              modifier = Modifier.padding(top = 14.dp)
            ) {
              Text("Create key")
            }
          }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Surface(
          color = AppleSurface,
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(20.dp)) {
            Text(
              text = "Import a backup",
              fontSize = 17.sp,
              fontWeight = FontWeight.SemiBold,
              color = AppleInk
            )
            Text(
              text = "Paste the backup from the Android app (or another browser).",
              fontSize = 13.sp,
              color = AppleSecondary,
              modifier = Modifier.padding(top = 4.dp)
            )
            OutlinedTextField(
              value = importJson,
              onValueChange = { importJson = it },
              label = { Text("Backup") },
              modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .padding(top = 12.dp)
            )
            OutlinedTextField(
              value = importPass,
              onValueChange = { importPass = it },
              label = { Text("Backup passphrase") },
              visualTransformation = PasswordVisualTransformation(),
              singleLine = true,
              modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
            )
            TextButton(
              onClick = {
                busy = true
                downloadDriveKeyBackup { backup ->
                  busy = false
                  if (backup.isBlank()) {
                    notice = "No key backup was found in your Drive."
                  } else {
                    importJson = backup
                    notice = "Backup loaded from your Drive — enter its passphrase and import."
                  }
                }
              },
              enabled = !busy
            ) {
              Text("Import from Drive instead", color = BwriterGold)
            }
            Button(
              onClick = {
                busy = true
                notice = ""
                KeyVault.import(email, importJson.trim(), importPass) { imported ->
                  busy = false
                  if (imported.isBlank()) {
                    notice = "That backup did not open or did not prove itself."
                  } else {
                    hasKey = true
                    address = imported
                    notice = "Your key is imported and bound to this account."
                  }
                }
              },
              enabled = !busy && importJson.isNotBlank() && importPass.isNotEmpty(),
              colors = ButtonDefaults.buttonColors(
                containerColor = AppleInk,
                contentColor = Color.White
              ),
              modifier = Modifier.padding(top = 14.dp)
            ) {
              Text("Import key")
            }
          }
        }
      } else if (unlockedHex.isBlank()) {
        Surface(
          color = AppleSurface,
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(20.dp)) {
            Text(
              text = "Your address",
              fontSize = 13.sp,
              color = AppleSecondary
            )
            Text(
              text = address,
              fontFamily = FontFamily.Monospace,
              fontSize = 14.sp,
              color = AppleInk,
              modifier = Modifier.padding(top = 4.dp)
            )
            OutlinedTextField(
              value = unlockPass,
              onValueChange = { unlockPass = it },
              label = { Text("Passphrase") },
              visualTransformation = PasswordVisualTransformation(),
              singleLine = true,
              modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp)
            )
            Row(modifier = Modifier.padding(top = 14.dp)) {
              Button(
                onClick = {
                  busy = true
                  notice = ""
                  KeyVault.unlock(email, unlockPass) { scalar ->
                    busy = false
                    if (scalar == null) {
                      notice = "That passphrase did not open the key."
                    } else {
                      unlockedHex = com.supernatural.bwriter.crypto.toHex(scalar)
                      notice = "Key unlocked for this session."
                    }
                  }
                },
                enabled = !busy && unlockPass.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                  containerColor = AppleInk,
                  contentColor = Color.White
                )
              ) {
                Text("Unlock")
              }
              TextButton(
                onClick = {
                  KeyVault.forget(email)
                  hasKey = false
                  address = ""
                  notice = "The key was removed from this device."
                }
              ) {
                Text("Remove from this device", color = AppleSecondary)
              }
            }
          }
        }
      } else {
        Surface(
          color = AppleSurface,
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(20.dp)) {
            Text(
              text = "Unlocked — your address",
              fontSize = 13.sp,
              color = AppleSecondary
            )
            Text(
              text = address,
              fontFamily = FontFamily.Monospace,
              fontSize = 14.sp,
              color = AppleInk,
              modifier = Modifier.padding(top = 4.dp)
            )
            Row(modifier = Modifier.padding(top = 14.dp)) {
              Button(
                onClick = {
                  val scalar = unlockedHex.hexToBytes()
                  if (scalar == null) {
                    testResult = "The unlocked key could not be read."
                  } else {
                    val sig = KeyVault.sign(scalar, address)
                    testResult = if (
                      sig.isNotBlank() &&
                      Secp256k1.verify(Secp256k1.publicKeyHex(scalar), address, sig)
                    ) {
                      "Test signature verified — this key signs as your address."
                    } else {
                      "The test signature did not verify."
                    }
                  }
                },
                colors = ButtonDefaults.buttonColors(
                  containerColor = AppleInk,
                  contentColor = Color.White
                )
              ) {
                Text("Sign a test message")
              }
              TextButton(
                onClick = {
                  unlockedHex = ""
                  testResult = ""
                  notice = "Key locked."
                }
              ) {
                Text("Lock", color = AppleSecondary)
              }
            }
            if (testResult.isNotBlank()) {
              Text(
                text = testResult,
                fontSize = 14.sp,
                color = BwriterGold,
                modifier = Modifier.padding(top = 10.dp)
              )
            }
            Text(
              text = "Your sealed backup (copy it somewhere safe):",
              fontSize = 13.sp,
              color = AppleSecondary,
              modifier = Modifier.padding(top = 18.dp)
            )
            OutlinedTextField(
              value = KeyVault.sealed(email) ?: "",
              onValueChange = { },
              readOnly = true,
              modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .padding(top = 8.dp)
            )
            Button(
              onClick = {
                val sealedRecord = KeyVault.sealed(email)
                if (sealedRecord == null) {
                  notice = "No sealed key is stored on this device."
                } else {
                  busy = true
                  uploadDriveKeyBackup(sealedRecord) { error ->
                    busy = false
                    notice = if (error.isBlank()) {
                      "Backup saved to your Drive."
                    } else {
                      "Drive backup did not land: $error"
                    }
                  }
                }
              },
              enabled = !busy,
              colors = ButtonDefaults.buttonColors(
                containerColor = AppleInk,
                contentColor = Color.White
              ),
              modifier = Modifier.padding(top = 12.dp)
            ) {
              Text("Save backup to Drive")
            }
          }
        }
      }
      Spacer(modifier = Modifier.height(40.dp))
    }
  }
}
