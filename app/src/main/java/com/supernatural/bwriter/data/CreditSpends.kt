package com.supernatural.bwriter.data

data class CreditSpend(
    val id: String = "",
    val fromEmail: String = "",
    val amount: Int = 0,
    val purpose: String = "",
    val createdAtMillis: Long = 0L,
    val prevSpendHash: String = "",
    val spendHash: String = ""
)

object SpendPurposes {
    const val GENERATION = "GENERATION"
    const val AD_FREE = "AD_FREE"
}

object SpendCosts {
    const val GENERATION_COST = 1
    const val AD_FREE_COST = 5
    // The 24-hour window itself is AdsFreeStore's ADS_FREE_DURATION_MS,
    // applied by grantAdsFree24Hours() — not duplicated here.
}

object SpendChain {
    fun spendHashFor(
        prevHash: String,
        fromEmail: String,
        amount: Int,
        purpose: String,
        createdAtMillis: Long
    ): String = CreditChain.sha256Hex("$prevHash|$fromEmail|$amount|$purpose|$createdAtMillis")
}
