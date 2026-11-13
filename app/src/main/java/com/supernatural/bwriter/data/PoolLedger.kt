package com.supernatural.bwriter.data

object PoolKinds {
    const val PURCHASE = "PURCHASE"
    const val EXPENSE = "EXPENSE"
    const val REFUND = "REFUND"
    const val FEE = "FEE"
    const val AD_RETURN = "AD_RETURN"
    const val PAYOUT = "PAYOUT"
}

data class PoolEntry(
    val id: String = "",
    val kind: String = "",
    val amountCents: Long = 0L,
    val note: String = "",
    val actorEmail: String = "",
    val createdAtMillis: Long = 0L,
    val settledAtMillis: Long = 0L,
    val prevEntryHash: String = "",
    val entryHash: String = ""
)

data class PoolTotals(
    val receiptsCents: Long = 0L,
    val expensesCents: Long = 0L,
    val refundsCents: Long = 0L,
    val feesCents: Long = 0L,
    val payoutsCents: Long = 0L,
    val balanceCents: Long = 0L,
    val unsettledCents: Long = 0L
)

object PoolChain {
    const val SETTLE_WINDOW_MILLIS = 14L * 24L * 60L * 60L * 1000L

    fun settledAtFor(createdAtMillis: Long): Long = createdAtMillis + SETTLE_WINDOW_MILLIS

    fun entryHashFor(
        prevHash: String,
        kind: String,
        amountCents: Long,
        actorEmail: String,
        createdAtMillis: Long
    ): String = CreditChain.sha256Hex("$prevHash|$kind|$amountCents|$actorEmail|$createdAtMillis")

    fun totalsFor(entries: List<PoolEntry>, nowMillis: Long = System.currentTimeMillis()): PoolTotals {
        var purchases = 0L
        var adReturns = 0L
        var expenses = 0L
        var refunds = 0L
        var fees = 0L
        var payouts = 0L
        var unsettled = 0L
        for (entry in entries) {
            val amount = if (entry.amountCents < 0L) -entry.amountCents else entry.amountCents
            when (entry.kind) {
                PoolKinds.PURCHASE -> {
                    purchases += amount
                    if (entry.settledAtMillis > nowMillis) unsettled += amount
                }
                PoolKinds.AD_RETURN -> {
                    adReturns += amount
                    if (entry.settledAtMillis > nowMillis) unsettled += amount
                }
                PoolKinds.EXPENSE -> expenses += amount
                PoolKinds.REFUND -> refunds += amount
                PoolKinds.FEE -> fees += amount
                PoolKinds.PAYOUT -> payouts += amount
            }
        }
        val receipts = purchases + adReturns
        return PoolTotals(
            receiptsCents = receipts,
            expensesCents = expenses,
            refundsCents = refunds,
            feesCents = fees,
            payoutsCents = payouts,
            balanceCents = receipts - expenses - refunds - fees - payouts,
            unsettledCents = unsettled
        )
    }
}
