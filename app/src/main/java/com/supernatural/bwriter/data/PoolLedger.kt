package com.supernatural.bwriter.data

object PoolFunds {
    const val AUTHOR = "AUTHOR"
    const val STUDIO = "STUDIO"
}

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
    val entryHash: String = "",
    val fund: String = ""
)

data class PoolTotals(
    val receiptsCents: Long = 0L,
    val expensesCents: Long = 0L,
    val refundsCents: Long = 0L,
    val feesCents: Long = 0L,
    val payoutsCents: Long = 0L,
    val balanceCents: Long = 0L,
    val unsettledCents: Long = 0L,
    val authorBalanceCents: Long = 0L,
    val studioBalanceCents: Long = 0L,
    val authorUnsettledCents: Long = 0L
)

object PoolChain {
    const val SETTLE_WINDOW_MILLIS = 14L * 24L * 60L * 60L * 1000L

    fun settledAtFor(createdAtMillis: Long): Long = createdAtMillis + SETTLE_WINDOW_MILLIS

    fun entryHashFor(
        prevHash: String,
        kind: String,
        amountCents: Long,
        actorEmail: String,
        createdAtMillis: Long,
        fund: String = ""
    ): String = CreditChain.sha256Hex(
        if (fund.isEmpty()) "$prevHash|$kind|$amountCents|$actorEmail|$createdAtMillis"
        else "$prevHash|$kind|$amountCents|$actorEmail|$createdAtMillis|$fund"
    )

    fun totalsFor(entries: List<PoolEntry>, nowMillis: Long = System.currentTimeMillis()): PoolTotals {
        var purchases = 0L
        var adReturns = 0L
        var expenses = 0L
        var refunds = 0L
        var fees = 0L
        var payouts = 0L
        var unsettled = 0L
        var author = 0L
        var studio = 0L
        var authorUnsettled = 0L
        for (entry in entries) {
            val amount = if (entry.amountCents < 0L) -entry.amountCents else entry.amountCents
            // A receipt (or its refund) carrying no fund tag is split here, in
            // the totals; a tagged entry already carries its own half. The odd
            // cent of a split stays with the authors.
            val authorShare = when (entry.fund) {
                PoolFunds.AUTHOR -> amount
                PoolFunds.STUDIO -> 0L
                else -> amount - amount / 2
            }
            val studioShare = amount - authorShare
            when (entry.kind) {
                PoolKinds.PURCHASE -> {
                    purchases += amount
                    author += authorShare
                    studio += studioShare
                    if (entry.settledAtMillis > nowMillis) {
                        unsettled += amount
                        authorUnsettled += authorShare
                    }
                }
                PoolKinds.AD_RETURN -> {
                    adReturns += amount
                    author += authorShare
                    studio += studioShare
                    if (entry.settledAtMillis > nowMillis) {
                        unsettled += amount
                        authorUnsettled += authorShare
                    }
                }
                PoolKinds.EXPENSE -> {
                    expenses += amount
                    studio -= amount
                }
                PoolKinds.REFUND -> {
                    refunds += amount
                    author -= authorShare
                    studio -= studioShare
                }
                PoolKinds.FEE -> {
                    fees += amount
                    author -= amount
                }
                PoolKinds.PAYOUT -> {
                    payouts += amount
                    author -= amount
                }
            }
        }
        val receipts = purchases + adReturns
        return PoolTotals(
            receiptsCents = receipts,
            expensesCents = expenses,
            refundsCents = refunds,
            feesCents = fees,
            payoutsCents = payouts,
            balanceCents = author + studio,
            unsettledCents = unsettled,
            authorBalanceCents = author,
            studioBalanceCents = studio,
            authorUnsettledCents = authorUnsettled
        )
    }
}
