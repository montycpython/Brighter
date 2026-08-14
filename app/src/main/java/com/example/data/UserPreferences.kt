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
    }
}
