package com.example.bookly.supabase

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage

object SupabaseClientProvider {
    private const val SUPABASE_URL = "https://jxxjdagjxinlwxnmagrd.supabase.co"
    private const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imp4eGpkYWdqeGlubHd4bm1hZ3JkIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjQ3NTY2NzQsImV4cCI6MjA4MDMzMjY3NH0.0GjbBHPRpEPaWjQI8sVI_IJwymoFf05iotE73UALX7A"

    // Store current access token for manual usage
    var currentAccessToken: String? = null

    val client: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = SUPABASE_URL,
            supabaseKey = SUPABASE_ANON_KEY
        ) {
            install(Auth)
            install(Postgrest)
            install(Storage) // wajib untuk akses Storage
        }
    }
}
