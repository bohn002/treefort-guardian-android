package com.treefort.guardian

import com.treefort.guardian.api.ApiException
import com.treefort.guardian.api.Contact
import com.treefort.guardian.api.TreeFortApi
import com.treefort.guardian.ui.AvatarCatalog
import com.treefort.guardian.ui.formatClaimCode
import com.treefort.guardian.ui.parseInstant
import com.treefort.guardian.ui.passwordTransformation
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import kotlinx.serialization.encodeToString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UnitTests {
    @Test fun passwordIsHiddenUntilToggled() {
        assertTrue(passwordTransformation(true, false) is PasswordVisualTransformation)
        assertEquals(VisualTransformation.None, passwordTransformation(true, true))
        assertEquals(VisualTransformation.None, passwordTransformation(false, false))
    }

    @Test fun claimCodeIsFormattedAsNnnNnn() {
        assertEquals("123-456", formatClaimCode("123456"))
        assertEquals("123-4", formatClaimCode("1234"))
        assertEquals("123-456", formatClaimCode("12a3-4567890"))
    }

    @Test fun backendDetailBecomesTheMessage() {
        assertEquals("Invite not found.", TreeFortApi.errorMessage(404, """{"detail":"Invite not found."}"""))
        assertEquals("Field required", TreeFortApi.errorMessage(422, """{"detail":[{"msg":"Field required"}]}"""))
    }

    @Test fun fallbackMessagesNeverLeakBodies() {
        assertEquals("You're signed out. Sign in again.", TreeFortApi.errorMessage(401, "<html>"))
        assertTrue(TreeFortApi.errorMessage(500, "Traceback ...").startsWith("TreeFort had a problem"))
        assertTrue(ApiException(409, "x").isConflict)
    }

    @Test fun contactDecodingIgnoresUnknownKeysAndNeverNeedsMac() {
        val c = TreeFortApi.json.decodeFromString<Contact>(
            """{"id":3,"name":"Uncle Bob","treefort_id":"*12345","avatar_icon":"guitar","avatar_color":"retro-grape","owner_id":9,"phone_number":null,"avatar_emoji":"x"}"""
        )
        assertEquals("*12345", c.treefortId)
        assertEquals("guitar", c.avatarIcon)
    }

    @Test fun contactUpdatesNeverSendTheTreefortNumber() {
        // The editor owns name, permissions, speed dial and avatar. treefort_id is not a field of the update body.
        val c = Contact(id = 1, name = "A", treefortId = "*11111")
        val encoded = TreeFortApi.json.encodeToString(c)
        assertTrue(encoded.contains("treefort_id")) // the model carries it for display only
        val src = java.io.File("src/main/java/com/treefort/guardian/api/TreeFortApi.kt").readText()
        val updateBody = src.substringAfter("suspend fun updateContact").substringBefore("suspend fun deleteContact")
        assertFalse(updateBody.contains("treefort_id"))
    }

    @Test fun contactDeleteGoesThroughTheBackendNotPostgrest() {
        val api = java.io.File("src/main/java/com/treefort/guardian/api/TreeFortApi.kt").readText()
        assertTrue(api.contains("\"DELETE\", \"/api/contacts/\$id\""))
        val manager = java.io.File("src/main/java/com/treefort/guardian/SupabaseManager.kt").readText()
        assertFalse(manager.contains("postgrest"))
        assertFalse(manager.contains("deleteContact"))
    }

    @Test fun guardianLockBacksOffAfterFiveMisses() {
        assertEquals(0, GuardianLock.backoffSeconds(4))
        assertEquals(30, GuardianLock.backoffSeconds(5))
        assertEquals(60, GuardianLock.backoffSeconds(6))
        assertEquals(900, GuardianLock.backoffSeconds(30))
        assertTrue(GuardianLock.validPin("1234"))
        assertFalse(GuardianLock.validPin("123"))
        assertFalse(GuardianLock.validPin("123456789"))
        assertFalse(GuardianLock.validPin("12a4"))
    }

    @Test fun unknownAvatarsFallBack() {
        assertEquals(AvatarCatalog.vector("user"), AvatarCatalog.vector("not-an-icon"))
        assertEquals(AvatarCatalog.preset("slate-mid"), AvatarCatalog.preset("nope"))
    }

    @Test fun timestampsParseWithAndWithoutOffset() {
        assertNotNull(parseInstant("2026-10-13T12:00:00Z"))
        assertNotNull(parseInstant("2026-10-13T12:00:00"))
        assertNotNull(parseInstant("2026-10-13T12:00:00+00:00"))
        assertNull(parseInstant("not a time"))
    }
}
