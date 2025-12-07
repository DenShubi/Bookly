package com.example.bookly.supabase

object SupabaseClientProvider {
    // Supabase configuration
    const val SUPABASE_URL = "https://jxxjdagjxinlwxnmagrd.supabase.co"
    const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imp4eGpkYWdqeGlubHd4bm1hZ3JkIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjQ3NTY2NzQsImV4cCI6MjA4MDMzMjY3NH0.0GjbBHPRpEPaWjQI8sVI_IJwymoFf05iotE73UALX7A"

    // Holds the current user's access token after login/signup
    @Volatile
    var currentAccessToken: String? = null

    fun headersWithAnon(): Map<String, String> {
        return mapOf(
            "apikey" to SUPABASE_ANON_KEY,
            "Authorization" to "Bearer $SUPABASE_ANON_KEY",
            "Content-Type" to "application/json"
        )
    }

    fun headersWithAuth(): Map<String, String> {
        val token = currentAccessToken ?: SUPABASE_ANON_KEY
        return mapOf(
            "apikey" to SUPABASE_ANON_KEY,
            "Authorization" to "Bearer $token",
            "Content-Type" to "application/json"
        )
    }
}
