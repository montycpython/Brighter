@file:OptIn(ExperimentalWasmJsInterop::class)

package com.supernatural.bwriter.web

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.js.JsAny
import kotlin.JsFun
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsString
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

@JsFun(
  "(apiKey, authDomain, projectId, storageBucket, messagingSenderId, appId) => " +
    "({ apiKey: apiKey, authDomain: authDomain, projectId: projectId, " +
    "storageBucket: storageBucket, messagingSenderId: messagingSenderId, appId: appId })"
)
private external fun firebaseConfigObject(
  apiKey: JsString,
  authDomain: JsString,
  projectId: JsString,
  storageBucket: JsString,
  messagingSenderId: JsString,
  appId: JsString
): JsAny

@JsFun(
  "(config) => window.BwriterFirebase.getAuth(window.BwriterFirebase.initializeApp(config))"
)
private external fun firebaseAuth(config: JsAny): JsAny

@JsFun(
  "(auth) => { const provider = new window.BwriterFirebase.GoogleAuthProvider(); " +
    "window.BwriterFirebase.signInWithPopup(auth, provider)" +
    ".catch((error) => console.error('Bwriter sign-in failed', error)); }"
)
private external fun firebaseSignInWithGoogle(auth: JsAny)

@JsFun("(auth) => { window.BwriterFirebase.signOut(auth); }")
private external fun firebaseSignOut(auth: JsAny)

// The JS adapter extracts user.email, so the Kotlin side only ever sees the
// email string (or null when signed out) — the same value the Android build
// keys standing and mail on.
@JsFun(
  "(auth, callback) => { window.BwriterFirebase.onAuthStateChanged(auth, " +
    "(user) => callback(user ? user.email : null)); }"
)
private external fun firebaseObserveAuth(auth: JsAny, callback: (JsString?) -> Unit)

/** The signed-in user's email, or null. Driven solely by onAuthStateChanged. */
object AuthState {
  var email: String? by mutableStateOf(null)
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
    firebaseObserveAuth(auth) { email ->
      AuthState.email = email?.toString()
    }
  }

  fun signInWithGoogle() = firebaseSignInWithGoogle(auth)

  fun signOut() = firebaseSignOut(auth)
}
