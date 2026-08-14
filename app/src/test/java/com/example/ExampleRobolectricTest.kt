package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.cmos.CmosFormatter
import com.example.cmos.CmosLeafEngine
import com.example.data.UserPreferences
import com.example.model.LeafDisplayType
import com.example.model.ManuscriptEntity
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
    fun `cmos leaf engine generates preliminary leaves with dynamic copyright`() {
        val manuscript = ManuscriptEntity(
            title = "A Chicago Treatise",
            authorName = "Real Name",
            authorPenName = "Nom De Plume",
            publisher = "Bwriter Editions",
            year = "2026"
        )
        val leaves = CmosLeafEngine.calculateLeaves(manuscript, emptyList())
        val copyrightLeaf = leaves.find { it.displayType == LeafDisplayType.COPYRIGHT }

        assertTrue(copyrightLeaf != null)
        assertTrue(copyrightLeaf!!.contentSnippet.contains("Nom De Plume"))
    }
}
