package com.supernatural.bwriter.ui.ads

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.supernatural.bwriter.ui.theme.BookGoldDark
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

object AdsConfig {
    const val BANNER_AD_UNIT_ID = "ca-app-pub-8153915858493339/9228952828"

    // While empty, the "Ad-free 24h" button stays hidden.
    const val REWARDED_AD_UNIT_ID = "ca-app-pub-8153915858493339/3102160618"
}

class AdsFreeStore(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("bwriter_ads", Context.MODE_PRIVATE)

    fun adsFreeUntil(): Long = prefs.getLong(KEY_ADS_FREE_UNTIL, 0L)

    fun grantAdsFree24Hours(now: Long = System.currentTimeMillis()) {
        prefs.edit().putLong(KEY_ADS_FREE_UNTIL, now + ADS_FREE_DURATION_MS).apply()
    }

    companion object {
        private const val KEY_ADS_FREE_UNTIL = "ads_free_until"
        const val ADS_FREE_DURATION_MS = 24L * 60L * 60L * 1000L
    }
}

@Composable
fun BannerAd(modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { ctx ->
            AdView(ctx).apply {
                setAdSize(
                    AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(ctx, AdSize.FULL_WIDTH)
                )
                adUnitId = AdsConfig.BANNER_AD_UNIT_ID
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}

@Composable
fun AdBannerBar(onRewardEarned: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? Activity
    var isLoadingRewarded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (AdsConfig.REWARDED_AD_UNIT_ID.isNotEmpty()) {
            Button(
                enabled = activity != null && !isLoadingRewarded,
                onClick = {
                    if (activity != null) {
                        isLoadingRewarded = true
                        loadAndShowRewardedAd(activity) { earned ->
                            isLoadingRewarded = false
                            if (earned) onRewardEarned()
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = BookGoldDark),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (isLoadingRewarded) "Loading ad..." else "Ad-free 24h",
                    fontWeight = FontWeight.Bold
                )
            }
        }
        BannerAd()
    }
}

private fun loadAndShowRewardedAd(activity: Activity, onDone: (Boolean) -> Unit) {
    RewardedAd.load(
        activity,
        AdsConfig.REWARDED_AD_UNIT_ID,
        AdRequest.Builder().build(),
        object : RewardedAdLoadCallback() {
            override fun onAdLoaded(ad: RewardedAd) {
                ad.show(activity) { onDone(true) }
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                onDone(false)
            }
        }
    )
}
