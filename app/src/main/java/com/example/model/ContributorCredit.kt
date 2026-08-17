package com.example.model

import org.json.JSONArray
import org.json.JSONObject

data class ContributorCredit(
    val penName: String,
    val email: String,
    val role: String, // "Editor" or "Contributor"
    val wordsContributed: Int,
    val commitTimestamp: Long = System.currentTimeMillis(),
    val chapterTitle: String = ""
) {
    fun toJsonObject(): JSONObject {
        return JSONObject().apply {
            put("penName", penName)
            put("email", email)
            put("role", role)
            put("wordsContributed", wordsContributed)
            put("commitTimestamp", commitTimestamp)
            put("chapterTitle", chapterTitle)
        }
    }

    companion object {
        fun fromJsonObject(obj: JSONObject): ContributorCredit {
            return ContributorCredit(
                penName = obj.optString("penName", "Unknown Contributor"),
                email = obj.optString("email", ""),
                role = obj.optString("role", "Contributor"),
                wordsContributed = obj.optInt("wordsContributed", 0),
                commitTimestamp = obj.optLong("commitTimestamp", System.currentTimeMillis()),
                chapterTitle = obj.optString("chapterTitle", "")
            )
        }

        fun parseListFromJson(jsonString: String?): List<ContributorCredit> {
            if (jsonString.isNullOrBlank() || jsonString == "[]") return emptyList()
            return try {
                val array = JSONArray(jsonString)
                val list = mutableListOf<ContributorCredit>()
                for (i in 0 until array.length()) {
                    list.add(fromJsonObject(array.getJSONObject(i)))
                }
                list
            } catch (e: Exception) {
                emptyList()
            }
        }

        fun listToJsonString(list: List<ContributorCredit>): String {
            val array = JSONArray()
            list.forEach { array.put(it.toJsonObject()) }
            return array.toString()
        }
    }
}
