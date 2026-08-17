package com.example.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.CustomCredential
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

data class GoogleSignInResult(
    val email: String,
    val displayName: String = "",
    val familyName: String = "",
    val givenName: String = "",
    val idToken: String = "",
    val profilePictureUri: String? = null
)

object GoogleAuthHelper {
    suspend fun signInWithGoogle(
        context: Context,
        serverClientId: String = "804972469813-placeholder.apps.googleusercontent.com"
    ): Result<GoogleSignInResult> {
        val credentialManager = CredentialManager.create(context)

        val googleIdOption = GetSignInWithGoogleOption.Builder(serverClientId)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            val response = credentialManager.getCredential(
                request = request,
                context = context
            )
            val credential = response.credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                Result.success(
                    GoogleSignInResult(
                        email = googleIdTokenCredential.id,
                        displayName = googleIdTokenCredential.displayName ?: "",
                        familyName = googleIdTokenCredential.familyName ?: "",
                        givenName = googleIdTokenCredential.givenName ?: "",
                        idToken = googleIdTokenCredential.idToken,
                        profilePictureUri = googleIdTokenCredential.profilePictureUri?.toString()
                    )
                )
            } else {
                Result.failure(IllegalStateException("Unsupported credential type returned: ${credential.type}"))
            }
        } catch (e: GetCredentialCancellationException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
