package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.UserProfile
import com.example.model.WorkRole

class UserPreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("bwriter_user_prefs", Context.MODE_PRIVATE)

    fun getUserProfile(): UserProfile {
        val id = prefs.getString(KEY_ID, "user_google_1") ?: "user_google_1"
        val email = prefs.getString(KEY_EMAIL, "real.artistry@gmail.com") ?: "real.artistry@gmail.com"
        val name = prefs.getString(KEY_NAME, "Author") ?: "Author"
        val penName = prefs.getString(KEY_PEN_NAME, "") ?: ""
        val roleStr = prefs.getString(KEY_ROLE, WorkRole.AUTHOR.name) ?: WorkRole.AUTHOR.name
        val role = try {
            WorkRole.valueOf(roleStr)
        } catch (e: Exception) {
            WorkRole.AUTHOR
        }
        val org = prefs.getString(KEY_ORG, "Author Studio") ?: "Author Studio"
        val cmos = prefs.getString(KEY_CMOS, "17th Edition") ?: "17th Edition"

        return UserProfile(
            id = id,
            email = email,
            name = name,
            penName = penName,
            role = role,
            organization = org,
            preferredCmosEdition = cmos
        )
    }

    fun saveUserProfile(profile: UserProfile) {
        prefs.edit()
            .putString(KEY_ID, profile.id)
            .putString(KEY_EMAIL, profile.email)
            .putString(KEY_NAME, profile.name)
            .putString(KEY_PEN_NAME, profile.penName)
            .putString(KEY_ROLE, profile.role.name)
            .putString(KEY_ORG, profile.organization)
            .putString(KEY_CMOS, profile.preferredCmosEdition)
            .apply()
    }

    companion object {
        private const val KEY_ID = "user_id"
        private const val KEY_EMAIL = "user_email"
        private const val KEY_NAME = "user_name"
        private const val KEY_PEN_NAME = "user_pen_name"
        private const val KEY_ROLE = "user_role"
        private const val KEY_ORG = "user_org"
        private const val KEY_CMOS = "user_cmos"
        private const val KEY_IS_LOGGED_IN = "user_is_logged_in"
        private const val KEY_SAVED_ACCOUNTS_JSON = "saved_google_accounts_json"
        private const val KEY_TERMS_ACCEPTED_VERSION = "user_terms_accepted_version"
        private const val KEY_TERMS_ACCEPTED_TIMESTAMP = "user_terms_accepted_timestamp"
        private const val KEY_SUBSCRIPTION_PLAN = "user_subscription_plan"
        private const val KEY_CREDITS_REMAINING = "user_credits_remaining"
        private const val KEY_TOTAL_TOKENS_USED = "user_total_tokens_used"
        private const val KEY_TOTAL_GENERATIONS = "user_total_generations"
        private const val KEY_RENEWAL_TIMESTAMP = "user_renewal_timestamp"
        private const val KEY_TOKEN_TRANSACTIONS_JSON = "user_token_transactions_json"
        private const val KEY_ALL_SUBSCRIBERS_JSON = "admin_all_subscribers_json"
    }

    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun setLoggedIn(loggedIn: Boolean) {
        prefs.edit().putBoolean(KEY_IS_LOGGED_IN, loggedIn).apply()
    }

    fun signOut() {
        prefs.edit().putBoolean(KEY_IS_LOGGED_IN, false).apply()
    }

    fun getSavedGoogleAccounts(): List<UserProfile> {
        val list = mutableListOf<UserProfile>()
        val defaultEditor = UserProfile(
            id = "user_editor_in_chief",
            email = "real.artistry@gmail.com",
            name = "Editor-in-Chief",
            penName = "real.artistry",
            role = WorkRole.EDITOR,
            organization = "Bwriter Editorial Board",
            preferredCmosEdition = "17th Edition"
        )
        val defaultAuthor = UserProfile(
            id = "user_author_demo",
            email = "author.studio@bwriter.io",
            name = "Jane Austen",
            penName = "J. Austen",
            role = WorkRole.AUTHOR,
            organization = "Literary Arts Studio",
            preferredCmosEdition = "17th Edition"
        )

        try {
            val jsonStr = prefs.getString(KEY_SAVED_ACCOUNTS_JSON, null)
            if (!jsonStr.isNullOrBlank()) {
                val array = org.json.JSONArray(jsonStr)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        UserProfile(
                            id = obj.optString("id", "user_${System.currentTimeMillis()}"),
                            email = obj.optString("email", ""),
                            name = obj.optString("name", "Author"),
                            penName = obj.optString("penName", ""),
                            role = try {
                                WorkRole.valueOf(obj.optString("role", WorkRole.AUTHOR.name))
                            } catch (e: Exception) {
                                WorkRole.AUTHOR
                            },
                            organization = obj.optString("organization", "Author Studio"),
                            preferredCmosEdition = obj.optString("preferredCmosEdition", "17th Edition")
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Prepopulate with default accounts if empty
        if (list.isEmpty()) {
            list.add(defaultEditor)
            list.add(defaultAuthor)
            saveSavedGoogleAccounts(list)
        } else {
            // Ensure real.artistry is available in list
            if (list.none { it.email.equals(defaultEditor.email, ignoreCase = true) }) {
                list.add(0, defaultEditor)
            }
        }
        return list
    }

    fun saveSavedGoogleAccounts(accounts: List<UserProfile>) {
        try {
            val array = org.json.JSONArray()
            accounts.distinctBy { it.email.lowercase() }.forEach { profile ->
                val obj = org.json.JSONObject()
                obj.put("id", profile.id)
                obj.put("email", profile.email)
                obj.put("name", profile.name)
                obj.put("penName", profile.penName)
                obj.put("role", profile.role.name)
                obj.put("organization", profile.organization)
                obj.put("preferredCmosEdition", profile.preferredCmosEdition)
                array.put(obj)
            }
            prefs.edit().putString(KEY_SAVED_ACCOUNTS_JSON, array.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun addOrUpdateSavedAccount(profile: UserProfile) {
        val currentAccounts = getSavedGoogleAccounts().toMutableList()
        val index = currentAccounts.indexOfFirst { it.email.equals(profile.email, ignoreCase = true) }
        if (index >= 0) {
            currentAccounts[index] = profile
        } else {
            currentAccounts.add(0, profile)
        }
        saveSavedGoogleAccounts(currentAccounts)
    }

    fun removeSavedGoogleAccount(email: String) {
        val currentAccounts = getSavedGoogleAccounts().toMutableList()
        currentAccounts.removeAll { it.email.equals(email, ignoreCase = true) }
        saveSavedGoogleAccounts(currentAccounts)
    }

    fun hasAcceptedTerms(): Boolean {
        val acceptedVer = prefs.getString(KEY_TERMS_ACCEPTED_VERSION, null)
        return !acceptedVer.isNullOrBlank()
    }

    fun getAcceptedTermsVersion(): String? {
        return prefs.getString(KEY_TERMS_ACCEPTED_VERSION, null)
    }

    fun setAcceptedTerms(version: String) {
        prefs.edit()
            .putString(KEY_TERMS_ACCEPTED_VERSION, version)
            .putLong(KEY_TERMS_ACCEPTED_TIMESTAMP, System.currentTimeMillis())
            .apply()
    }

    fun getUserSubscription(email: String): com.example.model.UserAiSubscription {
        val isEditorInChief = email.equals("real.artistry@gmail.com", ignoreCase = true)
        val defaultPlan = if (isEditorInChief) com.example.model.SubscriptionPlan.SUPERUSER_UNLIMITED else com.example.model.SubscriptionPlan.FREE
        val defaultCredits = if (isEditorInChief) 999999 else 5

        val planId = prefs.getString(KEY_SUBSCRIPTION_PLAN, defaultPlan.id) ?: defaultPlan.id
        val plan = if (isEditorInChief) com.example.model.SubscriptionPlan.SUPERUSER_UNLIMITED else com.example.model.SubscriptionPlan.fromId(planId)
        val credits = prefs.getInt(KEY_CREDITS_REMAINING, defaultCredits)
        val tokensUsed = prefs.getLong(KEY_TOTAL_TOKENS_USED, 0L)
        val generationsCount = prefs.getInt(KEY_TOTAL_GENERATIONS, 0)
        val renewalTime = prefs.getLong(KEY_RENEWAL_TIMESTAMP, System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000))

        return com.example.model.UserAiSubscription(
            userEmail = email,
            plan = plan,
            creditsRemaining = if (isEditorInChief) 999999 else credits,
            totalTokensUsed = tokensUsed,
            totalGenerationsCount = generationsCount,
            monthlyRenewalTimestamp = renewalTime,
            isActive = true
        )
    }

    fun saveUserSubscription(subscription: com.example.model.UserAiSubscription) {
        prefs.edit()
            .putString(KEY_SUBSCRIPTION_PLAN, subscription.plan.id)
            .putInt(KEY_CREDITS_REMAINING, subscription.creditsRemaining)
            .putLong(KEY_TOTAL_TOKENS_USED, subscription.totalTokensUsed)
            .putInt(KEY_TOTAL_GENERATIONS, subscription.totalGenerationsCount)
            .putLong(KEY_RENEWAL_TIMESTAMP, subscription.monthlyRenewalTimestamp)
            .apply()

        recordTelemetryInRegistry(subscription)
    }

    fun recordTokenUsage(
        email: String,
        sectionTitle: String,
        promptTokens: Int,
        completionTokens: Int,
        totalTokens: Int,
        modelUsed: String
    ): com.example.model.UserAiSubscription {
        val current = getUserSubscription(email)
        val isEditorInChief = email.equals("real.artistry@gmail.com", ignoreCase = true)
        val newCredits = if (isEditorInChief) current.creditsRemaining else Math.max(0, current.creditsRemaining - 1)
        val newTokensUsed = current.totalTokensUsed + totalTokens
        val newGenerations = current.totalGenerationsCount + 1

        val updated = current.copy(
            creditsRemaining = newCredits,
            totalTokensUsed = newTokensUsed,
            totalGenerationsCount = newGenerations
        )
        saveUserSubscription(updated)

        // Record transaction
        val tx = com.example.model.AiTokenTransaction(
            transactionId = "tx_" + System.currentTimeMillis() + "_" + (100..999).random(),
            userEmail = email,
            sectionTitle = sectionTitle,
            timestamp = System.currentTimeMillis(),
            promptTokens = promptTokens,
            completionTokens = completionTokens,
            totalTokens = totalTokens,
            creditsDeducted = if (isEditorInChief) 0 else 1,
            modelUsed = modelUsed,
            isSuccess = true
        )
        appendTokenTransaction(tx)
        return updated
    }

    private fun appendTokenTransaction(tx: com.example.model.AiTokenTransaction) {
        try {
            val raw = prefs.getString(KEY_TOKEN_TRANSACTIONS_JSON, "[]") ?: "[]"
            val array = org.json.JSONArray(raw)
            val obj = org.json.JSONObject().apply {
                put("transactionId", tx.transactionId)
                put("userEmail", tx.userEmail)
                put("sectionTitle", tx.sectionTitle)
                put("timestamp", tx.timestamp)
                put("promptTokens", tx.promptTokens)
                put("completionTokens", tx.completionTokens)
                put("totalTokens", tx.totalTokens)
                put("creditsDeducted", tx.creditsDeducted)
                put("modelUsed", tx.modelUsed)
                put("isSuccess", tx.isSuccess)
            }
            array.put(obj)
            // Keep last 100 transactions
            val trimmedArray = org.json.JSONArray()
            val startIdx = Math.max(0, array.length() - 100)
            for (i in startIdx until array.length()) {
                trimmedArray.put(array.getJSONObject(i))
            }
            prefs.edit().putString(KEY_TOKEN_TRANSACTIONS_JSON, trimmedArray.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getTokenTransactions(): List<com.example.model.AiTokenTransaction> {
        val list = mutableListOf<com.example.model.AiTokenTransaction>()
        try {
            val raw = prefs.getString(KEY_TOKEN_TRANSACTIONS_JSON, "[]") ?: "[]"
            val array = org.json.JSONArray(raw)
            for (i in (array.length() - 1) downTo 0) {
                val obj = array.getJSONObject(i)
                list.add(
                    com.example.model.AiTokenTransaction(
                        transactionId = obj.optString("transactionId"),
                        userEmail = obj.optString("userEmail"),
                        sectionTitle = obj.optString("sectionTitle"),
                        timestamp = obj.optLong("timestamp"),
                        promptTokens = obj.optInt("promptTokens"),
                        completionTokens = obj.optInt("completionTokens"),
                        totalTokens = obj.optInt("totalTokens"),
                        creditsDeducted = obj.optInt("creditsDeducted"),
                        modelUsed = obj.optString("modelUsed"),
                        isSuccess = obj.optBoolean("isSuccess", true)
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun recordTelemetryInRegistry(sub: com.example.model.UserAiSubscription) {
        try {
            val raw = prefs.getString(KEY_ALL_SUBSCRIBERS_JSON, "[]") ?: "[]"
            val array = org.json.JSONArray(raw)
            val updatedArray = org.json.JSONArray()
            var found = false
            val currentProfile = getUserProfile()

            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                if (obj.optString("userEmail").equals(sub.userEmail, ignoreCase = true)) {
                    val resolvedName = if (sub.userEmail.equals(currentProfile.email, ignoreCase = true)) currentProfile.displayName else obj.optString("displayName", "Author")
                    val updatedObj = org.json.JSONObject().apply {
                        put("userEmail", sub.userEmail)
                        put("displayName", resolvedName)
                        put("planId", sub.plan.id)
                        put("planTitle", sub.plan.title)
                        put("creditsRemaining", sub.creditsRemaining)
                        put("totalTokensUsed", sub.totalTokensUsed)
                        put("totalGenerationsCount", sub.totalGenerationsCount)
                        put("monthlyRenewalTimestamp", sub.monthlyRenewalTimestamp)
                        put("lastActiveTimestamp", System.currentTimeMillis())
                        put("status", if (sub.userEmail.equals("real.artistry@gmail.com", ignoreCase = true)) "UNLIMITED_SUPERUSER" else if (sub.isActive) "ACTIVE" else "EXPIRED")
                    }
                    updatedArray.put(updatedObj)
                    found = true
                } else {
                    updatedArray.put(obj)
                }
            }

            if (!found) {
                val resolvedName = if (sub.userEmail.equals(currentProfile.email, ignoreCase = true)) currentProfile.displayName else "Author"
                val newObj = org.json.JSONObject().apply {
                    put("userEmail", sub.userEmail)
                    put("displayName", resolvedName)
                    put("planId", sub.plan.id)
                    put("planTitle", sub.plan.title)
                    put("creditsRemaining", sub.creditsRemaining)
                    put("totalTokensUsed", sub.totalTokensUsed)
                    put("totalGenerationsCount", sub.totalGenerationsCount)
                    put("monthlyRenewalTimestamp", sub.monthlyRenewalTimestamp)
                    put("lastActiveTimestamp", System.currentTimeMillis())
                    put("status", if (sub.userEmail.equals("real.artistry@gmail.com", ignoreCase = true)) "UNLIMITED_SUPERUSER" else "ACTIVE")
                }
                updatedArray.put(newObj)
            }
            prefs.edit().putString(KEY_ALL_SUBSCRIBERS_JSON, updatedArray.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getAllSubscribersTelemetry(): List<com.example.model.PaidMemberTelemetry> {
        val list = mutableListOf<com.example.model.PaidMemberTelemetry>()
        val currentProfile = getUserProfile()
        try {
            val raw = prefs.getString(KEY_ALL_SUBSCRIBERS_JSON, "[]") ?: "[]"
            val array = org.json.JSONArray(raw)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val userEmail = obj.optString("userEmail")
                val isCurrent = userEmail.equals(currentProfile.email, ignoreCase = true)
                list.add(
                    com.example.model.PaidMemberTelemetry(
                        userEmail = userEmail,
                        displayName = if (isCurrent) currentProfile.displayName else obj.optString("displayName", "Author"),
                        planId = obj.optString("planId"),
                        planTitle = obj.optString("planTitle", "Novelist Plan"),
                        creditsRemaining = obj.optInt("creditsRemaining"),
                        totalTokensUsed = obj.optLong("totalTokensUsed"),
                        totalGenerationsCount = obj.optInt("totalGenerationsCount"),
                        monthlyRenewalTimestamp = obj.optLong("monthlyRenewalTimestamp"),
                        lastActiveTimestamp = obj.optLong("lastActiveTimestamp"),
                        status = obj.optString("status", "ACTIVE")
                    )
                )
            }

            // Always ensure current user / real.artistry@gmail.com is present with full metrics
            if (list.none { it.userEmail.equals(currentProfile.email, ignoreCase = true) }) {
                val superuserSub = getUserSubscription(currentProfile.email)
                val isEditor = currentProfile.email.equals("real.artistry@gmail.com", ignoreCase = true)
                list.add(
                    0,
                    com.example.model.PaidMemberTelemetry(
                        userEmail = currentProfile.email,
                        displayName = currentProfile.displayName,
                        planId = if (isEditor) com.example.model.SubscriptionPlan.SUPERUSER_UNLIMITED.id else superuserSub.plan.id,
                        planTitle = if (isEditor) com.example.model.SubscriptionPlan.SUPERUSER_UNLIMITED.title else superuserSub.plan.title,
                        creditsRemaining = superuserSub.creditsRemaining,
                        totalTokensUsed = superuserSub.totalTokensUsed,
                        totalGenerationsCount = superuserSub.totalGenerationsCount,
                        monthlyRenewalTimestamp = superuserSub.monthlyRenewalTimestamp,
                        lastActiveTimestamp = System.currentTimeMillis(),
                        status = if (isEditor) "UNLIMITED_SUPERUSER" else "ACTIVE"
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun adminGrantBonusCredits(targetEmail: String, bonusCredits: Int) {
        if (targetEmail.equals(getUserProfile().email, ignoreCase = true)) {
            val current = getUserSubscription(targetEmail)
            saveUserSubscription(current.copy(creditsRemaining = current.creditsRemaining + bonusCredits))
        }
    }
}
