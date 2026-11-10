package com.supernatural.bwriter.data

import java.util.Calendar

object UnitRoles {
    const val AUTHOR = "AUTHOR"
    const val EDITOR = "EDITOR"
    const val CHAIR = "CHAIR"
}

enum class ApprovalUnitsResult {
    WRITTEN,
    ALREADY_RECORDED,
    SEASON_CAP_REACHED,
    NOT_EDITOR_IN_CHIEF,
    FAILED
}

data class UnitGrant(
    val id: String = "",
    val holderEmail: String = "",
    val role: String = "",
    val fileId: String = "",
    val seasonYear: Int = 0,
    val issuedAtMillis: Long = 0L,
    val vested: Boolean = false,
    val prevGrantHash: String = "",
    val grantHash: String = ""
)

object UnitChain {
    const val SEASON_AUTHOR_CAP = 30

    fun grantHashFor(
        prevHash: String,
        holderEmail: String,
        role: String,
        fileId: String,
        issuedAtMillis: Long
    ): String = CreditChain.sha256Hex("$prevHash|$holderEmail|$role|$fileId|$issuedAtMillis")

    fun seasonYearFor(millis: Long): Int {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = millis
        val year = calendar.get(Calendar.YEAR)
        return when (calendar.get(Calendar.MONTH)) {
            Calendar.NOVEMBER -> year
            Calendar.DECEMBER -> year + 1
            else -> year
        }
    }

    fun totalsByHolder(grants: List<UnitGrant>): Map<String, Int> =
        grants.groupingBy { it.holderEmail }.eachCount()

    fun unitValueCents(settledBalanceCents: Long, vestedCount: Int): Long =
        if (vestedCount <= 0) 0L else settledBalanceCents / vestedCount
}
