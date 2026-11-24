
package com.supernatural.bwriter.web

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.js.JsAny
import kotlin.js.JsString
import kotlin.js.js
import kotlin.js.toJsString

// Hand-written interop to the Firebase web SDK. There are no official Firebase
// bindings for Kotlin/Wasm, so each declaration below maps one-to-one onto a
// documented modular SDK call (firebase/app and firebase/auth, v11):
//
//   initializeApp(config)                  -> window.BwriterFirebase.initializeApp
//   getAuth(app)                           -> window.BwriterFirebase.getAuth
//   new GoogleAuthProvider()               -> window.BwriterFirebase.GoogleAuthProvider
//   signInWithPopup(auth, provider)        -> window.BwriterFirebase.signInWithPopup
//   onAuthStateChanged(auth, callback)     -> window.BwriterFirebase.onAuthStateChanged
//   signOut(auth)                          -> window.BwriterFirebase.signOut
//
// index.html loads the SDK from the Firebase CDN and exposes those six
// functions as window.BwriterFirebase before this module starts.
//
// Each function below is a js() snippet: the compiler places the code string
// into a JavaScript function whose parameters carry the same names, so the
// snippet reads its arguments directly.

private fun firebaseConfigObject(
  apiKey: JsString,
  authDomain: JsString,
  projectId: JsString,
  storageBucket: JsString,
  messagingSenderId: JsString,
  appId: JsString
): JsAny = js("({ apiKey: apiKey, authDomain: authDomain, projectId: projectId, storageBucket: storageBucket, messagingSenderId: messagingSenderId, appId: appId })")

private fun firebaseAuth(config: JsAny): JsAny =
  js("window.BwriterFirebase.getAuth(window.BwriterFirebase.initializeApp(config))")

private fun firebaseSignInWithGoogle(auth: JsAny): Unit =
  js("{ const provider = new window.BwriterFirebase.GoogleAuthProvider(); window.BwriterFirebase.signInWithPopup(auth, provider).catch((error) => console.error('Bwriter sign-in failed', error)); }")

private fun firebaseSignOut(auth: JsAny): Unit =
  js("{ window.BwriterFirebase.signOut(auth); }")

// The JS adapter extracts the user's email and display name as one small
// JSON document (or null when signed out) — the same values the Android
// build keys standing and mail on.
private fun firebaseObserveAuth(auth: JsAny, callback: (JsString?) -> Unit): Unit =
  js("{ window.BwriterFirebase.onAuthStateChanged(auth, (user) => callback(user ? JSON.stringify({ email: user.email || '', name: user.displayName || '' }) : null)); }")

/** The signed-in user's identity. Driven solely by onAuthStateChanged. */
object AuthState {
  var email: String? by mutableStateOf(null)
    internal set
  var displayName: String by mutableStateOf("")
    internal set
}

object FirebaseAuth {
  private val auth: JsAny by lazy {
    firebaseAuth(
      firebaseConfigObject(
        FirebaseConfig.API_KEY.toJsString(),
        FirebaseConfig.AUTH_DOMAIN.toJsString(),
        FirebaseConfig.PROJECT_ID.toJsString(),
        FirebaseConfig.STORAGE_BUCKET.toJsString(),
        FirebaseConfig.MESSAGING_SENDER_ID.toJsString(),
        FirebaseConfig.APP_ID.toJsString()
      )
    )
  }

  /** Initializes the Firebase app and subscribes to auth state. Call once. */
  fun start() {
    firebaseObserveAuth(auth) { json ->
      val text = json?.toString().orEmpty()
      if (text.isBlank()) {
        AuthState.email = null
        AuthState.displayName = ""
      } else {
        AuthState.email = jsonStringField(text, "email").ifBlank { null }
        AuthState.displayName = jsonStringField(text, "name")
      }
    }
  }

  fun signInWithGoogle() = firebaseSignInWithGoogle(auth)

  fun signOut() = firebaseSignOut(auth)
}
