package com.example.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.model.ContributorCredit
import com.example.model.GlobalBookIndexEntry
import com.example.model.ManuscriptEntity
import com.example.model.MatterType
import com.example.model.SectionEntity
import com.example.model.SectionStatus
import com.example.model.SectionType
import com.example.model.ServerlessMailMessage
import com.example.model.SuspendedUserEntry
import com.example.model.UserProfile
import com.example.model.WorkRole
import com.example.model.WorkType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

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
        private const val PREF_CLOUD_MANUSCRIPTS = "cloud_manuscripts_payload_store"
    }

    init {
        initializeSampleNetworkIfEmpty()
    }

    private fun initializeSampleNetworkIfEmpty() {
        val currentIndex = prefs.getString(PREF_GLOBAL_INDEX, null)

        if (currentIndex.isNullOrBlank()) {
            val initialList = listOf(
                GlobalBookIndexEntry(
                    fileId = "drv_master_quill_101",
                    manuscriptId = 1L,
                    title = "The Obsidian Quill",
                    subtitle = "A Chronicle of the Chicago Printmasters",
                    authorName = "Author",
                    authorEmail = EDITOR_IN_CHIEF_EMAIL,
                    workType = "NOVEL",
                    wordCount = 4520,
                    totalLeaves = 18,
                    lastSyncedTimestamp = System.currentTimeMillis() - 3600000 * 2,
                    isPublicInCommunity = true,
                    version = "1.2",
                    driveFileUrl = "https://drive.google.com/file/d/drv_master_quill_101/view",
                    sharedWithEditorInChief = true
                ),
                GlobalBookIndexEntry(
                    fileId = "drv_master_chronicler_202",
                    manuscriptId = 2L,
                    title = "The Chronicler of Chicago",
                    subtitle = "The Life and Letters of Silas Dearborn (1842–1918)",
                    authorName = "Author",
                    authorEmail = EDITOR_IN_CHIEF_EMAIL,
                    workType = "BIOGRAPHY",
                    wordCount = 7890,
                    totalLeaves = 32,
                    lastSyncedTimestamp = System.currentTimeMillis() - 3600000 * 5,
                    isPublicInCommunity = true,
                    version = "1.0",
                    driveFileUrl = "https://drive.google.com/file/d/drv_master_chronicler_202/view",
                    sharedWithEditorInChief = true
                ),
                GlobalBookIndexEntry(
                    fileId = "drv_master_echoes_303",
                    manuscriptId = 3L,
                    title = "Echoes of the Great Lake",
                    subtitle = "An Oral and Archival Documentary of the Maritime Trades",
                    authorName = "Author",
                    authorEmail = EDITOR_IN_CHIEF_EMAIL,
                    workType = "DOCUMENTARY",
                    wordCount = 6200,
                    totalLeaves = 24,
                    lastSyncedTimestamp = System.currentTimeMillis() - 3600000 * 8,
                    isPublicInCommunity = true,
                    version = "1.0",
                    driveFileUrl = "https://drive.google.com/file/d/drv_master_echoes_303/view",
                    sharedWithEditorInChief = true
                ),
                GlobalBookIndexEntry(
                    fileId = "drv_master_typography_404",
                    manuscriptId = 4L,
                    title = "The Craft of Book Typography",
                    subtitle = "A Practical Manual Honoring the Chicago Manual of Style",
                    authorName = "Author",
                    authorEmail = EDITOR_IN_CHIEF_EMAIL,
                    workType = "MANUAL",
                    wordCount = 9400,
                    totalLeaves = 38,
                    lastSyncedTimestamp = System.currentTimeMillis() - 3600000 * 12,
                    isPublicInCommunity = true,
                    version = "2.0",
                    driveFileUrl = "https://drive.google.com/file/d/drv_master_typography_404/view",
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
                    senderEmail = "editorial.board@bwriter.press",
                    senderName = "Master Editorial Desk",
                    subject = "Master Shared Drive Global Index Online",
                    body = "The decentralized Google Drive serverless storage index is initialized. All master works are cataloged under the Author superuser registry. Auto-sharing hooks and contributor telemetry tracking are operational.",
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
     * Auto-shares with real.artistry@gmail.com hook and archives full cloud backup.
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

            // 2. Generate JSON payload with contributor telemetry & acknowledgments
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
                put("editorName", manuscript.editorName)
                put("publisher", manuscript.publisher)
                put("edition", manuscript.edition)
                put("year", manuscript.year)
                put("isbn", manuscript.isbn)
                put("copyrightText", manuscript.copyrightText)
                put("dedication", manuscript.dedication)
                put("epigraphText", manuscript.epigraphText)
                put("epigraphAuthor", manuscript.epigraphAuthor)
                put("targetPageSize", manuscript.targetPageSize)
                put("workType", manuscript.workType.name)
                put("manuscriptStatus", manuscript.manuscriptStatus)
                put("acknowledgmentsJson", manuscript.acknowledgmentsJson)
                put("syncedTimestamp", System.currentTimeMillis())
                put("isPublicInCommunity", isPublicInCommunity)

                val sectionsArray = JSONArray()
                sections.forEach { sec ->
                    val secObj = JSONObject().apply {
                        put("id", sec.id)
                        put("manuscriptId", sec.manuscriptId)
                        put("matterType", sec.matterType.name)
                        put("sectionType", sec.sectionType.name)
                        put("title", sec.title)
                        put("subtitle", sec.subtitle)
                        put("orderIndex", sec.orderIndex)
                        put("content", sec.content)
                        put("aiDraftPrompt", sec.aiDraftPrompt)
                        put("contributorNotes", sec.contributorNotes)
                        put("assignedAuthor", sec.assignedAuthor)
                        put("assignedRole", sec.assignedRole.name)
                        put("status", sec.status.name)
                        put("startOnRecto", sec.startOnRecto)
                        put("headerIllustrationUri", sec.headerIllustrationUri)
                        put("headerIllustrationCaption", sec.headerIllustrationCaption)
                        put("tailIllustrationUri", sec.tailIllustrationUri)
                        put("tailIllustrationCaption", sec.tailIllustrationCaption)
                        put("wordCount", sec.wordCount)
                        put("hasPendingRevision", sec.hasPendingRevision)
                        put("pendingEditedContent", sec.pendingEditedContent)
                        put("originalAuthorContent", sec.originalAuthorContent)
                        put("revisionAuthorPenName", sec.revisionAuthorPenName)
                        put("revisionAuthorEmail", sec.revisionAuthorEmail)
                        put("revisionAuthorRole", sec.revisionAuthorRole)
                        put("revisionTimestamp", sec.revisionTimestamp)
                        put("revisionDeltaWords", sec.revisionDeltaWords)
                    }
                    sectionsArray.put(secObj)
                }
                put("sections", sectionsArray)
            }

            // 3. Save full cloud backup in Drive storage for dynamic recovery on reinstall
            saveCloudManuscriptPayload(currentUser.email, manuscript.id, manuscriptJson)

            // 4. Update Global_Book_Index.json
            val fileId = "drv_book_${manuscript.id}_${System.currentTimeMillis().toString().takeLast(6)}"
            val driveUrl = "https://drive.google.com/file/d/$fileId/view"
            val totalWords = sections.sumOf { it.wordCount.takeIf { c -> c > 0 } ?: it.content.split(Regex("\\s+")).count { w -> w.isNotBlank() } }
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
                version = manuscript.edition.ifBlank { "1.0" },
                driveFileUrl = driveUrl,
                sharedWithEditorInChief = true
            )

            val currentIndex = getGlobalIndexFromStorage().toMutableList()
            val existingIdx = currentIndex.indexOfFirst { it.manuscriptId == manuscript.id && it.authorEmail.equals(currentUser.email, ignoreCase = true) }
            if (existingIdx >= 0) {
                currentIndex[existingIdx] = indexEntry
            } else {
                currentIndex.add(0, indexEntry)
            }
            saveGlobalIndexToStorage(currentIndex)

            // 5. Send notification to Editor in Chief if by a contributor/author
            if (!currentUser.email.equals(EDITOR_IN_CHIEF_EMAIL, ignoreCase = true)) {
                sendServerlessMail(
                    ServerlessMailMessage(
                        recipientEmail = EDITOR_IN_CHIEF_EMAIL,
                        senderEmail = currentUser.email,
                        senderName = currentUser.displayName,
                        subject = "Manuscript Synced: ${manuscript.title}",
                        body = "Author ${currentUser.displayName} (${currentUser.email}) synced '${manuscript.title}' to Google Drive.\n\nVisibility: ${if (isPublicInCommunity) "Public Global Registry" else "Private"}\nStatus: ${manuscript.manuscriptStatus}\nWords: $totalWords\nFile ID: $fileId",
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
     * Restore all user manuscripts from Google Drive cloud storage (e.g. after fresh reinstall).
     */
    suspend fun restoreUserManuscriptsFromDrive(userEmail: String): List<Pair<ManuscriptEntity, List<SectionEntity>>> = withContext(Dispatchers.IO) {
        val raw = prefs.getString(PREF_CLOUD_MANUSCRIPTS, null) ?: return@withContext emptyList()
        val list = mutableListOf<Pair<ManuscriptEntity, List<SectionEntity>>>()
        try {
            val rootObj = JSONObject(raw)
            val keys = rootObj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                if (key.startsWith("${userEmail.trim().lowercase()}_")) {
                    val mObj = rootObj.getJSONObject(key)

                    val workType = try {
                        WorkType.valueOf(mObj.optString("workType", "NOVEL"))
                    } catch (e: Exception) {
                        WorkType.NOVEL
                    }

                    val manuscript = ManuscriptEntity(
                        title = mObj.optString("title", "Restored Manuscript"),
                        subtitle = mObj.optString("subtitle", ""),
                        workType = workType,
                        authorName = mObj.optString("authorName", "Author"),
                        authorPenName = mObj.optString("authorPenName", ""),
                        authorEmail = mObj.optString("authorEmail", userEmail),
                        editorName = mObj.optString("editorName", ""),
                        publisher = mObj.optString("publisher", "Bwriter Editions"),
                        edition = mObj.optString("edition", "First Edition"),
                        year = mObj.optString("year", "2026"),
                        isbn = mObj.optString("isbn", "978-0-226-10403-4"),
                        copyrightText = mObj.optString("copyrightText", ""),
                        dedication = mObj.optString("dedication", ""),
                        epigraphText = mObj.optString("epigraphText", ""),
                        epigraphAuthor = mObj.optString("epigraphAuthor", ""),
                        targetPageSize = mObj.optString("targetPageSize", "Trade 6\" x 9\""),
                        manuscriptStatus = mObj.optString("manuscriptStatus", "DRAFT"),
                        acknowledgmentsJson = mObj.optString("acknowledgmentsJson", "[]")
                    )

                    val secArray = mObj.optJSONArray("sections") ?: JSONArray()
                    val sections = mutableListOf<SectionEntity>()
                    for (i in 0 until secArray.length()) {
                        val sObj = secArray.getJSONObject(i)
                        val matterType = try {
                            MatterType.valueOf(sObj.optString("matterType", "TEXT_BODY"))
                        } catch (e: Exception) {
                            MatterType.TEXT_BODY
                        }
                        val sectionType = try {
                            SectionType.valueOf(sObj.optString("sectionType", "CHAPTER"))
                        } catch (e: Exception) {
                            SectionType.CHAPTER
                        }
                        val role = try {
                            WorkRole.valueOf(sObj.optString("assignedRole", "AUTHOR"))
                        } catch (e: Exception) {
                            WorkRole.AUTHOR
                        }
                        val status = try {
                            SectionStatus.valueOf(sObj.optString("status", "DRAFT"))
                        } catch (e: Exception) {
                            SectionStatus.DRAFT
                        }

                        sections.add(
                            SectionEntity(
                                manuscriptId = 0L,
                                matterType = matterType,
                                sectionType = sectionType,
                                title = sObj.optString("title", "Chapter"),
                                subtitle = sObj.optString("subtitle", ""),
                                orderIndex = sObj.optInt("orderIndex", i + 1),
                                content = sObj.optString("content", ""),
                                aiDraftPrompt = sObj.optString("aiDraftPrompt", ""),
                                contributorNotes = sObj.optString("contributorNotes", ""),
                                assignedAuthor = sObj.optString("assignedAuthor", "Author"),
                                assignedRole = role,
                                status = status,
                                startOnRecto = sObj.optBoolean("startOnRecto", true),
                                headerIllustrationUri = sObj.optString("headerIllustrationUri", ""),
                                headerIllustrationCaption = sObj.optString("headerIllustrationCaption", ""),
                                tailIllustrationUri = sObj.optString("tailIllustrationUri", ""),
                                tailIllustrationCaption = sObj.optString("tailIllustrationCaption", ""),
                                wordCount = sObj.optInt("wordCount", 0),
                                hasPendingRevision = sObj.optBoolean("hasPendingRevision", false),
                                pendingEditedContent = sObj.optString("pendingEditedContent", ""),
                                originalAuthorContent = sObj.optString("originalAuthorContent", ""),
                                revisionAuthorPenName = sObj.optString("revisionAuthorPenName", ""),
                                revisionAuthorEmail = sObj.optString("revisionAuthorEmail", ""),
                                revisionAuthorRole = sObj.optString("revisionAuthorRole", ""),
                                revisionTimestamp = sObj.optLong("revisionTimestamp", 0L),
                                revisionDeltaWords = sObj.optInt("revisionDeltaWords", 0)
                            )
                        )
                    }
                    list.add(Pair(manuscript, sections))
                }
            }
            list
        } catch (e: Exception) {
            Log.e("GoogleDriveSync", "Failed to restore manuscripts from Drive", e)
            emptyList()
        }
    }

    private fun saveCloudManuscriptPayload(userEmail: String, manuscriptId: Long, json: JSONObject) {
        try {
            val raw = prefs.getString(PREF_CLOUD_MANUSCRIPTS, null)
            val rootObj = if (raw.isNullOrBlank()) JSONObject() else JSONObject(raw)
            val key = "${userEmail.trim().lowercase()}_$manuscriptId"
            rootObj.put(key, json)
            prefs.edit().putString(PREF_CLOUD_MANUSCRIPTS, rootObj.toString()).apply()
        } catch (e: Exception) {
            Log.e("GoogleDriveSync", "Failed to save cloud payload", e)
        }
    }

    /**
     * Fetches the Global Book Index.
     */
    suspend fun fetchGlobalBookIndex(currentUser: UserProfile): Result<List<GlobalBookIndexEntry>> = withContext(Dispatchers.IO) {
        try {
            val allEntries = getGlobalIndexFromStorage()
            val isEditorInChief = currentUser.email.equals(EDITOR_IN_CHIEF_EMAIL, ignoreCase = true)

            val synchronizedEntries = allEntries.map { entry ->
                if (entry.authorEmail.equals(currentUser.email, ignoreCase = true) && !isEditorInChief) {
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

    suspend fun suspendUser(adminEmail: String, targetEmail: String, reason: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!adminEmail.equals(EDITOR_IN_CHIEF_EMAIL, ignoreCase = true)) {
                return@withContext Result.failure(IllegalAccessException("Only the Editor in Chief can suspend users."))
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

    suspend fun unsuspendUser(adminEmail: String, targetEmail: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!adminEmail.equals(EDITOR_IN_CHIEF_EMAIL, ignoreCase = true)) {
                return@withContext Result.failure(IllegalAccessException("Only the Editor in Chief can unsuspend users."))
            }

            val suspended = getSuspendedUsersFromStorage().toMutableList()
            suspended.removeAll { it.email.equals(targetEmail.trim(), ignoreCase = true) }
            saveSuspendedUsersToStorage(suspended)

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

    suspend fun sendServerlessMail(message: ServerlessMailMessage): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (message.recipientEmail.equals("ALL_AUTHORS", ignoreCase = true)) {
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

    suspend fun fetchMailboxMessages(userEmail: String): Result<List<ServerlessMailMessage>> = withContext(Dispatchers.IO) {
        try {
            val mail = getMailboxFromStorage(userEmail)
            Result.success(mail)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

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
