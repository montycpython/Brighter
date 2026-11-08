package com.supernatural.bwriter.data

import android.content.Context
import android.util.Log
import androidx.activity.result.IntentSenderRequest
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class GoogleDriveClient(context: Context) {

    private val appContext: Context = context.applicationContext

    companion object {
        private const val TAG = "GoogleDriveClient"
        private const val DRIVE_FILE_SCOPE = "https://www.googleapis.com/auth/drive.file"
        private const val FOLDER_NAME = "Bwriter"
        private const val FOLDER_MIME = "application/vnd.google-apps.folder"
        private const val KEY_BACKUP_NAME = "bwriter-author-key.json"
        private const val FILES_URL = "https://www.googleapis.com/drive/v3/files"
        private const val UPLOAD_URL = "https://www.googleapis.com/upload/drive/v3/files"
    }

    suspend fun accessToken(interactive: Boolean): Result<String> = withContext(Dispatchers.IO) {
        try {
            val client = Identity.getAuthorizationClient(appContext)
            val request = AuthorizationRequest.builder()
                .setRequestedScopes(listOf(Scope(DRIVE_FILE_SCOPE)))
                .build()
            var result = client.authorize(request).await()
            if (result.hasResolution()) {
                if (!interactive) {
                    return@withContext Result.failure(IllegalStateException("Google Drive access not yet granted"))
                }
                val pendingIntent = result.pendingIntent
                    ?: return@withContext Result.failure(IllegalStateException("Google Drive consent unavailable"))
                val granted = DriveConsentBridge.requestConsent(
                    IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                )
                if (!granted) {
                    return@withContext Result.failure(IllegalStateException("Google Drive access not granted"))
                }
                result = client.authorize(request).await()
            }
            val token = result.accessToken
                ?: return@withContext Result.failure(IllegalStateException("No Google Drive access token"))
            Result.success(token)
        } catch (e: Exception) {
            Log.e(TAG, "Drive authorization failed", e)
            Result.failure(e)
        }
    }

    suspend fun saveBook(
        token: String,
        bookKey: String,
        title: String,
        payloadJson: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val folderId = ensureFolder(token)
            val existingId = findBookFileId(token, folderId, bookKey)
            if (existingId != null) {
                val (dlCode, dlContent) = http("GET", "$FILES_URL/$existingId?alt=media", token, null, null)
                if (dlCode in 200..299 && dlContent.isNotBlank()) {
                    val existingUpdatedAt = try { JSONObject(dlContent).optLong("updatedAt", 0L) } catch (e: Exception) { 0L }
                    val newUpdatedAt = try { JSONObject(payloadJson).optLong("updatedAt", 0L) } catch (e: Exception) { 0L }
                    if (existingUpdatedAt > newUpdatedAt) {
                        return@withContext Result.failure(
                            IllegalStateException("Drive copy of '$title' is newer than this device. Restore it before syncing.")
                        )
                    }
                }
                val (code, _) = http(
                    "PATCH",
                    "$UPLOAD_URL/$existingId?uploadType=media",
                    token,
                    payloadJson.toByteArray(Charsets.UTF_8),
                    "application/json"
                )
                if (code !in 200..299) {
                    return@withContext Result.failure(IllegalStateException("Drive update failed ($code)"))
                }
                Result.success(existingId)
            } else {
                val metadata = JSONObject().apply {
                    put("name", sanitizeFileName(title) + ".json")
                    put("mimeType", "application/json")
                    put("parents", JSONArray().put(folderId))
                    put("appProperties", JSONObject().apply {
                        put("app", "bwriter")
                        put("birthId", bookKey)
                    })
                }
                val boundary = "bwriter-" + System.currentTimeMillis()
                val body = buildMultipart(boundary, metadata.toString(), payloadJson)
                val (code, response) = http(
                    "POST",
                    "$UPLOAD_URL?uploadType=multipart",
                    token,
                    body,
                    "multipart/related; boundary=$boundary"
                )
                if (code !in 200..299) {
                    return@withContext Result.failure(IllegalStateException("Drive create failed ($code)"))
                }
                val id = JSONObject(response).optString("id")
                if (id.isBlank()) {
                    return@withContext Result.failure(IllegalStateException("Drive create returned no file id"))
                }
                Result.success(id)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Drive save failed", e)
            Result.failure(e)
        }
    }

    suspend fun deleteBook(token: String, bookKey: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val folderId = ensureFolder(token)
            val existingId = findBookFileId(token, folderId, bookKey)
            if (existingId != null) {
                val (code, _) = http("DELETE", "$FILES_URL/$existingId", token, null, null)
                if (code !in 200..299 && code != 404) {
                    return@withContext Result.failure(IllegalStateException("Drive delete failed (HTTP $code)"))
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun downloadAllPayloads(token: String): List<String> = withContext(Dispatchers.IO) {
        try {
            val folderId = findFolderId(token) ?: return@withContext emptyList()
            val payloads = mutableListOf<String>()
            var pageToken: String? = null
            do {
                val q = URLEncoder.encode("'$folderId' in parents and trashed = false", "UTF-8")
                var url = "$FILES_URL?q=$q&fields=nextPageToken,files(id,name)&pageSize=100"
                if (pageToken != null) {
                    url += "&pageToken=" + URLEncoder.encode(pageToken, "UTF-8")
                }
                val (code, response) = http("GET", url, token, null, null)
                if (code !in 200..299) break
                val root = JSONObject(response)
                val files = root.optJSONArray("files") ?: JSONArray()
                for (i in 0 until files.length()) {
                    val fileObj = files.getJSONObject(i)
                    if (fileObj.optString("name") == KEY_BACKUP_NAME) continue
                    val id = fileObj.optString("id")
                    if (id.isBlank()) continue
                    val (downloadCode, content) = http("GET", "$FILES_URL/$id?alt=media", token, null, null)
                    if (downloadCode in 200..299 && content.isNotBlank()) {
                        payloads.add(content)
                    }
                }
                pageToken = root.optString("nextPageToken").ifBlank { null }
            } while (pageToken != null)
            payloads
        } catch (e: Exception) {
            Log.e(TAG, "Drive download failed", e)
            emptyList()
        }
    }

    suspend fun uploadKeyBackup(token: String, json: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val folderId = ensureFolder(token)
            val existingId = findFileIdByName(token, folderId, KEY_BACKUP_NAME)
            if (existingId != null) {
                val (code, _) = http(
                    "PATCH",
                    "$UPLOAD_URL/$existingId?uploadType=media",
                    token,
                    json.toByteArray(Charsets.UTF_8),
                    "application/json"
                )
                if (code !in 200..299) {
                    return@withContext Result.failure(IllegalStateException("Key backup update failed ($code)"))
                }
            } else {
                val metadata = JSONObject().apply {
                    put("name", KEY_BACKUP_NAME)
                    put("mimeType", "application/json")
                    put("parents", JSONArray().put(folderId))
                }
                val boundary = "bwriter-" + System.currentTimeMillis()
                val body = buildMultipart(boundary, metadata.toString(), json)
                val (code, response) = http(
                    "POST",
                    "$UPLOAD_URL?uploadType=multipart",
                    token,
                    body,
                    "multipart/related; boundary=$boundary"
                )
                if (code !in 200..299) {
                    return@withContext Result.failure(IllegalStateException("Key backup create failed ($code)"))
                }
                if (JSONObject(response).optString("id").isBlank()) {
                    return@withContext Result.failure(IllegalStateException("Key backup create returned no file id"))
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Key backup upload failed", e)
            Result.failure(e)
        }
    }

    suspend fun downloadKeyBackup(token: String): String? = withContext(Dispatchers.IO) {
        try {
            val folderId = findFolderId(token) ?: return@withContext null
            val fileId = findFileIdByName(token, folderId, KEY_BACKUP_NAME) ?: return@withContext null
            val (code, content) = http("GET", "$FILES_URL/$fileId?alt=media", token, null, null)
            if (code in 200..299 && content.isNotBlank()) content else null
        } catch (e: Exception) {
            Log.e(TAG, "Key backup download failed", e)
            null
        }
    }

    private fun sanitizeFileName(title: String): String {
        val cleaned = title.replace(Regex("[\\\\/:*?\"<>|]"), " ").trim().replace(Regex("\\s+"), " ")
        return cleaned.ifBlank { "Untitled Book" }
    }

    private fun buildMultipart(boundary: String, metadataJson: String, payloadJson: String): ByteArray {
        val sb = StringBuilder()
        sb.append("--").append(boundary).append("\r\n")
        sb.append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
        sb.append(metadataJson).append("\r\n")
        sb.append("--").append(boundary).append("\r\n")
        sb.append("Content-Type: application/json\r\n\r\n")
        sb.append(payloadJson).append("\r\n")
        sb.append("--").append(boundary).append("--")
        return sb.toString().toByteArray(Charsets.UTF_8)
    }

    private suspend fun ensureFolder(token: String): String {
        val existing = findFolderId(token)
        if (existing != null) return existing
        val metadata = JSONObject().apply {
            put("name", FOLDER_NAME)
            put("mimeType", FOLDER_MIME)
        }
        val (code, response) = http(
            "POST", FILES_URL, token,
            metadata.toString().toByteArray(Charsets.UTF_8), "application/json"
        )
        if (code !in 200..299) {
            throw IllegalStateException("Drive folder create failed ($code)")
        }
        return JSONObject(response).getString("id")
    }

    private suspend fun findFolderId(token: String): String? {
        val q = URLEncoder.encode(
            "mimeType = '$FOLDER_MIME' and name = '$FOLDER_NAME' and trashed = false", "UTF-8"
        )
        val (code, response) = http("GET", "$FILES_URL?q=$q&fields=files(id,name)", token, null, null)
        if (code !in 200..299) return null
        val files = JSONObject(response).optJSONArray("files") ?: return null
        if (files.length() == 0) return null
        return files.getJSONObject(0).optString("id").ifBlank { null }
    }

    private suspend fun findFileIdByName(token: String, folderId: String, name: String): String? {
        val rawQuery = "name = '$name' and '$folderId' in parents and trashed = false"
        val q = URLEncoder.encode(rawQuery, "UTF-8")
        val (code, response) = http("GET", "$FILES_URL?q=$q&fields=files(id,name)", token, null, null)
        if (code !in 200..299) return null
        val files = JSONObject(response).optJSONArray("files") ?: return null
        if (files.length() == 0) return null
        return files.getJSONObject(0).optString("id").ifBlank { null }
    }

    private suspend fun findBookFileId(token: String, folderId: String, bookKey: String): String? {
        val rawQuery = "appProperties has { key='birthId' and value='$bookKey' } " +
            "and '$folderId' in parents and trashed = false"
        val q = URLEncoder.encode(rawQuery, "UTF-8")
        val (code, response) = http("GET", "$FILES_URL?q=$q&fields=files(id,name)", token, null, null)
        if (code !in 200..299) return null
        val files = JSONObject(response).optJSONArray("files") ?: return null
        if (files.length() == 0) return null
        return files.getJSONObject(0).optString("id").ifBlank { null }
    }

    private fun http(
        method: String,
        urlString: String,
        token: String,
        body: ByteArray?,
        contentType: String?
    ): Pair<Int, String> {
        val connection = URL(urlString).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.connectTimeout = 30000
            connection.readTimeout = 60000
            if (body != null) {
                connection.doOutput = true
                if (contentType != null) {
                    connection.setRequestProperty("Content-Type", contentType)
                }
                connection.outputStream.use { it.write(body) }
            }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
            return Pair(code, text)
        } finally {
            connection.disconnect()
        }
    }
}
