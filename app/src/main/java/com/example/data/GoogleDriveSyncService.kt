package com.example.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.model.GlobalBookIndexEntry
import com.example.model.ManuscriptEntity
import com.example.model.SectionEntity
import com.example.model.ServerlessMailMessage
import com.example.model.SuspendedUserEntry
import com.example.model.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class GoogleDriveSyncService(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("bwriter_drive_storage", Context.MODE_PRIVATE)

    companion object {
        const val EDITOR_IN_CHIEF_EMAIL = "real.artistry@gmail.com"
        const val MASTER_SHARED_DRIVE_NAME = "Bwriter_Master_Drive"
        const val GLOBAL_BOOK_INDEX_FILE = "Global_Book_Index.json"
        const val SUSPENDED_USERS_FILE = "suspended_users.json"
        const val MAILBOX_DIR = "Mailboxes"

        private const val PREF_GLOBAL_INDEX = "global_book_index_cache"
        private const val PREF_SUSPENDED_USERS = "suspended_users_cache"
        private const val PREF_MAILBOX_PREFIX = "mailbox_"
    }

    init {
        initializeSampleNetworkIfEmpty()
    }

    private fun initializeSampleNetworkIfEmpty() {
        val currentIndex = prefs.getString(PREF_GLOBAL_INDEX, null)
        val userProfile = UserPreferences(context).getUserProfile()
        val userDisplayName = userProfile.displayName.ifBlank { "Author" }

        if (currentIndex.isNullOrBlank()) {
            val initialList = listOf(
                GlobalBookIndexEntry(
                    fileId = "drv_sample_master_101",
                    manuscriptId = 1L,
                    title = "The Obsidian Quill",
                    subtitle = "A Chronicle of the Chicago Printmasters",
                    authorName = userDisplayName,
                    authorEmail = userProfile.email,
                    workType = "NOVEL",
                    wordCount = 4520,
                    totalLeaves = 18,
                    lastSyncedTimestamp = System.currentTimeMillis() - 3600000 * 2,
                    isPublicInCommunity = true,
                    version = "1.2",
                    driveFileUrl = "https://drive.google.com/file/d/drv_sample_master_101/view",
                    sharedWithEditorInChief = true
                ),
                GlobalBookIndexEntry(
                    fileId = "drv_sample_carrie_202",
                    manuscriptId = 2L,
                    title = "Chicago Nights: The Gilded Foundry",
                    subtitle = "Industrial Typecasting on the Lakefront",
                    authorName = "Eleanor Vance",
                    authorEmail = "eleanor.types@bwriter.press",
                    workType = "NOVEL",
                    wordCount = 7890,
                    totalLeaves = 32,
                    lastSyncedTimestamp = System.currentTimeMillis() - 3600000 * 5,
                    isPublicInCommunity = true,
                    version = "1.0",
                    driveFileUrl = "https://drive.google.com/file/d/drv_sample_carrie_202/view",
                    sharedWithEditorInChief = true
                ),
                GlobalBookIndexEntry(
                    fileId = "drv_sample_private_303",
                    manuscriptId = 3L,
                    title = "Confidential Journal: Chicago Proofs",
                    subtitle = "Private Discrepancy Ledger",
                    authorName = "Silas Thorne",
                    authorEmail = "silas.thorne@artisan.press",
                    workType = "MEMOIR",
                    wordCount = 3100,
                    totalLeaves = 12,
                    lastSyncedTimestamp = System.currentTimeMillis() - 3600000 * 12,
                    isPublicInCommunity = false, // HIDDEN: strictly private to Silas & Editor in Chief
                    version = "0.9",
                    driveFileUrl = "https://drive.google.com/file/d/drv_sample_private_303/view",
                    sharedWithEditorInChief = true
                )
            )
            saveGlobalIndexToStorage(initialList)
        }

        // Initialize sample mailbox for real.artistry@gmail.com
        val existingMail = prefs.getString("${PREF_MAILBOX_PREFIX}$EDITOR_IN_CHIEF_EMAIL", null)
        if (existingMail.isNullOrBlank()) {
            val initialMessages = listOf(
                ServerlessMailMessage(
                    recipientEmail = EDITOR_IN_CHIEF_EMAIL,
                    senderEmail = "eleanor.types@bwriter.press",
                    senderName = "Eleanor Vance",
                    subject = "Submitted Chicago Nights Chapter 3 for Chicago §13 Review",
                    body = "Greetings Editor in Chief,\n\nI have synced the updated manuscript to my personal Drive and automatically granted your account writer permissions. The block quotations in §3 have been adjusted to 0.5-inch CMOS standards.",
                    timestamp = System.currentTimeMillis() - 3600000 * 4,
                    isRead = false,
                    manuscriptTitle = "Chicago Nights: The Gilded Foundry",
                    messageType = "EDITORIAL_REVISION"
                ),
                ServerlessMailMessage(
                    recipientEmail = EDITOR_IN_CHIEF_EMAIL,
                    senderEmail = "system@bwriter.press",
                    senderName = "Master Shared Drive Bot",
                    subject = "Master Shared Drive Global Index Online",
                    body = "The decentralized Google Drive serverless storage index is initialized. Auto-sharing hooks and suspension kill-switch channels are operational.",
                    timestamp = System.currentTimeMillis() - 3600000 * 24,
                    isRead = true,
                    messageType = "GOVERNANCE_ALERT"
                )
            )
            saveMailboxToStorage(EDITOR_IN_CHIEF_EMAIL, initialMessages)
        }
    }

    /**
     * Check if a user is currently suspended by the Editor in Chief.
     */
    suspend fun checkUserSuspension(email: String): SuspendedUserEntry? = withContext(Dispatchers.IO) {
        val list = getSuspendedUsersFromStorage()
        list.firstOrNull { it.email.equals(email.trim(), ignoreCase = true) && it.isLockedOut }
    }

    /**
     * Sync local JSON manuscript to Google Drive and update Global_Book_Index.json.
     * Auto-shares with real.artistry@gmail.com hook.
     */
    suspend fun syncManuscriptToDrive(
        manuscript: ManuscriptEntity,
        sections: List<SectionEntity>,
        currentUser: UserProfile,
        isPublicInCommunity: Boolean
    ): Result<GlobalBookIndexEntry> = withContext(Dispatchers.IO) {
        try {
            // 1. Check if user is suspended
            val suspension = checkUserSuspension(currentUser.email)
            if (suspension != null) {
                return@withContext Result.failure(Exception("Account suspended: ${suspension.reason}"))
            }

            // 2. Generate JSON payload
            val resolvedAuthorName = when {
                manuscript.authorPenName.isNotBlank() -> manuscript.authorPenName.trim()
                manuscript.authorName.isNotBlank() -> manuscript.authorName.trim()
                else -> currentUser.displayName
            }

            val manuscriptJson = JSONObject().apply {
                put("id", manuscript.id)
                put("title", manuscript.title)
                put("subtitle", manuscript.subtitle)
                put("authorName", resolvedAuthorName)
                put("authorPenName", manuscript.authorPenName)
                put("authorEmail", currentUser.email)
                put("workType", manuscript.workType.name)
                put("trimSize", manuscript.targetPageSize)
                put("description", manuscript.subtitle)
                put("syncedTimestamp", System.currentTimeMillis())
                put("isPublicInCommunity", isPublicInCommunity)

                val sectionsArray = JSONArray()
                sections.forEach { sec ->
                    val secObj = JSONObject().apply {
                        put("id", sec.id)
                        put("title", sec.title)
                        put("subtitle", sec.subtitle)
                        put("matterType", sec.matterType.name)
                        put("sectionType", sec.sectionType.name)
                        put("content", sec.content)
                        put("status", sec.status.name)
                        put("orderIndex", sec.orderIndex)
                        put("aiDraftPrompt", sec.aiDraftPrompt)
                    }
                    sectionsArray.put(secObj)
                }
                put("sections", sectionsArray)
            }

            // 3. Simulate / Perform Drive Upload & Auto-Grant Permission
            val fileId = "drv_book_${manuscript.id}_${System.currentTimeMillis().toString().takeLast(6)}"
            val driveUrl = "https://drive.google.com/file/d/$fileId/view"

            val totalWords = sections.sumOf { it.content.split(Regex("\\s+")).filter { w -> w.isNotBlank() }.size }
            val estimatedLeaves = (totalWords / 250).coerceAtLeast(1)

            val indexEntry = GlobalBookIndexEntry(
                fileId = fileId,
                manuscriptId = manuscript.id,
                title = manuscript.title,
                subtitle = manuscript.subtitle,
                authorName = resolvedAuthorName,
                authorEmail = currentUser.email,
                workType = manuscript.workType.name,
                wordCount = totalWords,
                totalLeaves = estimatedLeaves,
                lastSyncedTimestamp = System.currentTimeMillis(),
                isPublicInCommunity = isPublicInCommunity,
                version = "1.0",
                driveFileUrl = driveUrl,
                sharedWithEditorInChief = true // Auto-shared with real.artistry@gmail.com
            )

            // 4. Update Global_Book_Index.json
            val currentIndex = getGlobalIndexFromStorage().toMutableList()
            val existingIdx = currentIndex.indexOfFirst { it.manuscriptId == manuscript.id && it.authorEmail.equals(currentUser.email, ignoreCase = true) }
            if (existingIdx >= 0) {
                currentIndex[existingIdx] = indexEntry
            } else {
                currentIndex.add(0, indexEntry)
            }
            saveGlobalIndexToStorage(currentIndex)

            // 5. If not the Editor in Chief, send an auto notification to Editor in Chief's mailbox
            if (!currentUser.email.equals(EDITOR_IN_CHIEF_EMAIL, ignoreCase = true)) {
                sendServerlessMail(
                    ServerlessMailMessage(
                        recipientEmail = EDITOR_IN_CHIEF_EMAIL,
                        senderEmail = currentUser.email,
                        senderName = currentUser.displayName,
                        subject = "Manuscript Synced: ${manuscript.title}",
                        body = "Author ${currentUser.displayName} (${currentUser.email}) synced '${manuscript.title}' to Google Drive.\n\nVisibility: ${if (isPublicInCommunity) "Public Community" else "Private / Hidden"}\nWord Count: $totalWords words\nFile ID: $fileId",
                        manuscriptId = manuscript.id,
                        manuscriptTitle = manuscript.title,
                        messageType = "EDITORIAL_REVISION"
                    )
                )
            }

            Log.i("GoogleDriveSync", "Successfully synced manuscript ${manuscript.title} to Google Drive file $fileId")
            Result.success(indexEntry)
        } catch (e: Exception) {
            Log.e("GoogleDriveSync", "Sync failed", e)
            Result.failure(e)
        }
    }

    /**
     * Fetches the Global Book Index.
     * If user is real.artistry@gmail.com (Editor in Chief): sees ALL books (public and hidden).
     * If user is regular author: sees public books + their own hidden books.
     */
    suspend fun fetchGlobalBookIndex(currentUser: UserProfile): Result<List<GlobalBookIndexEntry>> = withContext(Dispatchers.IO) {
        try {
            val allEntries = getGlobalIndexFromStorage()
            val isEditorInChief = currentUser.email.equals(EDITOR_IN_CHIEF_EMAIL, ignoreCase = true)

            // Dynamically synchronize the authorName for current user's manuscripts to match their active pen name / legal name
            val synchronizedEntries = allEntries.map { entry ->
                if (entry.authorEmail.equals(currentUser.email, ignoreCase = true) || entry.authorName == "Aldus Manutius") {
                    entry.copy(authorName = currentUser.displayName)
                } else {
                    entry
                }
            }

            val filtered = if (isEditorInChief) {
                synchronizedEntries
            } else {
                synchronizedEntries.filter { entry ->
                    entry.isPublicInCommunity || entry.authorEmail.equals(currentUser.email, ignoreCase = true)
                }
            }
            Result.success(filtered)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Admin God-Mode: Suspend a user.
     */
    suspend fun suspendUser(
        adminEmail: String,
        targetEmail: String,
        reason: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!adminEmail.equals(EDITOR_IN_CHIEF_EMAIL, ignoreCase = true)) {
                return@withContext Result.failure(IllegalAccessException("Only the Editor in Chief ($EDITOR_IN_CHIEF_EMAIL) can suspend users."))
            }

            val suspended = getSuspendedUsersFromStorage().toMutableList()
            suspended.removeAll { it.email.equals(targetEmail.trim(), ignoreCase = true) }
            suspended.add(
                SuspendedUserEntry(
                    email = targetEmail.trim(),
                    reason = reason,
                    suspendedBy = adminEmail,
                    isLockedOut = true
                )
            )
            saveSuspendedUsersToStorage(suspended)

            // Send notification message to the user's mailbox
            sendServerlessMail(
                ServerlessMailMessage(
                    recipientEmail = targetEmail.trim(),
                    senderEmail = EDITOR_IN_CHIEF_EMAIL,
                    senderName = "Editor in Chief",
                    subject = "Account Suspended: Governance Notice",
                    body = "Your account has been suspended by the Editor in Chief.\n\nReason: $reason\n\nAll editing and network capabilities are locked until further review.",
                    messageType = "GOVERNANCE_ALERT"
                )
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Admin God-Mode: Unsuspend a user.
     */
    suspend fun unsuspendUser(adminEmail: String, targetEmail: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!adminEmail.equals(EDITOR_IN_CHIEF_EMAIL, ignoreCase = true)) {
                return@withContext Result.failure(IllegalAccessException("Only the Editor in Chief can unsuspend users."))
            }

            val suspended = getSuspendedUsersFromStorage().toMutableList()
            suspended.removeAll { it.email.equals(targetEmail.trim(), ignoreCase = true) }
            saveSuspendedUsersToStorage(suspended)

            // Send restoration notice
            sendServerlessMail(
                ServerlessMailMessage(
                    recipientEmail = targetEmail.trim(),
                    senderEmail = EDITOR_IN_CHIEF_EMAIL,
                    senderName = "Editor in Chief",
                    subject = "Account Restored",
                    body = "Your account access has been fully restored by the Editor in Chief. You may now sync and edit your manuscripts normally.",
                    messageType = "GOVERNANCE_ALERT"
                )
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Admin God-Mode: Revoke / Remove a manuscript from the master index & drive.
     */
    suspend fun revokeManuscriptAccess(adminEmail: String, fileId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!adminEmail.equals(EDITOR_IN_CHIEF_EMAIL, ignoreCase = true)) {
                return@withContext Result.failure(IllegalAccessException("Only the Editor in Chief can revoke drive access."))
            }

            val index = getGlobalIndexFromStorage().toMutableList()
            val removed = index.firstOrNull { it.fileId == fileId }
            index.removeAll { it.fileId == fileId }
            saveGlobalIndexToStorage(index)

            if (removed != null) {
                sendServerlessMail(
                    ServerlessMailMessage(
                        recipientEmail = removed.authorEmail,
                        senderEmail = EDITOR_IN_CHIEF_EMAIL,
                        senderName = "Editor in Chief",
                        subject = "Manuscript Access Revoked: ${removed.title}",
                        body = "The shared drive access for '${removed.title}' (File ID: $fileId) has been revoked from the global registry by the Editor in Chief.",
                        messageType = "GOVERNANCE_ALERT"
                    )
                )
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Send serverless message to a user's mailbox (or broadcast to all).
     */
    suspend fun sendServerlessMail(message: ServerlessMailMessage): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (message.recipientEmail.equals("ALL_AUTHORS", ignoreCase = true)) {
                // Broadcast to all known authors in the global index
                val allAuthors = getGlobalIndexFromStorage().map { it.authorEmail }.distinct()
                allAuthors.forEach { authorEmail ->
                    val userMail = getMailboxFromStorage(authorEmail).toMutableList()
                    userMail.add(0, message.copy(id = java.util.UUID.randomUUID().toString(), recipientEmail = authorEmail))
                    saveMailboxToStorage(authorEmail, userMail)
                }
            } else {
                val userMail = getMailboxFromStorage(message.recipientEmail).toMutableList()
                userMail.add(0, message)
                saveMailboxToStorage(message.recipientEmail, userMail)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetch user's mailbox messages.
     */
    suspend fun fetchMailboxMessages(userEmail: String): Result<List<ServerlessMailMessage>> = withContext(Dispatchers.IO) {
        try {
            val mail = getMailboxFromStorage(userEmail)
            Result.success(mail)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Mark a message as read.
     */
    suspend fun markMailAsRead(userEmail: String, messageId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val mail = getMailboxFromStorage(userEmail).toMutableList()
            val idx = mail.indexOfFirst { it.id == messageId }
            if (idx >= 0) {
                mail[idx] = mail[idx].copy(isRead = true)
                saveMailboxToStorage(userEmail, mail)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==========================================
    // Internal JSON Serialization Helpers
    // ==========================================

    private fun getGlobalIndexFromStorage(): List<GlobalBookIndexEntry> {
        val raw = prefs.getString(PREF_GLOBAL_INDEX, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<GlobalBookIndexEntry>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    GlobalBookIndexEntry(
                        fileId = obj.optString("fileId", "drv_${System.currentTimeMillis()}"),
                        manuscriptId = obj.optLong("manuscriptId", 0L),
                        title = obj.optString("title", "Untitled"),
                        subtitle = obj.optString("subtitle", ""),
                        authorName = obj.optString("authorName", "Unknown"),
                        authorEmail = obj.optString("authorEmail", "author@bwriter.press"),
                        workType = obj.optString("workType", "NOVEL"),
                        wordCount = obj.optInt("wordCount", 0),
                        totalLeaves = obj.optInt("totalLeaves", 0),
                        lastSyncedTimestamp = obj.optLong("lastSyncedTimestamp", System.currentTimeMillis()),
                        isPublicInCommunity = obj.optBoolean("isPublicInCommunity", true),
                        version = obj.optString("version", "1.0"),
                        driveFileUrl = obj.optString("driveFileUrl", ""),
                        sharedWithEditorInChief = obj.optBoolean("sharedWithEditorInChief", true)
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveGlobalIndexToStorage(list: List<GlobalBookIndexEntry>) {
        val arr = JSONArray()
        list.forEach { entry ->
            val obj = JSONObject().apply {
                put("fileId", entry.fileId)
                put("manuscriptId", entry.manuscriptId)
                put("title", entry.title)
                put("subtitle", entry.subtitle)
                put("authorName", entry.authorName)
                put("authorEmail", entry.authorEmail)
                put("workType", entry.workType)
                put("wordCount", entry.wordCount)
                put("totalLeaves", entry.totalLeaves)
                put("lastSyncedTimestamp", entry.lastSyncedTimestamp)
                put("isPublicInCommunity", entry.isPublicInCommunity)
                put("version", entry.version)
                put("driveFileUrl", entry.driveFileUrl)
                put("sharedWithEditorInChief", entry.sharedWithEditorInChief)
            }
            arr.put(obj)
        }
        prefs.edit().putString(PREF_GLOBAL_INDEX, arr.toString()).apply()
    }

    fun getSuspendedUsersFromStorage(): List<SuspendedUserEntry> {
        val raw = prefs.getString(PREF_SUSPENDED_USERS, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<SuspendedUserEntry>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    SuspendedUserEntry(
                        email = obj.optString("email", ""),
                        suspendedAt = obj.optLong("suspendedAt", System.currentTimeMillis()),
                        reason = obj.optString("reason", "Suspended by Editor in Chief"),
                        suspendedBy = obj.optString("suspendedBy", EDITOR_IN_CHIEF_EMAIL),
                        isLockedOut = obj.optBoolean("isLockedOut", true)
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveSuspendedUsersToStorage(list: List<SuspendedUserEntry>) {
        val arr = JSONArray()
        list.forEach { entry ->
            val obj = JSONObject().apply {
                put("email", entry.email)
                put("suspendedAt", entry.suspendedAt)
                put("reason", entry.reason)
                put("suspendedBy", entry.suspendedBy)
                put("isLockedOut", entry.isLockedOut)
            }
            arr.put(obj)
        }
        prefs.edit().putString(PREF_SUSPENDED_USERS, arr.toString()).apply()
    }

    private fun getMailboxFromStorage(userEmail: String): List<ServerlessMailMessage> {
        val key = "${PREF_MAILBOX_PREFIX}${userEmail.trim().lowercase()}"
        val raw = prefs.getString(key, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<ServerlessMailMessage>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    ServerlessMailMessage(
                        id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                        recipientEmail = obj.optString("recipientEmail", userEmail),
                        senderEmail = obj.optString("senderEmail", EDITOR_IN_CHIEF_EMAIL),
                        senderName = obj.optString("senderName", "Editor in Chief"),
                        subject = obj.optString("subject", "No Subject"),
                        body = obj.optString("body", ""),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        isRead = obj.optBoolean("isRead", false),
                        manuscriptId = if (obj.has("manuscriptId")) obj.optLong("manuscriptId") else null,
                        manuscriptTitle = if (obj.has("manuscriptTitle")) obj.optString("manuscriptTitle") else null,
                        messageType = obj.optString("messageType", "DIRECT_MESSAGE")
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveMailboxToStorage(userEmail: String, list: List<ServerlessMailMessage>) {
        val key = "${PREF_MAILBOX_PREFIX}${userEmail.trim().lowercase()}"
        val arr = JSONArray()
        list.forEach { msg ->
            val obj = JSONObject().apply {
                put("id", msg.id)
                put("recipientEmail", msg.recipientEmail)
                put("senderEmail", msg.senderEmail)
                put("senderName", msg.senderName)
                put("subject", msg.subject)
                put("body", msg.body)
                put("timestamp", msg.timestamp)
                put("isRead", msg.isRead)
                if (msg.manuscriptId != null) put("manuscriptId", msg.manuscriptId)
                if (msg.manuscriptTitle != null) put("manuscriptTitle", msg.manuscriptTitle)
                put("messageType", msg.messageType)
            }
            arr.put(obj)
        }
        prefs.edit().putString(key, arr.toString()).apply()
    }
}
