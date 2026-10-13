package com.treefort.guardian

import com.treefort.guardian.data.Contact
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc

object SupabaseManager {
    val client: SupabaseClient by lazy {
        val url = System.getenv("SUPABASE_URL")?.takeIf { it.isNotBlank() }
            ?: "https://fqluqmrouysbosioqhhg.supabase.co"
        val key = System.getenv("SUPABASE_ANON_KEY")?.takeIf { it.isNotBlank() }
            ?: "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImZxbHVxbXJvdXlzYm9zaW9xaGhnIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODg0MjQ2NDcsImV4cCI6MjEwNDAwMDY0N30.j2eCkkVsW6-nko54sQRM9o7eOzfsWzpd-1Hgwg4SlPw"
        createSupabaseClient(
            supabaseUrl = url,
            supabaseKey = key
        ) {
            install(Postgrest)
            install(Auth)
        }
    }

    suspend fun signIn(email: String, password: String) {
        client.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    suspend fun signOut() {
        client.auth.signOut()
    }

    suspend fun fetchContacts(): List<Contact> {
        return client.postgrest["contact"]
            .select()
            .decodeList<Contact>()
    }

    suspend fun insertContact(contact: Contact) {
        try {
            client.postgrest["contact"].insert(contact)
        } catch (e: Exception) {
            // Log or fallback
        }
    }

    suspend fun deleteContact(id: Int) {
        try {
            client.postgrest["contact"].delete {
                filter {
                    eq("id", id)
                }
            }
        } catch (e: Exception) {
            // Log or fallback
        }
    }

    suspend fun scheduleAccountDeletion() {
        try {
            client.postgrest.rpc("schedule_account_deletion")
        } catch (_: Exception) {
            // Fallback or offline
        }
    }
}
