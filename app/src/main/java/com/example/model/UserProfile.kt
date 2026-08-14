package com.example.model

enum class WorkRole(
    val title: String,
    val badgeLabel: String,
    val description: String
) {
    AUTHOR(
        title = "Author",
        badgeLabel = "Author / Creator",
        description = "Full creative control, chapter drafting, structure creation, and final sign-off."
    ),
    EDITOR(
        title = "Editor",
        badgeLabel = "Managing Editor",
        description = "Chicago Manual of Style compliance, copy-editing, structural remarks, and leaf pagination."
    ),
    CONTRIBUTOR(
        title = "Contributor",
        badgeLabel = "Foreword / Contributor",
        description = "Drafting forewords, prefaces, specific chapters, or specialized appendices."
    )
}

data class UserProfile(
    val id: String = "user_google_1",
    val email: String = "real.artistry@gmail.com",
    val name: String = "Dr. Arthur Vance",
    val penName: String = "A. V. Hawthorne",
    val role: WorkRole = WorkRole.AUTHOR,
    val avatarUrl: String = "",
    val organization: String = "Chicago Literary Guild",
    val preferredCmosEdition: String = "17th Edition"
)
