package com.example.model

enum class SubscriptionPlan(
    val id: String,
    val title: String,
    val priceMonthly: String,
    val monthlyCredits: Int,
    val tokenAllowanceDescription: String,
    val badgeColorHex: Long
) {
    FREE(
        id = "bwriter_free_starter",
        title = "Starter Author",
        priceMonthly = "$0.00",
        monthlyCredits = 5,
        tokenAllowanceDescription = "5 complimentary AI chapter generations (5,000 tokens)",
        badgeColorHex = 0xFF7E7E8E
    ),
    STANDARD_AUTHOR(
        id = "bwriter_standard_monthly",
        title = "Standard Novelist",
        priceMonthly = "$9.99/mo",
        monthlyCredits = 500,
        tokenAllowanceDescription = "500 AI chapter generations (~500,000 CMOS tokens/mo)",
        badgeColorHex = 0xFFC5A059
    ),
    PRO_IMPRINT(
        id = "bwriter_pro_imprint_monthly",
        title = "Pro Imprint / Studio",
        priceMonthly = "$24.99/mo",
        monthlyCredits = 2500,
        tokenAllowanceDescription = "2,500 AI chapter generations (~2,500,000 CMOS tokens/mo) + Priority Gemini 2.5",
        badgeColorHex = 0xFFE5C07B
    ),
    SUPERUSER_UNLIMITED(
        id = "bwriter_godmode_unlimited",
        title = "Editor-in-Chief Unlimited",
        priceMonthly = "Complimentary",
        monthlyCredits = 999999,
        tokenAllowanceDescription = "Unlimited AI prose generations & full token ledger bypass",
        badgeColorHex = 0xFFFFD700
    );

    companion object {
        fun fromId(id: String): SubscriptionPlan {
            return entries.find { it.id.equals(id, ignoreCase = true) } ?: FREE
        }
    }
}

data class UserAiSubscription(
    val userEmail: String = "",
    val plan: SubscriptionPlan = SubscriptionPlan.FREE,
    val creditsRemaining: Int = 5,
    val totalTokensUsed: Long = 0L,
    val totalGenerationsCount: Int = 0,
    val monthlyRenewalTimestamp: Long = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000),
    val isActive: Boolean = true,
    val lastTransactionId: String = ""
)

data class AiGenerationResult(
    val text: String,
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int,
    val modelUsed: String,
    val isSuccess: Boolean,
    val errorMessage: String? = null
)

data class AiTokenTransaction(
    val transactionId: String,
    val userEmail: String,
    val sectionTitle: String,
    val timestamp: Long,
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int,
    val creditsDeducted: Int,
    val modelUsed: String,
    val isSuccess: Boolean
)

data class PaidMemberTelemetry(
    val userEmail: String,
    val displayName: String,
    val planId: String,
    val planTitle: String,
    val creditsRemaining: Int,
    val totalTokensUsed: Long,
    val totalGenerationsCount: Int,
    val monthlyRenewalTimestamp: Long,
    val lastActiveTimestamp: Long,
    val status: String
)
