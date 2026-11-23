package com.supernatural.bwriter.web

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ComposeViewport
import com.supernatural.bwriter.data.bisnFor
import kotlinx.browser.document

// Fixed sample input proving the shared core executes in the browser build:
// bisnFor lives in :shared commonMain and derives the BISN from the birth id
// with the same SHA-256 the Android build uses (the wasm actual in :shared).
private const val SAMPLE_BIRTH_ID = "bwriter-web-sample"

fun main() {
  FirebaseAuth.start()
  ComposeViewport(document.body!!) {
    App()
  }
}

@Composable
fun App() {
  MaterialTheme {
    Column(modifier = Modifier.padding(24.dp)) {
      Text(
        text = "Bwriter: Phoenicia",
        style = MaterialTheme.typography.headlineMedium
      )
      val email = AuthState.email
      if (email == null) {
        Button(onClick = { FirebaseAuth.signInWithGoogle() }) {
          Text("Sign in with Google")
        }
      } else {
        Text(text = "Signed in as $email")
        Text(text = "Sample BISN (shared core): ${bisnFor(SAMPLE_BIRTH_ID)}")
        Button(onClick = { FirebaseAuth.signOut() }) {
          Text("Sign out")
        }
      }
    }
  }
}
