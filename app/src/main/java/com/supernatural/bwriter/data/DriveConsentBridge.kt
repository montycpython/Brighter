package com.supernatural.bwriter.data

import android.app.Activity
import androidx.activity.result.IntentSenderRequest
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

object DriveConsentBridge {

    @Volatile
    var launcher: ((IntentSenderRequest) -> Unit)? = null

    @Volatile
    private var pending: ((Boolean) -> Unit)? = null

    suspend fun requestConsent(request: IntentSenderRequest): Boolean {
        val launch = launcher ?: return false
        val granted = withTimeoutOrNull(120_000L) {
            suspendCancellableCoroutine { cont ->
                val previous = pending
                pending = { granted -> if (cont.isActive) cont.resume(granted) }
                previous?.invoke(false)
                cont.invokeOnCancellation { pending = null }
                try {
                    launch(request)
                } catch (e: Exception) {
                    pending = null
                    if (cont.isActive) cont.resume(false)
                }
            }
        }
        return granted ?: false
    }

    fun onConsentResult(resultCode: Int) {
        val callback = pending
        pending = null
        callback?.invoke(resultCode == Activity.RESULT_OK)
    }
}
