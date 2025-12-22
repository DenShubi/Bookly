package com.example.bookly.supabase

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage

object SupabaseClientProvider {
    private const val SUPABASE_URL = "https://jxxjdagjxinlwxnmagrd.supabase.co"
    private const val SUPABASE_ANON_KEY = "YOUR_ANON_KEY"

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
