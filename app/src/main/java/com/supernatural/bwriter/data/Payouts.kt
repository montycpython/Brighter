package com.supernatural.bwriter.data

object PayoutMethods {
    const val PAYPAL = "PAYPAL"
    const val STRIPE = "STRIPE"
}

object PayoutStatuses {
    const val PENDING = "PENDING"
    const val SENT = "SENT"
    const val REJECTED = "REJECTED"
}

enum class PayoutRequestResult {
    CREATED,
    EXISTING_PENDING,
    NO_VESTED_UNITS,
    NO_VALUE,
    NOT_SIGNED_IN,
    FAILED
}

data class PayoutRequest(
    val id: String = "",
    val requesterEmail: String = "",
    val units: Int = 0,
    val amountCents: Long = 0L,
    val method: String = "",
    val destination: String = "",
    val status: String = PayoutStatuses.PENDING,
    val createdAtMillis: Long = 0L,
    val paidAtMillis: Long = 0L,
    val prevRequestHash: String = "",
    val requestHash: String = ""
)

data class PayoutRequestOutcome(
    val request: PayoutRequest? = null,
    val result: PayoutRequestResult = PayoutRequestResult.FAILED
)

data class StudioTotals(
    val settledBalanceCents: Long = 0L,
    val totalVestedUnits: Int = 0,
    val unitValueCents: Long = 0L,
    val updatedAtMillis: Long = 0L
)

object PayoutChain {
    fun requestHashFor(
        prevHash: String,
        requesterEmail: String,
        units: Int,
        amountCents: Long,
        method: String,
        createdAtMillis: Long
    ): String = CreditChain.sha256Hex("$prevHash|$requesterEmail|$units|$amountCents|$method|$createdAtMillis")
}
