package com.supernatural.bwriter.data

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.supernatural.bwriter.model.BookVersionSnapshot
import com.supernatural.bwriter.model.GlobalBookIndexEntry
import com.supernatural.bwriter.model.ManuscriptEntity
import com.supernatural.bwriter.model.MatterType
import com.supernatural.bwriter.model.SectionEntity
import com.supernatural.bwriter.model.CharacterEntity
import com.supernatural.bwriter.model.EditorialCommentEntity
import com.supernatural.bwriter.model.StorySettingEntity
import com.supernatural.bwriter.model.SectionStatus
import com.supernatural.bwriter.model.SectionType
import com.supernatural.bwriter.model.ServerlessMailMessage
import com.supernatural.bwriter.model.SuspendedUserEntry
import com.supernatural.bwriter.model.UserProfile
import com.supernatural.bwriter.model.WorkRole
import com.supernatural.bwriter.model.WorkType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

object StudioStanding {
    @Volatile
    var isEditorInChief: Boolean = false
}

data class BookStamp(
    val authorAddress: String = "",
    val authorPubKey: String = "",
    val birthId: String = "",
    val stampHash: String = "",
    val prevStampHash: String = "",
    val stampSignature: String = ""
)

data class RestoredBook(
    val manuscript: ManuscriptEntity,
    val sections: List<SectionEntity>,
    val sectionIds: List<Long> = emptyList(),
    val characters: List<CharacterEntity> = emptyList(),
    val settings: List<StorySettingEntity> = emptyList(),
    val comments: List<EditorialCommentEntity> = emptyList()
)

data class SiteUser(
    val displayName: String = "",
    val email: String = "",
    val lastSeenAtMillis: Long = 0L
)

class FirestoreNetworkService(private val context: Context) {

    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    companion object {
        const val DEFAULT_STUDIO_AUTHOR_EMAIL = "author.studio@bwriter.io"

        private const val COL_REGISTRY = "registry"
        private const val COL_PENDING = "registryPending"
        private const val COL_SUSPENDED = "suspended"
        private const val COL_MAIL = "mail"
        private const val COL_CLOUD_BOOKS = "cloudBooks"
        private const val COL_VERSIONS = "versionSnapshots"
        private const val COL_STUDIO_CONFIG = "studioConfig"
        private const val COL_CREDIT_ISSUANCES = "creditIssuances"
        private const val COL_CREDIT_CLAIMS = "creditClaims"
        private const val COL_EDITOR_ASSIGNMENTS = "editorAssignments"
        private const val COL_USERS = "users"
    }

    @Volatile
    private var suspendedCache: List<SuspendedUserEntry> = emptyList()

    init {
        db.collection(COL_SUSPENDED).addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("FirestoreNetwork", "Suspended listener failed", error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                suspendedCache = snapshot.documents.mapNotNull { docToSuspended(it.data) }
            }
        }
    }

    private fun normEmail(email: String): String = email.trim().lowercase()


    suspend fun checkUserSuspension(email: String): SuspendedUserEntry? = withContext(Dispatchers.IO) {
        try {
            val doc = db.collection(COL_SUSPENDED).document(normEmail(email)).get().await()
            val entry = if (doc.exists()) docToSuspended(doc.data) else null
            if (entry != null && entry.isLockedOut) entry else null
        } catch (e: Exception) {
            Log.e("FirestoreNetwork", "Suspension check failed, using cache", e)
            suspendedCache.firstOrNull { it.email.equals(email.trim(), ignoreCase = true) && it.isLockedOut }
        }
    }

    fun getSuspendedUsers(): List<SuspendedUserEntry> = suspendedCache

    suspend fun suspendUser(adminEmail: String, targetEmail: String, reason: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!StudioStanding.isEditorInChief) {
                return@withContext Result.failure(IllegalAccessException("Only the Editor in Chief can suspend users."))
            }
            val entry = SuspendedUserEntry(
                email = targetEmail.trim(),
                reason = reason,
                suspendedBy = "Editor in Chief",
                isLockedOut = true
            )
            db.collection(COL_SUSPENDED).document(normEmail(targetEmail)).set(suspendedToMap(entry)).await()

            sendServerlessMail(
                ServerlessMailMessage(
                    recipientEmail = targetEmail.trim(),
                    senderEmail = DEFAULT_STUDIO_AUTHOR_EMAIL,
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
            if (!StudioStanding.isEditorInChief) {
                return@withContext Result.failure(IllegalAccessException("Only the Editor in Chief can unsuspend users."))
            }
            db.collection(COL_SUSPENDED).document(normEmail(targetEmail)).delete().await()

            sendServerlessMail(
                ServerlessMailMessage(
                    recipientEmail = targetEmail.trim(),
                    senderEmail = DEFAULT_STUDIO_AUTHOR_EMAIL,
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


    suspend fun syncManuscriptToBackRoom(
        manuscript: ManuscriptEntity,
        sections: List<SectionEntity>,
        characters: List<CharacterEntity>,
        settings: List<StorySettingEntity>,
        comments: List<EditorialCommentEntity>,
        currentUser: UserProfile,
        isPublicInCommunity: Boolean,
        stamp: BookStamp
    ): Result<GlobalBookIndexEntry> = withContext(Dispatchers.IO) {
        try {
            val suspension = checkUserSuspension(currentUser.email)
            if (suspension != null) {
                return@withContext Result.failure(Exception("Account suspended: ${suspension.reason}"))
            }

            val resolvedAuthorName = when {
                manuscript.authorPenName.isNotBlank() -> manuscript.authorPenName.trim()
                manuscript.authorName.isNotBlank() -> manuscript.authorName.trim()
                else -> currentUser.displayName
            }

            val manuscriptJson = buildManuscriptPayload(manuscript, sections, characters, settings, comments, currentUser, resolvedAuthorName, isPublicInCommunity)

            val cloudDocId = "${normEmail(currentUser.email)}_${stamp.birthId}"
            val cloudRef = db.collection(COL_CLOUD_BOOKS).document(cloudDocId)
            val existingCloud = cloudRef.get().await()
            val cloudUpdatedAt = existingCloud.getLong("updatedAt") ?: 0L
            if (existingCloud.exists() && cloudUpdatedAt > manuscript.updatedAt) {
                return@withContext Result.failure(
                    Exception("Cloud copy of '${manuscript.title}' is newer than this device. Restore it before syncing.")
                )
            }
            cloudRef.set(
                mapOf(
                    "ownerEmail" to normEmail(currentUser.email),
                    "manuscriptId" to manuscript.id,
                    "birthId" to stamp.birthId,
                    "payload" to manuscriptJson.toString(),
                    "updatedAt" to System.currentTimeMillis()
                )
            ).await()

            val totalWords = sections.sumOf { it.wordCount.takeIf { c -> c > 0 } ?: it.content.split(Regex("\\s+")).count { w -> w.isNotBlank() } }
            val estimatedLeaves = (totalWords / 250).coerceAtLeast(1)

            val isEditorInChief = StudioStanding.isEditorInChief
            val pendingExisting = findDocInCollection(COL_PENDING, manuscript.id, currentUser.email)
            val registryExisting = if (pendingExisting == null) findRegistryDoc(manuscript.id, currentUser.email) else null
            val existing = pendingExisting ?: registryExisting
            val targetCollection = when {
                pendingExisting != null -> COL_PENDING
                registryExisting != null -> COL_REGISTRY
                isEditorInChief -> COL_REGISTRY
                else -> COL_PENDING
            }
            val fileId = existing?.id ?: ("book_" + sha256Hex(stamp.authorAddress + "|" + stamp.birthId).take(48))

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
                editorInChief = StudioStanding.isEditorInChief,
                version = manuscript.edition.ifBlank { "1.0" },
                authorAddress = stamp.authorAddress,
                authorPubKey = stamp.authorPubKey,
                birthId = stamp.birthId,
                stampHash = stamp.stampHash,
                prevStampHash = stamp.prevStampHash,
                stampSignature = stamp.stampSignature,
            )
            db.collection(targetCollection).document(fileId).set(registryToMap(indexEntry)).await()

            db.collection(targetCollection).document(fileId)
                .collection("content").document("fullText")
                .set(
                    mapOf(
                        "payload" to manuscriptJson.toString(),
                        "authorEmail" to currentUser.email,
                        "authorEmailLower" to normEmail(currentUser.email),
                        "isPublicInCommunity" to isPublicInCommunity,
                        "authorAddress" to stamp.authorAddress,
                        "authorPubKey" to stamp.authorPubKey,
                        "birthId" to stamp.birthId,
                        "stampHash" to stamp.stampHash,
                        "prevStampHash" to stamp.prevStampHash,
                        "stampSignature" to stamp.stampSignature,
                        "updatedAt" to System.currentTimeMillis()
                    )
                ).await()

            val snapshot = BookVersionSnapshot(
                manuscriptId = manuscript.id,
                title = manuscript.title,
                versionTag = manuscript.edition.ifBlank { "v1.0" },
                changeSummary = "Cloud backup & sync (${sections.size} divisions, $totalWords words)",
                authorPenName = resolvedAuthorName,
                authorEmail = currentUser.email,
                wordCount = totalWords,
                totalLeaves = estimatedLeaves,
                timestamp = System.currentTimeMillis(),
                driveFileId = fileId,
            )
            db.collection(COL_VERSIONS).add(snapshotToMap(snapshot)).await()

            if (!StudioStanding.isEditorInChief) {
                sendServerlessMail(
                    ServerlessMailMessage(
                        recipientEmail = DEFAULT_STUDIO_AUTHOR_EMAIL,
                        senderEmail = currentUser.email,
                        senderName = currentUser.displayName,
                        subject = "Manuscript Synced: ${manuscript.title}",
                        body = "Author ${currentUser.displayName} (${currentUser.email}) synced '${manuscript.title}' to the cloud registry.\n\nVisibility: ${if (isPublicInCommunity) "Public Global Registry" else "Private"}\nStatus: ${manuscript.manuscriptStatus}\nWords: $totalWords\nFile ID: $fileId",
                        manuscriptId = manuscript.id,
                        manuscriptTitle = manuscript.title,
                        messageType = "EDITORIAL_REVISION"
                    )
                )
            }

            Log.i("FirestoreNetwork", "Synced manuscript ${manuscript.title} as $fileId")
            Result.success(indexEntry)
        } catch (e: Exception) {
            Log.e("FirestoreNetwork", "Sync failed", e)
            Result.failure(e)
        }
    }

    suspend fun restoreUserManuscriptsFromBackRoom(userEmail: String): List<RestoredBook> = withContext(Dispatchers.IO) {
        try {
            val snap = db.collection(COL_CLOUD_BOOKS)
                .whereEqualTo("ownerEmail", normEmail(userEmail))
                .get().await()
            val list = mutableListOf<RestoredBook>()
            for (doc in snap.documents) {
                val payload = doc.getString("payload") ?: continue
                val parsed = parseManuscriptPayload(JSONObject(payload), userEmail)
                if (parsed != null) list.add(parsed)
            }
            list
        } catch (e: Exception) {
            Log.e("FirestoreNetwork", "Restore failed", e)
            emptyList()
        }
    }

    suspend fun fetchGlobalBookIndex(currentUser: UserProfile): Result<List<GlobalBookIndexEntry>> = withContext(Dispatchers.IO) {
        try {
            val isEditorInChief = StudioStanding.isEditorInChief
            val allEntries = if (isEditorInChief) {
                db.collection(COL_REGISTRY).get().await()
                    .documents.mapNotNull { docToRegistry(it.data) }
            } else {
                val publicDocs = db.collection(COL_REGISTRY)
                    .whereEqualTo("isPublicInCommunity", true).get().await().documents
                val ownDocs = db.collection(COL_REGISTRY)
                    .whereEqualTo("authorEmail", currentUser.email).get().await().documents
                (publicDocs + ownDocs).mapNotNull { docToRegistry(it.data) }
                    .distinctBy { it.fileId }
            }

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
            Result.success(filtered.sortedByDescending { it.lastSyncedTimestamp })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchRegistryBook(fileId: String): RestoredBook? = withContext(Dispatchers.IO) {
        try {
            val doc = db.collection(COL_REGISTRY).document(fileId)
                .collection("content").document("fullText")
                .get().await()
            val payload = doc.getString("payload") ?: return@withContext null
            val authorEmail = doc.getString("authorEmail") ?: ""
            parseManuscriptPayload(JSONObject(payload), authorEmail)
        } catch (e: Exception) {
            Log.e("FirestoreNetwork", "Fetch registry book failed", e)
            null
        }
    }

    fun buildPayloadJson(
        manuscript: ManuscriptEntity,
        sections: List<SectionEntity>,
        characters: List<CharacterEntity>,
        settings: List<StorySettingEntity>,
        comments: List<EditorialCommentEntity>,
        currentUser: UserProfile,
        isPublicInCommunity: Boolean
    ): String {
        val resolvedAuthorName = when {
            manuscript.authorPenName.isNotBlank() -> manuscript.authorPenName.trim()
            manuscript.authorName.isNotBlank() -> manuscript.authorName.trim()
            else -> currentUser.displayName
        }
        return buildManuscriptPayload(manuscript, sections, characters, settings, comments, currentUser, resolvedAuthorName, isPublicInCommunity).toString()
    }

    fun parsePayloadJson(payload: String, fallbackEmail: String): RestoredBook? {
        return try {
            parseManuscriptPayload(JSONObject(payload), fallbackEmail)
        } catch (e: Exception) {
            Log.e("FirestoreNetwork", "Failed to parse payload JSON", e)
            null
        }
    }

    fun verifyStamp(
        authorPubKey: String,
        authorAddress: String,
        birthId: String,
        prevStampHash: String,
        payloadJson: String,
        stampHash: String,
        stampSignature: String
    ): Boolean {
        if (stampHash.isBlank() || stampSignature.isBlank()) return false
        val recomputed = sha256Hex(authorAddress + "|" + birthId + "|" + prevStampHash + "|" + payloadJson)
        if (recomputed != stampHash) return false
        return AuthorKeys.verify(authorPubKey, stampHash, stampSignature)
    }

    private fun sha256Hex(input: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    suspend fun revokeManuscriptAccess(adminEmail: String, fileId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!StudioStanding.isEditorInChief) {
                return@withContext Result.failure(IllegalAccessException("Only the Editor in Chief can revoke drive access."))
            }
            val docRef = db.collection(COL_REGISTRY).document(fileId)
            val removed = docToRegistry(docRef.get().await().data)
            docRef.collection("content").document("fullText").delete().await()
            docRef.delete().await()

            if (removed != null) {
                sendServerlessMail(
                    ServerlessMailMessage(
                        recipientEmail = removed.authorEmail,
                        senderEmail = DEFAULT_STUDIO_AUTHOR_EMAIL,
                        senderName = "Editor in Chief",
                        subject = "Manuscript Access Revoked: ${removed.title}",
                        body = "The shared access for '${removed.title}' (File ID: $fileId) has been revoked from the global registry by the Editor in Chief.",
                        messageType = "GOVERNANCE_ALERT"
                    )
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    suspend fun fetchPendingBooks(): Result<List<GlobalBookIndexEntry>> = withContext(Dispatchers.IO) {
        try {
            val snap = db.collection(COL_PENDING).get().await()
            val entries = snap.documents.mapNotNull { docToRegistry(it.data) }
            Result.success(entries.sortedByDescending { it.lastSyncedTimestamp })
        } catch (e: Exception) {
            Log.e("FirestoreNetwork", "Fetch pending books failed", e)
            Result.failure(e)
        }
    }

    suspend fun approvePendingBook(adminEmail: String, fileId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!StudioStanding.isEditorInChief) {
                return@withContext Result.failure(IllegalAccessException("Only the Editor in Chief can approve books."))
            }
            val pendingRef = db.collection(COL_PENDING).document(fileId)
            val entry = docToRegistry(pendingRef.get().await().data)
                ?: return@withContext Result.failure(IllegalStateException("Pending book not found."))
            val contentSnap = pendingRef.collection("content").document("fullText").get().await()

            val stampVerified = entry.authorAddress.isNotBlank()
                && entry.authorAddress == AuthorKeys.addressFromPubKeyHex(entry.authorPubKey)
                && AuthorKeys.verify(entry.authorPubKey, entry.stampHash, entry.stampSignature)
                && contentSnap.exists()
                && contentSnap.getString("stampHash") == entry.stampHash
                && contentSnap.getString("authorAddress") == entry.authorAddress
                && contentSnap.getString("authorPubKey") == entry.authorPubKey
                && contentSnap.getString("birthId") == entry.birthId
                && contentSnap.getString("prevStampHash") == entry.prevStampHash
                && contentSnap.getString("stampSignature") == entry.stampSignature
                && verifyStamp(
                    authorPubKey = entry.authorPubKey,
                    authorAddress = entry.authorAddress,
                    birthId = entry.birthId,
                    prevStampHash = entry.prevStampHash,
                    payloadJson = contentSnap.getString("payload") ?: "",
                    stampHash = entry.stampHash,
                    stampSignature = entry.stampSignature
                )
            if (!stampVerified) {
                return@withContext Result.failure(IllegalStateException("Author stamp verification failed."))
            }

            db.collection(COL_REGISTRY).document(fileId).set(registryToMap(entry)).await()
            if (contentSnap.exists()) {
                db.collection(COL_REGISTRY).document(fileId)
                    .collection("content").document("fullText")
                    .set(contentSnap.data ?: emptyMap<String, Any>()).await()
            }
            issueCredit(entry)
            pendingRef.collection("content").document("fullText").delete().await()
            pendingRef.delete().await()

            sendServerlessMail(
                ServerlessMailMessage(
                    recipientEmail = entry.authorEmail,
                    senderEmail = DEFAULT_STUDIO_AUTHOR_EMAIL,
                    senderName = "Editor in Chief",
                    subject = "Manuscript Approved: ${entry.title}",
                    body = "Your manuscript '${entry.title}' (File ID: $fileId) has been approved by the Editor in Chief and is now in the global registry.",
                    messageType = "GOVERNANCE_ALERT"
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirestoreNetwork", "Approve pending book failed", e)
            Result.failure(e)
        }
    }

    suspend fun rejectPendingBook(adminEmail: String, fileId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!StudioStanding.isEditorInChief) {
                return@withContext Result.failure(IllegalAccessException("Only the Editor in Chief can reject books."))
            }
            val pendingRef = db.collection(COL_PENDING).document(fileId)
            val entry = docToRegistry(pendingRef.get().await().data)
            pendingRef.collection("content").document("fullText").delete().await()
            pendingRef.delete().await()

            if (entry != null) {
                sendServerlessMail(
                    ServerlessMailMessage(
                        recipientEmail = entry.authorEmail,
                        senderEmail = DEFAULT_STUDIO_AUTHOR_EMAIL,
                        senderName = "Editor in Chief",
                        subject = "Manuscript Not Approved: ${entry.title}",
                        body = "Your manuscript '${entry.title}' (File ID: $fileId) was not approved for the global registry by the Editor in Chief. Your own cloud copy is unchanged.",
                        messageType = "GOVERNANCE_ALERT"
                    )
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirestoreNetwork", "Reject pending book failed", e)
            Result.failure(e)
        }
    }


    suspend fun fetchIssuancesFor(email: String): List<CreditIssuance> = withContext(Dispatchers.IO) {
        try {
            val snap = db.collection(COL_CREDIT_ISSUANCES)
                .whereEqualTo("authorEmail", normEmail(email)).get().await()
            snap.documents.mapNotNull { docToIssuance(it.data) }
                .sortedBy { it.issuedAtMillis }
        } catch (e: Exception) {
            Log.e("FirestoreNetwork", "Fetch credit issuances failed", e)
            emptyList()
        }
    }

    suspend fun issueCredit(entry: GlobalBookIndexEntry) {
        if (!StudioStanding.isEditorInChief) return
        try {
            val existingForBook = db.collection(COL_CREDIT_ISSUANCES)
                .whereEqualTo("fileId", entry.fileId).get().await()
                .documents.mapNotNull { docToIssuance(it.data) }
            val existingAuthorIssuance = existingForBook.firstOrNull {
                it.kind == "AUTHOR" && it.authorEmail == normEmail(entry.authorEmail)
            }

            val amount = existingAuthorIssuance?.amount
                ?: CreditSchedule.amountForApproval(
                    fetchIssuancesFor(entry.authorEmail).count { it.kind == "AUTHOR" }
                )

            if (existingAuthorIssuance == null && entry.authorAddress.isNotBlank()) {
                writeIssuance(entry.authorEmail, entry.authorAddress, amount, "AUTHOR", entry)
            }
            fetchApprovedEditorAssignments(entry.fileId).forEach { assignment ->
                val editorEntry = db.collection(COL_REGISTRY)
                    .whereEqualTo("authorEmailLower", normEmail(assignment.editorEmail)).get().await()
                    .documents.mapNotNull { docToRegistry(it.data) }
                    .firstOrNull { it.authorAddress.isNotBlank() }
                if (editorEntry != null && !existingForBook.any { it.kind == "EDITOR" && it.authorEmail == normEmail(assignment.editorEmail) }) {
                    writeIssuance(assignment.editorEmail, editorEntry.authorAddress, amount, "EDITOR", entry)
                }
            }
        } catch (e: Exception) {
            Log.e("FirestoreNetwork", "Credit issuance failed", e)
        }
    }

    private suspend fun writeIssuance(
        recipientEmail: String,
        recipientAddress: String,
        amount: Int,
        kind: String,
        entry: GlobalBookIndexEntry
    ) {
        val recipientIssuances = fetchIssuancesFor(recipientEmail)
        val prevHash = recipientIssuances.maxByOrNull { it.issuedAtMillis }?.issuanceHash ?: ""
        val issuedAt = System.currentTimeMillis()
        val hash = CreditChain.issuanceHashFor(prevHash, recipientAddress, amount, entry.fileId, issuedAt)
        val issuance = CreditIssuance(
            id = hash,
            authorEmail = normEmail(recipientEmail),
            authorAddress = recipientAddress,
            amount = amount,
            kind = kind,
            fileId = entry.fileId,
            birthId = entry.birthId,
            issuedAtMillis = issuedAt,
            prevIssuanceHash = prevHash,
            issuanceHash = hash,
            signature = AuthorKeys.sign(context, hash)
        )
        db.collection(COL_CREDIT_ISSUANCES).document(hash).set(issuanceToMap(issuance)).await()
    }

    suspend fun fetchClaimedIssuanceIds(email: String): Set<String> = withContext(Dispatchers.IO) {
        try {
            val snap = db.collection(COL_CREDIT_CLAIMS)
                .whereEqualTo("authorEmail", normEmail(email)).get().await()
            snap.documents.mapNotNull { it.getString("issuanceHash") }.toSet()
        } catch (e: Exception) {
            Log.e("FirestoreNetwork", "Fetch credit claims failed", e)
            emptySet()
        }
    }

    suspend fun recordClaim(email: String, issuance: CreditIssuance, nonce: Long) = withContext(Dispatchers.IO) {
        try {
            val issuances = fetchIssuancesFor(email)
            val index = issuances.indexOfFirst { it.issuanceHash == issuance.issuanceHash }
            if (index < 0) return@withContext
            val difficulty = CreditSchedule.difficultyBits(index)
            if (!CreditChain.verifyClaim(issuance.issuanceHash, nonce, difficulty)) return@withContext
            db.collection(COL_CREDIT_CLAIMS).document(issuance.issuanceHash + "_" + nonce).set(
                mapOf(
                    "authorEmail" to normEmail(email),
                    "issuanceHash" to issuance.issuanceHash,
                    "amount" to issuance.amount,
                    "fileId" to issuance.fileId,
                    "nonce" to nonce,
                    "claimedAtMillis" to System.currentTimeMillis()
                )
            ).await()
        } catch (e: Exception) {
            Log.e("FirestoreNetwork", "Record credit claim failed", e)
        }
    }

    private fun docToIssuance(data: Map<String, Any>?): CreditIssuance? {
        if (data == null) return null
        val hash = data["issuanceHash"] as? String ?: return null
        return CreditIssuance(
            id = data["id"] as? String ?: hash,
            authorEmail = data["authorEmail"] as? String ?: "",
            authorAddress = data["authorAddress"] as? String ?: "",
            amount = (data["amount"] as? Number)?.toInt() ?: 0,
            kind = data["kind"] as? String ?: "AUTHOR",
            fileId = data["fileId"] as? String ?: "",
            birthId = data["birthId"] as? String ?: "",
            issuedAtMillis = (data["issuedAtMillis"] as? Number)?.toLong() ?: 0L,
            prevIssuanceHash = data["prevIssuanceHash"] as? String ?: "",
            issuanceHash = hash,
            signature = data["signature"] as? String ?: ""
        )
    }

    private fun issuanceToMap(i: CreditIssuance): Map<String, Any> = mapOf(
        "id" to i.id,
        "authorEmail" to i.authorEmail,
        "authorAddress" to i.authorAddress,
        "amount" to i.amount,
        "kind" to i.kind,
        "fileId" to i.fileId,
        "birthId" to i.birthId,
        "issuedAtMillis" to i.issuedAtMillis,
        "prevIssuanceHash" to i.prevIssuanceHash,
        "issuanceHash" to i.issuanceHash,
        "signature" to i.signature
    )


    suspend fun fetchEditorAssignmentsForBook(fileId: String): List<EditorAssignment> = withContext(Dispatchers.IO) {
        try {
            val snap = db.collection(COL_EDITOR_ASSIGNMENTS)
                .whereEqualTo("fileId", fileId).get().await()
            snap.documents.mapNotNull { docToAssignment(it.data) }
        } catch (e: Exception) {
            Log.e("FirestoreNetwork", "Fetch editor assignments failed", e)
            emptyList()
        }
    }

    suspend fun fetchApprovedEditorAssignments(fileId: String): List<EditorAssignment> =
        fetchEditorAssignmentsForBook(fileId).filter { it.status == "APPROVED" }

    suspend fun fetchPendingEditorAssignments(): List<EditorAssignment> = withContext(Dispatchers.IO) {
        try {
            val snap = db.collection(COL_EDITOR_ASSIGNMENTS)
                .whereEqualTo("status", "PENDING").get().await()
            snap.documents.mapNotNull { docToAssignment(it.data) }
        } catch (e: Exception) {
            Log.e("FirestoreNetwork", "Fetch pending editor assignments failed", e)
            emptyList()
        }
    }

    suspend fun createEditorAssignment(assignment: EditorAssignment) = withContext(Dispatchers.IO) {
        try {
            val id = CreditChain.sha256Hex(assignment.fileId + "|" + normEmail(assignment.editorEmail))
            val stored = assignment.copy(id = id, editorEmail = normEmail(assignment.editorEmail))
            db.collection(COL_EDITOR_ASSIGNMENTS).document(id).set(assignmentToMap(stored)).await()
        } catch (e: Exception) {
            Log.e("FirestoreNetwork", "Create editor assignment failed", e)
        }
    }

    suspend fun setEditorAssignmentStatus(id: String, status: String) = withContext(Dispatchers.IO) {
        try {
            db.collection(COL_EDITOR_ASSIGNMENTS).document(id).update("status", status).await()
        } catch (e: Exception) {
            Log.e("FirestoreNetwork", "Set editor assignment status failed", e)
        }
    }

    suspend fun recordUserPresence(displayName: String, email: String) = withContext(Dispatchers.IO) {
        try {
            val normalized = normEmail(email)
            if (normalized.isBlank()) return@withContext
            val ref = db.collection(COL_USERS).document(normalized)
            val existing = ref.get().await()
            val now = System.currentTimeMillis()
            val firstSeen = if (existing.exists()) {
                (existing.get("firstSeenAt") as? Number)?.toLong() ?: now
            } else {
                now
            }
            ref.set(
                mapOf(
                    "displayName" to displayName,
                    "email" to normalized,
                    "firstSeenAt" to firstSeen,
                    "lastSeenAt" to now
                )
            ).await()
        } catch (e: Exception) {
            Log.e("FirestoreNetwork", "Record user presence failed", e)
        }
    }

    suspend fun fetchSiteUsers(): List<SiteUser> = withContext(Dispatchers.IO) {
        try {
            val snap = db.collection(COL_USERS).get().await()
            snap.documents.mapNotNull { docToSiteUser(it.data) }
                .sortedByDescending { it.lastSeenAtMillis }
        } catch (e: Exception) {
            Log.e("FirestoreNetwork", "Fetch site users failed", e)
            emptyList()
        }
    }

    private fun docToSiteUser(data: Map<String, Any>?): SiteUser? {
        if (data == null) return null
        val email = data["email"] as? String ?: return null
        return SiteUser(
            displayName = data["displayName"] as? String ?: "",
            email = email,
            lastSeenAtMillis = (data["lastSeenAt"] as? Number)?.toLong() ?: 0L
        )
    }

    private fun docToAssignment(data: Map<String, Any>?): EditorAssignment? {
        if (data == null) return null
        val id = data["id"] as? String ?: return null
        return EditorAssignment(
            id = id,
            fileId = data["fileId"] as? String ?: "",
            bookAuthorEmail = data["bookAuthorEmail"] as? String ?: "",
            editorEmail = data["editorEmail"] as? String ?: "",
            editorAddress = data["editorAddress"] as? String ?: "",
            requestedBy = data["requestedBy"] as? String ?: "",
            status = data["status"] as? String ?: "PENDING"
        )
    }

    private fun assignmentToMap(a: EditorAssignment): Map<String, Any> = mapOf(
        "id" to a.id,
        "fileId" to a.fileId,
        "bookAuthorEmail" to a.bookAuthorEmail,
        "editorEmail" to a.editorEmail,
        "editorAddress" to a.editorAddress,
        "requestedBy" to a.requestedBy,
        "status" to a.status
    )


    suspend fun sendServerlessMail(message: ServerlessMailMessage): Result<Unit> = withContext(Dispatchers.IO) {
        val message = if (StudioStanding.isEditorInChief) {
            message.copy(senderEmail = DEFAULT_STUDIO_AUTHOR_EMAIL)
        } else {
            message
        }
        try {
            if (message.recipientEmail.equals("ALL_AUTHORS", ignoreCase = true) && !StudioStanding.isEditorInChief) {
                return@withContext Result.failure(IllegalStateException("Broadcast mail requires Editor in Chief standing."))
            }
            if (message.recipientEmail.equals("ALL_AUTHORS", ignoreCase = true)) {
                val snap = db.collection(COL_REGISTRY).get().await()
                val allAuthors = snap.documents.mapNotNull { it.getString("authorEmail") }.distinct()
                allAuthors.forEach { authorEmail ->
                    val copy = message.copy(id = java.util.UUID.randomUUID().toString(), recipientEmail = authorEmail)
                    db.collection(COL_MAIL).document(copy.id).set(mailToMap(copy)).await()
                }
            } else {
                db.collection(COL_MAIL).document(message.id).set(mailToMap(message)).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun verifyEditorInChief(): Boolean = withContext(Dispatchers.IO) {
        try {
            val snap = db.collection(COL_STUDIO_CONFIG).document("main").get().await()
            snap.exists()
        } catch (e: Exception) {
            false
        }
    }

    suspend fun fetchRegentEmail(): String = withContext(Dispatchers.IO) {
        try {
            val snap = db.collection(COL_STUDIO_CONFIG).document("regency").get().await()
            snap.getString("regentEmail") ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    suspend fun setRegent(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val data = mapOf(
                "regentEmail" to email.trim().lowercase(),
                "updatedAtMillis" to System.currentTimeMillis()
            )
            db.collection(COL_STUDIO_CONFIG).document("regency").set(data).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchMailboxMessages(userEmail: String): Result<List<ServerlessMailMessage>> = withContext(Dispatchers.IO) {
        try {
            val mine = fetchMailboxFor(userEmail)
            val combined = if (StudioStanding.isEditorInChief) {
                (mine + fetchMailboxFor(DEFAULT_STUDIO_AUTHOR_EMAIL)).distinctBy { it.id }.sortedByDescending { it.timestamp }
            } else {
                mine
            }
            Result.success(combined)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun fetchMailboxFor(email: String): List<ServerlessMailMessage> {
        val snap: QuerySnapshot = db.collection(COL_MAIL)
            .whereEqualTo("recipientEmail", normEmail(email))
            .get().await()
        return snap.documents.mapNotNull { docToMail(it.data) }.sortedByDescending { it.timestamp }
    }

    suspend fun markMailAsRead(userEmail: String, messageId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            db.collection(COL_MAIL).document(messageId).update("isRead", true).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    suspend fun getVersionHistory(manuscriptId: Long, requesterEmail: String): List<BookVersionSnapshot> = withContext(Dispatchers.IO) {
        try {
            val base = db.collection(COL_VERSIONS)
                .whereEqualTo("manuscriptId", manuscriptId)
            val query = if (StudioStanding.isEditorInChief) base
                else base.whereEqualTo("authorEmail", requesterEmail)
            val snap = query.get().await()
            snap.documents.mapNotNull { docToSnapshot(it.data) }.sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            Log.e("FirestoreNetwork", "Version history failed", e)
            emptyList()
        }
    }


    private fun buildManuscriptPayload(
        manuscript: ManuscriptEntity,
        sections: List<SectionEntity>,
        characters: List<CharacterEntity>,
        settings: List<StorySettingEntity>,
        comments: List<EditorialCommentEntity>,
        currentUser: UserProfile,
        resolvedAuthorName: String,
        isPublicInCommunity: Boolean
    ): JSONObject {
        return JSONObject().apply {
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
            put("authorAddress", manuscript.authorAddress)
            put("authorPubKey", AuthorKeys.publicKeyHex(context))
            put("birthId", manuscript.birthId)
            put("updatedAt", manuscript.updatedAt)
            put("stampHash", manuscript.stampHash)
            put("prevStampHash", manuscript.prevStampHash)
            put("stampSignature", manuscript.stampSignature)
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

            val charactersArray = JSONArray()
            characters.forEach { ch ->
                val chObj = JSONObject().apply {
                    put("id", ch.id)
                    put("name", ch.name)
                    put("role", ch.role)
                    put("physicalDescription", ch.physicalDescription)
                    put("psychologicalDescription", ch.psychologicalDescription)
                    put("backstory", ch.backstory)
                    put("voiceAndMannerisms", ch.voiceAndMannerisms)
                    put("intertextualArchetype", ch.intertextualArchetype)
                    put("otherDetails", ch.otherDetails)
                }
                charactersArray.put(chObj)
            }
            put("characters", charactersArray)

            val settingsArray = JSONArray()
            settings.forEach { st ->
                val stObj = JSONObject().apply {
                    put("id", st.id)
                    put("locationName", st.locationName)
                    put("timePeriodOrEra", st.timePeriodOrEra)
                    put("atmosphereAndSensory", st.atmosphereAndSensory)
                    put("architecturalOrSpatialDetails", st.architecturalOrSpatialDetails)
                    put("historicalOrCulturalContext", st.historicalOrCulturalContext)
                    put("targetIntertextualTouchstones", st.targetIntertextualTouchstones)
                    put("otherNotes", st.otherNotes)
                }
                settingsArray.put(stObj)
            }
            put("settings", settingsArray)

            val commentsArray = JSONArray()
            comments.forEach { cm ->
                val cmObj = JSONObject().apply {
                    put("id", cm.id)
                    put("sectionId", cm.sectionId)
                    put("authorName", cm.authorName)
                    put("authorRole", cm.authorRole.name)
                    put("commentText", cm.commentText)
                    put("cmosRuleReference", cm.cmosRuleReference)
                    put("isResolved", cm.isResolved)
                    put("timestamp", cm.timestamp)
                }
                commentsArray.put(cmObj)
            }
            put("comments", commentsArray)
        }
    }

    private fun parseManuscriptPayload(mObj: JSONObject, userEmail: String): RestoredBook? {
        return try {
            val workType = try {
                WorkType.valueOf(mObj.optString("workType", "NOVEL"))
            } catch (e: Exception) {
                WorkType.NOVEL
            }

            val manuscript = ManuscriptEntity(
                title = mObj.optString("title", "Restored Manuscript"),
                subtitle = mObj.optString("subtitle", ""),
                workType = workType,
                authorName = mObj.optString("authorName", ""),
                authorPenName = mObj.optString("authorPenName", ""),
                authorEmail = mObj.optString("authorEmail", userEmail),
                editorName = mObj.optString("editorName", ""),
                publisher = mObj.optString("publisher", "Bwriter Editions"),
                edition = mObj.optString("edition", "First Edition"),
                year = mObj.optString("year", "2026"),
                isbn = mObj.optString("isbn", ""),
                birthId = mObj.optString("birthId", ""),
                stampHash = mObj.optString("stampHash", ""),
                prevStampHash = mObj.optString("prevStampHash", ""),
                stampSignature = mObj.optString("stampSignature", ""),
                authorAddress = mObj.optString("authorAddress", ""),
                copyrightText = mObj.optString("copyrightText", ""),
                dedication = mObj.optString("dedication", ""),
                epigraphText = mObj.optString("epigraphText", ""),
                epigraphAuthor = mObj.optString("epigraphAuthor", ""),
                targetPageSize = mObj.optString("targetPageSize", "Trade 6\" x 9\""),
                manuscriptStatus = mObj.optString("manuscriptStatus", "DRAFT"),
                acknowledgmentsJson = mObj.optString("acknowledgmentsJson", "[]"),
                updatedAt = mObj.optLong("updatedAt", System.currentTimeMillis())
            )

            val secArray = mObj.optJSONArray("sections") ?: JSONArray()
            val sections = mutableListOf<SectionEntity>()
            val sectionPayloadIds = mutableListOf<Long>()
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

                sectionPayloadIds.add(sObj.optLong("id", 0L))
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
            val characters = mutableListOf<CharacterEntity>()
            val charArray = mObj.optJSONArray("characters") ?: JSONArray()
            for (i in 0 until charArray.length()) {
                val cObj = charArray.getJSONObject(i)
                characters.add(
                    CharacterEntity(
                        manuscriptId = 0L,
                        name = cObj.optString("name", ""),
                        role = cObj.optString("role", "Protagonist"),
                        physicalDescription = cObj.optString("physicalDescription", ""),
                        psychologicalDescription = cObj.optString("psychologicalDescription", ""),
                        backstory = cObj.optString("backstory", ""),
                        voiceAndMannerisms = cObj.optString("voiceAndMannerisms", ""),
                        intertextualArchetype = cObj.optString("intertextualArchetype", ""),
                        otherDetails = cObj.optString("otherDetails", "")
                    )
                )
            }

            val settings = mutableListOf<StorySettingEntity>()
            val setArray = mObj.optJSONArray("settings") ?: JSONArray()
            for (i in 0 until setArray.length()) {
                val stObj = setArray.getJSONObject(i)
                settings.add(
                    StorySettingEntity(
                        manuscriptId = 0L,
                        locationName = stObj.optString("locationName", ""),
                        timePeriodOrEra = stObj.optString("timePeriodOrEra", ""),
                        atmosphereAndSensory = stObj.optString("atmosphereAndSensory", ""),
                        architecturalOrSpatialDetails = stObj.optString("architecturalOrSpatialDetails", ""),
                        historicalOrCulturalContext = stObj.optString("historicalOrCulturalContext", ""),
                        targetIntertextualTouchstones = stObj.optString("targetIntertextualTouchstones", ""),
                        otherNotes = stObj.optString("otherNotes", "")
                    )
                )
            }

            val comments = mutableListOf<EditorialCommentEntity>()
            val comArray = mObj.optJSONArray("comments") ?: JSONArray()
            for (i in 0 until comArray.length()) {
                val cmObj = comArray.getJSONObject(i)
                val cRole = try {
                    WorkRole.valueOf(cmObj.optString("authorRole", "AUTHOR"))
                } catch (e: Exception) {
                    WorkRole.AUTHOR
                }
                comments.add(
                    EditorialCommentEntity(
                        sectionId = cmObj.optLong("sectionId", 0L),
                        manuscriptId = 0L,
                        authorName = cmObj.optString("authorName", ""),
                        authorRole = cRole,
                        commentText = cmObj.optString("commentText", ""),
                        cmosRuleReference = cmObj.optString("cmosRuleReference", ""),
                        isResolved = cmObj.optBoolean("isResolved", false),
                        timestamp = cmObj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }

            RestoredBook(manuscript, sections, sectionPayloadIds, characters, settings, comments)
        } catch (e: Exception) {
            Log.e("FirestoreNetwork", "Failed to parse manuscript payload", e)
            null
        }
    }


    private suspend fun findRegistryDoc(manuscriptId: Long, authorEmail: String): com.google.firebase.firestore.DocumentSnapshot? {
        return findDocInCollection(COL_REGISTRY, manuscriptId, authorEmail)
    }

    private suspend fun findDocInCollection(collection: String, manuscriptId: Long, authorEmail: String): com.google.firebase.firestore.DocumentSnapshot? {
        val snap = db.collection(collection)
            .whereEqualTo("manuscriptId", manuscriptId)
            .whereEqualTo("authorEmail", authorEmail)
            .get().await()
        return snap.documents.firstOrNull()
    }

    private fun registryToMap(e: GlobalBookIndexEntry): Map<String, Any?> = mapOf(
        "fileId" to e.fileId,
        "manuscriptId" to e.manuscriptId,
        "title" to e.title,
        "subtitle" to e.subtitle,
        "authorName" to e.authorName,
        "authorEmail" to e.authorEmail,
        "authorEmailLower" to normEmail(e.authorEmail),
        "workType" to e.workType,
        "wordCount" to e.wordCount,
        "totalLeaves" to e.totalLeaves,
        "lastSyncedTimestamp" to e.lastSyncedTimestamp,
        "isPublicInCommunity" to e.isPublicInCommunity,
        "editorInChief" to e.editorInChief,
        "version" to e.version,
        "authorAddress" to e.authorAddress,
        "authorPubKey" to e.authorPubKey,
        "birthId" to e.birthId,
        "stampHash" to e.stampHash,
        "prevStampHash" to e.prevStampHash,
        "stampSignature" to e.stampSignature,
        "hasFullText" to true
    )

    private fun docToRegistry(data: Map<String, Any>?): GlobalBookIndexEntry? {
        if (data == null) return null
        return try {
            GlobalBookIndexEntry(
                fileId = data["fileId"] as? String ?: return null,
                manuscriptId = (data["manuscriptId"] as? Number)?.toLong() ?: 0L,
                title = data["title"] as? String ?: "",
                subtitle = data["subtitle"] as? String ?: "",
                authorName = data["authorName"] as? String ?: "",
                authorEmail = data["authorEmail"] as? String ?: "",
                workType = data["workType"] as? String ?: "NOVEL",
                wordCount = (data["wordCount"] as? Number)?.toInt() ?: 0,
                totalLeaves = (data["totalLeaves"] as? Number)?.toInt() ?: 0,
                lastSyncedTimestamp = (data["lastSyncedTimestamp"] as? Number)?.toLong() ?: 0L,
                isPublicInCommunity = data["isPublicInCommunity"] as? Boolean ?: true,
                editorInChief = data["editorInChief"] as? Boolean ?: false,
                version = data["version"] as? String ?: "1.0",
                authorAddress = data["authorAddress"] as? String ?: "",
                authorPubKey = data["authorPubKey"] as? String ?: "",
                birthId = data["birthId"] as? String ?: "",
                stampHash = data["stampHash"] as? String ?: "",
                prevStampHash = data["prevStampHash"] as? String ?: "",
                stampSignature = data["stampSignature"] as? String ?: "",
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun suspendedToMap(e: SuspendedUserEntry): Map<String, Any?> = mapOf(
        "email" to e.email,
        "emailLower" to normEmail(e.email),
        "suspendedAt" to e.suspendedAt,
        "reason" to e.reason,
        "suspendedBy" to e.suspendedBy,
        "isLockedOut" to e.isLockedOut
    )

    private fun docToSuspended(data: Map<String, Any>?): SuspendedUserEntry? {
        if (data == null) return null
        return try {
            SuspendedUserEntry(
                email = data["email"] as? String ?: return null,
                suspendedAt = (data["suspendedAt"] as? Number)?.toLong() ?: 0L,
                reason = data["reason"] as? String ?: "",
                suspendedBy = data["suspendedBy"] as? String ?: "",
                isLockedOut = data["isLockedOut"] as? Boolean ?: true
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun mailToMap(m: ServerlessMailMessage): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>(
            "id" to m.id,
            "recipientEmail" to normEmail(m.recipientEmail),
            "recipientEmailDisplay" to m.recipientEmail,
            "senderEmail" to m.senderEmail,
            "senderName" to m.senderName,
            "subject" to m.subject,
            "body" to m.body,
            "timestamp" to m.timestamp,
            "isRead" to m.isRead,
            "messageType" to m.messageType
        )
        if (m.manuscriptId != null) map["manuscriptId"] = m.manuscriptId
        if (m.manuscriptTitle != null) map["manuscriptTitle"] = m.manuscriptTitle
        return map
    }

    private fun docToMail(data: Map<String, Any>?): ServerlessMailMessage? {
        if (data == null) return null
        return try {
            ServerlessMailMessage(
                id = data["id"] as? String ?: return null,
                recipientEmail = data["recipientEmailDisplay"] as? String ?: (data["recipientEmail"] as? String ?: ""),
                senderEmail = data["senderEmail"] as? String ?: "",
                senderName = data["senderName"] as? String ?: "",
                subject = data["subject"] as? String ?: "",
                body = data["body"] as? String ?: "",
                timestamp = (data["timestamp"] as? Number)?.toLong() ?: 0L,
                isRead = data["isRead"] as? Boolean ?: false,
                manuscriptId = (data["manuscriptId"] as? Number)?.toLong(),
                manuscriptTitle = data["manuscriptTitle"] as? String,
                messageType = data["messageType"] as? String ?: "DIRECT_MESSAGE"
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun snapshotToMap(s: BookVersionSnapshot): Map<String, Any?> = mapOf(
        "snapshotId" to s.snapshotId,
        "manuscriptId" to s.manuscriptId,
        "title" to s.title,
        "versionTag" to s.versionTag,
        "changeSummary" to s.changeSummary,
        "authorPenName" to s.authorPenName,
        "authorEmail" to s.authorEmail,
        "wordCount" to s.wordCount,
        "totalLeaves" to s.totalLeaves,
        "timestamp" to s.timestamp,
        "driveFileId" to s.driveFileId,
    )

    private fun docToSnapshot(data: Map<String, Any>?): BookVersionSnapshot? {
        if (data == null) return null
        return try {
            BookVersionSnapshot(
                snapshotId = data["snapshotId"] as? String ?: "",
                manuscriptId = (data["manuscriptId"] as? Number)?.toLong() ?: 0L,
                title = data["title"] as? String ?: "",
                versionTag = data["versionTag"] as? String ?: "",
                changeSummary = data["changeSummary"] as? String ?: "",
                authorPenName = data["authorPenName"] as? String ?: "",
                authorEmail = data["authorEmail"] as? String ?: "",
                wordCount = (data["wordCount"] as? Number)?.toInt() ?: 0,
                totalLeaves = (data["totalLeaves"] as? Number)?.toInt() ?: 0,
                timestamp = (data["timestamp"] as? Number)?.toLong() ?: 0L,
                driveFileId = data["driveFileId"] as? String ?: "",
            )
        } catch (e: Exception) {
            null
        }
    }
}

