package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.cmos.CmosFormatter
import com.example.cmos.CmosLeafEngine
import com.example.data.UserPreferences
import com.example.model.BookTrimSize
import com.example.model.LeafDisplayType
import com.example.model.ManuscriptEntity
import com.example.model.MatterType
import com.example.model.SectionEntity
import com.example.model.SectionType
import com.example.model.UserProfile
import com.example.model.WorkRole
import com.example.model.WorkType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Bwriter", appName)
    }

    @Test
    fun `user preferences saves and restores pen name`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = UserPreferences(context)
        val profile = UserProfile(
            name = "Jane Doe",
            penName = "J. D. Salinger",
            email = "jane@example.com",
            role = WorkRole.AUTHOR
        )
        prefs.saveUserProfile(profile)

        val restored = prefs.getUserProfile()
        assertEquals("Jane Doe", restored.name)
        assertEquals("J. D. Salinger", restored.penName)
        assertEquals("J. D. Salinger", restored.displayName)
    }

    @Test
    fun `manuscript entity generates dynamic copyright using pen name`() {
        val manuscript = ManuscriptEntity(
            title = "A Study in Typography",
            authorName = "Jane Doe",
            authorPenName = "J. D. Cross",
            publisher = "Press of Chicago",
            year = "2026"
        )
        assertEquals("J. D. Cross", manuscript.effectiveAuthorByline)
        assertTrue(manuscript.effectiveCopyrightText.contains("Copyright © 2026 by J. D. Cross"))
        assertTrue(manuscript.effectiveCopyrightText.contains("Press of Chicago"))
    }

    @Test
    fun `cmos formatter headline casing and roman numerals`() {
        assertEquals("The Old Man and the Sea", CmosFormatter.toChicagoHeadlineCase("the old man and the sea"))
        assertEquals("iv", CmosFormatter.toRoman(4))
        assertEquals("xiv", CmosFormatter.toRoman(14))
    }

    @Test
    fun `cmos leaf engine recalculates leaves based on trim size`() {
        val manuscriptPocket = ManuscriptEntity(
            title = "Pocket Guide",
            authorName = "Author",
            targetPageSize = BookTrimSize.POCKET_4_25X6_87.displayName
        )
        val manuscriptRoyal = ManuscriptEntity(
            title = "Royal Edition",
            authorName = "Author",
            targetPageSize = BookTrimSize.ROYAL_8X10.displayName
        )

        val sampleWords = (1..800).joinToString(" ") { "word$it" }
        val section = listOf(
            SectionEntity(
                manuscriptId = 1,
                matterType = MatterType.TEXT_BODY,
                sectionType = SectionType.CHAPTER,
                title = "Chapter 1",
                orderIndex = 1,
                content = sampleWords
            )
        )

        val leavesPocket = CmosLeafEngine.calculateLeaves(manuscriptPocket, section)
        val leavesRoyal = CmosLeafEngine.calculateLeaves(manuscriptRoyal, section)

        // Pocket format has smaller capacity per page, thus generating more leaves than Royal 8x10
        assertTrue(leavesPocket.size > leavesRoyal.size)
    }

    @Test
    fun `cmos pagination preserves paragraphs and handles multi-paragraph prose`() {
        val trimSize = BookTrimSize.TRADE_6X9
        val prose = "Paragraph one with some text.\n\nParagraph two with dialogue.\n\n“Watch out!” called the voice.\n\nParagraph four continues."
        val pages = CmosLeafEngine.paginateSectionProse(prose, trimSize)
        assertTrue(pages.isNotEmpty())
        assertTrue(pages[0].contains("Paragraph one"))
        assertTrue(pages[0].contains("Watch out!"))
    }

    @Test
    fun `user preferences handles multiple google accounts and login logout session state`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = UserPreferences(context)

        // Initially logged out
        prefs.setLoggedIn(false)
        assertEquals(false, prefs.isLoggedIn())

        // Save active session
        val account1 = UserProfile(
            name = "Jane Doe",
            penName = "J. D. Salinger",
            email = "author1@gmail.com",
            role = WorkRole.AUTHOR
        )
        prefs.addOrUpdateSavedAccount(account1)
        prefs.saveUserProfile(account1)
        prefs.setLoggedIn(true)
        assertEquals(true, prefs.isLoggedIn())

        // Add second account
        val account2 = UserProfile(
            name = "Editor Boss",
            penName = "E. Boss",
            email = "editor@bwriter.io",
            role = WorkRole.EDITOR
        )
        prefs.addOrUpdateSavedAccount(account2)

        val accounts = prefs.getSavedGoogleAccounts()
        assertTrue(accounts.any { it.email.equals("author1@gmail.com", ignoreCase = true) })
        assertTrue(accounts.any { it.email.equals("editor@bwriter.io", ignoreCase = true) })

        // Sign out
        prefs.signOut()
        assertEquals(false, prefs.isLoggedIn())
    }
}
