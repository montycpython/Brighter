package com.supernatural.bwriter.data

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams

object BillingProducts {
    const val AD_FREE_PRODUCT_ID = "adfree"
    const val TOKENS_PRODUCT_ID = "tokens_20"
    const val TOKENS_PER_PACK = 20
    const val TOKENS_100_PRODUCT_ID = "tokens_100"
    const val TOKENS_100_PER_PACK = 100

    // The ad-free product type lives in exactly this one place: flip it to
    // BillingClient.ProductType.SUBS if ad-free ever becomes a subscription.
    // A plain val, not const: nothing needs it at compile time, and a val
    // does not depend on how the billing AAR exposes the ProductType field.
    val AD_FREE_PRODUCT_TYPE = BillingClient.ProductType.INAPP

    const val AD_FREE_PRICE_CENTS = 299L
    const val TOKENS_PRICE_CENTS = 99L
    const val TOKENS_100_PRICE_CENTS = 399L

    fun priceCentsFor(productId: String): Long = when (productId) {
        AD_FREE_PRODUCT_ID -> AD_FREE_PRICE_CENTS
        TOKENS_PRODUCT_ID -> TOKENS_PRICE_CENTS
        TOKENS_100_PRODUCT_ID -> TOKENS_100_PRICE_CENTS
        else -> 0L
    }

    fun tokensPerPackFor(productId: String): Int = when (productId) {
        TOKENS_PRODUCT_ID -> TOKENS_PER_PACK
        TOKENS_100_PRODUCT_ID -> TOKENS_100_PER_PACK
        else -> 0
    }
}

data class BillingPurchase(
    val productId: String,
    val purchaseToken: String,
    val orderId: String,
    val purchaseTime: Long
)

class BillingManager(
    context: Context,
    private val onPurchase: (BillingPurchase) -> Unit,
    private val onPrices: (Map<String, String>) -> Unit
) {
    private val purchasesListener = PurchasesUpdatedListener { result, purchases ->
        if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            purchases.forEach { report(it) }
        }
    }

    private val client: BillingClient = BillingClient.newBuilder(context.applicationContext)
        .setListener(purchasesListener)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
        )
        .build()

    private val detailsById = mutableMapOf<String, ProductDetails>()

    fun connect() {
        if (client.isReady) {
            onConnected()
            return
        }
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    onConnected()
                }
            }

            override fun onBillingServiceDisconnected() {
                connect()
            }
        })
    }

    private fun onConnected() {
        queryDetails()
        queryOwned()
    }

    private fun queryDetails() {
        val products = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(BillingProducts.AD_FREE_PRODUCT_ID)
                .setProductType(BillingProducts.AD_FREE_PRODUCT_TYPE)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(BillingProducts.TOKENS_PRODUCT_ID)
                .setProductType(BillingClient.ProductType.INAPP)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(BillingProducts.TOKENS_100_PRODUCT_ID)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )
        val params = QueryProductDetailsParams.newBuilder().setProductList(products).build()
        client.queryProductDetailsAsync(params) { result, queryResult ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                val prices = mutableMapOf<String, String>()
                queryResult.productDetailsList.forEach { details ->
                    detailsById[details.productId] = details
                    val price = details.oneTimePurchaseOfferDetails?.formattedPrice
                    if (price != null) prices[details.productId] = price
                }
                if (prices.isNotEmpty()) onPrices(prices)
            }
        }
    }

    private fun queryOwned() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        client.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                purchases.forEach { report(it) }
            }
        }
    }

    private fun report(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return
        val productId = purchase.products.firstOrNull() ?: return
        onPurchase(
            BillingPurchase(
                productId = productId,
                purchaseToken = purchase.purchaseToken,
                orderId = purchase.orderId ?: "",
                purchaseTime = purchase.purchaseTime
            )
        )
    }

    fun launch(activity: Activity, productId: String): Boolean {
        val details = detailsById[productId] ?: return false
        if (!client.isReady) return false
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(details)
                        .build()
                )
            )
            .build()
        val result = client.launchBillingFlow(activity, params)
        return result.responseCode == BillingClient.BillingResponseCode.OK
    }

    fun acknowledge(purchase: BillingPurchase) {
        if (!client.isReady) return
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        client.acknowledgePurchase(params) { result ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                Log.e("Billing", "Acknowledge failed: ${result.responseCode}")
            }
        }
    }

    fun consume(purchase: BillingPurchase) {
        if (!client.isReady) return
        val params = ConsumeParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        client.consumeAsync(params) { result, _ ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                Log.e("Billing", "Consume failed: ${result.responseCode}")
            }
        }
    }
}
