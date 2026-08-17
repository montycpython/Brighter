package com.example.model

data class GlobalBookIndexEntry(
    val fileId: String,
    val manuscriptId: Long,
    val title: String,
    val subtitle: String = "",
    val authorName: String,
    val authorEmail: String,
    val workType: String = "NOVEL",
    val wordCount: Int = 0,
    val totalLeaves: Int = 0,
    val lastSyncedTimestamp: Long = System.currentTimeMillis(),
    val isPublicInCommunity: Boolean = true, // If false, hidden to all except author and Editor in Chief
    val version: String = "1.0",
    val driveFileUrl: String = "",
    val sharedWithEditorInChief: Boolean = true
)

data class SuspendedUserEntry(
    val email: String,
    val suspendedAt: Long = System.currentTimeMillis(),
    val reason: String = "Administrative suspension by Editor in Chief.",
    val suspendedBy: String = "real.artistry@gmail.com",
    val isLockedOut: Boolean = true
)

data class ServerlessMailMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val recipientEmail: String,
    val senderEmail: String = "real.artistry@gmail.com",
    val senderName: String = "Editor in Chief",
    val subject: String,
    val body: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val manuscriptId: Long? = null,
    val manuscriptTitle: String? = null,
    val messageType: String = "DIRECT_MESSAGE" // DIRECT_MESSAGE, BROADCAST, EDITORIAL_REVISION, GOVERNANCE_ALERT
)

data class DriveSyncStatus(
    val isSyncing: Boolean = false,
    val lastSyncTime: Long? = null,
    val lastSyncFileId: String? = null,
    val syncError: String? = null,
    val isSuspended: Boolean = false,
    val suspensionReason: String? = null,
    val unreadMailCount: Int = 0
)

data class BookVersionSnapshot(
    val snapshotId: String = java.util.UUID.randomUUID().toString(),
    val manuscriptId: Long,
    val title: String,
    val versionTag: String, // e.g. "v1.0", "v1.1", "v2.0-FINAL"
    val changeSummary: String, // e.g. "Synced to Google Drive with 14 chapters"
    val authorPenName: String,
    val authorEmail: String,
    val wordCount: Int,
    val totalLeaves: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val driveFileId: String = "",
    val driveUrl: String = ""
)

