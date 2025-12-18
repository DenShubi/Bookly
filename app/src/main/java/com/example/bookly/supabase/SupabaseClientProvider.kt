package com.example.bookly.supabase

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
// Import tambahan untuk Engine Android & Serializer JSON
import io.ktor.client.engine.android.Android
import io.github.jan.supabase.serializer.KotlinXSerializer
import kotlinx.serialization.json.Json

object SupabaseClientProvider {
    // --- KONFIGURASI UMUM ---
    const val SUPABASE_URL = "https://jxxjdagjxinlwxnmagrd.supabase.co"
    // Token Anonim Anda (Pastikan ini benar)
    const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imp4eGpkYWdqeGlubHd4bm1hZ3JkIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjQ3NTY2NzQsImV4cCI6MjA4MDMzMjY3NH0.0GjbBHPRpEPaWjQI8sVI_IJwymoFf05iotE73UALX7A"

    // --- SISTEM LAMA (Manual Token untuk Wishlist & Profile) ---
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

    // --- SISTEM BARU (Library Client untuk Review, Storage, & Request) ---
    val client = createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = SUPABASE_ANON_KEY
    ) {
        // 1. Pasang Engine Android agar koneksi stabil
        httpEngine = Android.create()

        // 2. Pasang Serializer yang "pemaaf" (ignoreUnknownKeys = true)
        // Ini mencegah crash jika tabel database punya kolom baru yg belum ada di DTO
        defaultSerializer = KotlinXSerializer(Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        })

        install(Auth)      // Plugin Auth
        install(Postgrest) // Plugin Database
        install(Storage)   // Plugin Storage
    }
}