package com.supernatural.bwriter.auth

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

/**
 * Bridges the Credential Manager Google sign-in to a Firebase Auth session.
 * Firestore security rules key on request.auth, so the app must hold a
 * Firebase session for the back room to recognize the signed-in user.
 */
object FirebaseAuthBridge {

    suspend fun signInWithGoogleIdToken(idToken: String): Result<Unit> {
        if (idToken.isBlank()) {
            return Result.failure(IllegalStateException("Missing Google ID token"))
        }
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            FirebaseAuth.getInstance().signInWithCredential(credential).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseAuthBridge", "Firebase sign-in failed", e)
            Result.failure(e)
        }
    }

    fun signOut() {
        try {
            FirebaseAuth.getInstance().signOut()
        } catch (e: Exception) {
            Log.e("FirebaseAuthBridge", "Firebase sign-out failed", e)
        }
    }
}
