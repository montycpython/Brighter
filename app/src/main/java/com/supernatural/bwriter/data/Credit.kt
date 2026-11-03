package com.supernatural.bwriter.data

import java.security.MessageDigest

/**
 * In-app credit: minted by a signed issuance at the moment a book is
 * approved, and claimed by the author's own device doing real
 * proof-of-work. The reward halves at intervals of the author's own
 * approval count, and the claim difficulty rises on the same schedule,
 * so the deeper an author goes, the less each book earns and the
 * harder each claim is to mine.
 */
object CreditSchedule {
    const val BASE_AMOUNT = 100
    const val HALVING_INTERVAL = 4
    const val BASE_DIFFICULTY_BITS = 20

    /** Credit minted for an approval, given how many approvals came before it. */
    fun amountForApproval(priorAuthorApprovals: Int): Int {
        val era = priorAuthorApprovals / HALVING_INTERVAL
        if (era >= 31) return 1
        return maxOf(1, BASE_AMOUNT shr era)
    }

    /** Leading zero bits a claim hash must carry, on the same era schedule. */
    fun difficultyBits(priorAuthorApprovals: Int): Int =
        BASE_DIFFICULTY_BITS + (priorAuthorApprovals / HALVING_INTERVAL)
}

/**
 * One signed minting record. The Editor in Chief's key signs the
 * issuance hash at approval; the chain of prevIssuanceHash links makes
 * each author's credit history verifiable end to end.
 */
data class CreditIssuance(
    val id: String = "",
    val authorEmail: String = "",
    val authorAddress: String = "",
    val amount: Int = 0,
    val kind: String = "AUTHOR", // AUTHOR or EDITOR
    val fileId: String = "",
    val birthId: String = "",
    val issuedAtMillis: Long = 0L,
    val prevIssuanceHash: String = "",
    val issuanceHash: String = "",
    val signature: String = ""
)

/**
 * An editor of record on a book: appointed by the Editor in Chief, or
 * requested by a user and decided by the Editor in Chief. Approved
 * assignments earn their editor the same credit as the book's author
 * when the book is approved.
 */
data class EditorAssignment(
    val id: String = "",
    val fileId: String = "",
    val bookAuthorEmail: String = "",
    val editorEmail: String = "",
    val editorAddress: String = "",
    val requestedBy: String = "",
    val status: String = "PENDING" // PENDING, APPROVED, DECLINED
)

object CreditChain {
    fun sha256Hex(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun issuanceHashFor(
        prevHash: String,
        authorAddress: String,
        amount: Int,
        fileId: String,
        issuedAtMillis: Long
    ): String = sha256Hex("$prevHash|$authorAddress|$amount|$fileId|$issuedAtMillis")

    /** A claim holds when SHA-256 of "issuanceHash|nonce" carries at least [difficultyBits] leading zero bits. */
    fun verifyClaim(issuanceHash: String, nonce: Long, difficultyBits: Int): Boolean {
        val hash = MessageDigest.getInstance("SHA-256")
            .digest("$issuanceHash|$nonce".toByteArray(Charsets.UTF_8))
        return hasLeadingZeroBits(hash, difficultyBits)
    }

    /** Search for a claim nonce from zero upward; null when the iteration budget runs out. */
    fun mineClaim(issuanceHash: String, difficultyBits: Int, maxIterations: Long = 500_000_000L): Long? {
        val digest = MessageDigest.getInstance("SHA-256")
        val prefix = "$issuanceHash|".toByteArray(Charsets.UTF_8)
        var nonce = 0L
        while (nonce < maxIterations) {
            val hash = digest.digest(prefix + nonce.toString().toByteArray(Charsets.UTF_8))
            if (hasLeadingZeroBits(hash, difficultyBits)) return nonce
            nonce++
        }
        return null
    }

    private fun hasLeadingZeroBits(hash: ByteArray, bits: Int): Boolean {
        var remaining = bits
        for (byte in hash) {
            if (remaining <= 0) return true
            val value = byte.toInt() and 0xFF
            if (remaining >= 8) {
                if (value != 0) return false
                remaining -= 8
            } else {
                return (value ushr (8 - remaining)) == 0
            }
        }
        return remaining <= 0
    }
}
