package com.supernatural.bwriter.data

import java.security.MessageDigest

object CreditSchedule {
    const val BASE_AMOUNT = 50
    const val HALVING_INTERVAL = 4
    const val BASE_DIFFICULTY_BITS = 20

    fun amountForApproval(priorMinedClaims: Int): Int {
        val era = priorMinedClaims / HALVING_INTERVAL
        if (era >= 31) return 1
        return maxOf(1, BASE_AMOUNT shr era)
    }

    const val MAX_DIFFICULTY_BITS = 26

    fun difficultyBits(priorAuthorApprovals: Int): Int =
        minOf(MAX_DIFFICULTY_BITS, BASE_DIFFICULTY_BITS + (priorAuthorApprovals / HALVING_INTERVAL))

    fun difficultyFor(issuances: List<CreditIssuance>, target: CreditIssuance): Int {
        val sorted = issuances.sortedWith(
            compareBy<CreditIssuance> { it.issuedAtMillis }.thenBy { it.issuanceHash }
        )
        val priorAuthorApprovals = sorted
            .takeWhile { it.issuanceHash != target.issuanceHash }
            .count { it.kind == "AUTHOR" }
        return difficultyBits(priorAuthorApprovals)
    }
}

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
    val signature: String = "",
    val bonus: Boolean = false
)

)

data class CreditTransfer(
    val id: String = "",
    val fromEmail: String = "",
    val toEmail: String = "",
    val amount: Int = 0,
    val createdAtMillis: Long = 0L,
    val prevTransferHash: String = "",
    val transferHash: String = ""
)

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

    fun transferHashFor(
        prevHash: String,
        fromEmail: String,
        toEmail: String,
        amount: Int,
        createdAtMillis: Long
    ): String = sha256Hex("$prevHash|$fromEmail|$toEmail|$amount|$createdAtMillis")

    fun issuanceHashFor(
        prevHash: String,
        authorAddress: String,
        amount: Int,
        fileId: String,
        issuedAtMillis: Long
    ): String = sha256Hex("$prevHash|$authorAddress|$amount|$fileId|$issuedAtMillis")

    fun verifyClaim(issuanceHash: String, nonce: Long, difficultyBits: Int): Boolean {
        val hash = MessageDigest.getInstance("SHA-256")
            .digest("$issuanceHash|$nonce".toByteArray(Charsets.UTF_8))
        return hasLeadingZeroBits(hash, difficultyBits)
    }

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
