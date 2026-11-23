package com.supernatural.bwriter.data

object IllustrationUris {
    fun isOwnBook(authorEmail: String, userEmail: String): Boolean =
        authorEmail.isNotBlank() && userEmail.isNotBlank() && authorEmail.equals(userEmail, ignoreCase = true)

    fun isLoadable(uriString: String, ownBook: Boolean): Boolean {
        if (ownBook) return true
        return uriString.startsWith("drawable:") || uriString.startsWith("content://")
    }
}
