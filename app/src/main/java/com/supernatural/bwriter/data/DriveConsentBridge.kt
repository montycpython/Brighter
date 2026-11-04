package com.supernatural.bwriter.data

import android.app.Activity
import androidx.activity.result.IntentSenderRequest
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object DriveConsentBridge {

    @Volatile
    var launcher: ((IntentSenderRequest) -> Unit)? = null

    @Volatile
    private var pending: ((Boolean) -> Unit)? = null

    suspend fun requestConsent(request: IntentSenderRequest): Boolean {
        val launch = launcher ?: return false
        return suspendCancellableCoroutine { cont ->
            pending = { granted -> if (cont.isActive) cont.resume(granted) }
            cont.invokeOnCancellation { pending = null }
            launch(request)
        }
    }

    fun onConsentResult(resultCode: Int) {
        val callback = pending
        pending = null
        callback?.invoke(resultCode == Activity.RESULT_OK)
    }
}
