package com.supernatural.bwriter.ui.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

object InterstitialAds {
    private const val MIN_INTERVAL_MS = 3L * 60L * 1000L

    private var loadedAd: InterstitialAd? = null
    private var isLoading = false
    private var lastShownAt = 0L

    fun preload(context: Context) {
        if (loadedAd != null || isLoading) return
        try {
            isLoading = true
            InterstitialAd.load(
                context.applicationContext,
                AdsConfig.INTERSTITIAL_AD_UNIT_ID,
                AdRequest.Builder().build(),
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: InterstitialAd) {
                        isLoading = false
                        loadedAd = ad
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        isLoading = false
                        loadedAd = null
                    }
                }
            )
        } catch (t: Throwable) {
            isLoading = false
        }
    }

    fun showIfEligible(activity: Activity) {
        try {
            val now = System.currentTimeMillis()
            if (now < AdsFreeStore(activity).adsFreeUntil()) return
            if (now - lastShownAt < MIN_INTERVAL_MS) return
            val ad = loadedAd
            if (ad == null) {
                preload(activity)
                return
            }
            loadedAd = null
            lastShownAt = now
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    preload(activity)
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    preload(activity)
                }
            }
            ad.show(activity)
        } catch (t: Throwable) {
            // An ad must never block the app.
        }
    }
}
