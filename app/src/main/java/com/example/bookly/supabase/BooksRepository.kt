package com.example.bookly.supabase

import android.util.Log
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

object BooksRepository {

    // --- Model Data Buku (MERGED: Lengkap sesuai kode lama + tambahan untuk Home) ---
    @Serializable
    data class BookRow(
        val id: String = "",
        val title: String = "",
        val author: String = "",
        val publisher: String? = null,
        @SerialName("publication_year") val publicationYear: Int? = null,
        val language: String? = null,
        val pages: Int? = null,
        val category: String? = null,
        @SerialName("category_color") val categoryColor: String? = null,
        val description: String? = null,
        @SerialName("cover_image_url") val coverImageUrl: String? = null,
        val rating: Float = 0f,
        @SerialName("total_copies") val totalCopies: Int = 0,
        @SerialName("available_copies") val availableCopies: Int = 0,
        // Tambahan untuk fitur rekomendasi (terbaru)
        @SerialName("created_at") val createdAt: String? = null
    )

    // Model User untuk ambil nama
    @Serializable
    data class UserProfile(
        @SerialName("full_name") val fullName: String?
    )

    // 1. Ambil Semua Buku (Untuk Katalog)
    suspend fun getBooks(): Result<List<BookRow>> = withContext(Dispatchers.IO) {
        try {
            val client = SupabaseClientProvider.client
            // Menggunakan SDK lebih simpel daripada Ktor manual
            val books = client.from("books").select().decodeList<BookRow>()
            Result.success(books)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 2. Ambil Detail Buku
    suspend fun getBookById(bookId: String): Result<BookRow?> = withContext(Dispatchers.IO) {
        try {
            val client = SupabaseClientProvider.client
            val book = client.from("books").select {
                filter { eq("id", bookId) }
            }.decodeSingleOrNull<BookRow>()
            Result.success(book)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 3. Ambil Buku Populer (Top 5 Rating Tertinggi) - BARU
    suspend fun getPopularBooks(): Result<List<BookRow>> = withContext(Dispatchers.IO) {
        try {
            val client = SupabaseClientProvider.client
            val books = client.from("books").select {
                order("rating", Order.DESCENDING) // Urutkan rating tertinggi
                limit(5) // Ambil 5 saja
            }.decodeList<BookRow>()
            Result.success(books)
        } catch (e: Exception) {
            Log.e("BooksRepo", "Error popular: ${e.message}")
            Result.failure(e)
        }
    }

    // 4. Ambil Rekomendasi (Top 5 Buku Terbaru) - BARU
    suspend fun getRecommendedBooks(): Result<List<BookRow>> = withContext(Dispatchers.IO) {
        try {
            val client = SupabaseClientProvider.client
            val books = client.from("books").select {
                // Pastikan ada kolom created_at di tabel books Supabase Anda
                // Jika tidak ada, bisa ganti order by 'title' atau lainnya
                order("created_at", Order.DESCENDING)
                limit(5)
            }.decodeList<BookRow>()
            Result.success(books)
        } catch (e: Exception) {
            Log.e("BooksRepo", "Error recommended: ${e.message}")
            // Fallback: Jika gagal sort by date, ambil sembarang
            getPopularBooks()
        }
    }

    // 5. Ambil Nama User yang Login - BARU
    suspend fun getCurrentUserName(): String {
        return try {
            val client = SupabaseClientProvider.client
            val user = client.auth.currentUserOrNull() ?: return "Pengunjung"

            // Coba ambil nama dari tabel users
            val profile = client.from("users").select(columns = Columns.list("full_name")) {
                filter { eq("id", user.id) }
            }.decodeSingleOrNull<UserProfile>()

            // Jika nama kosong, pakai bagian depan email atau "User"
            profile?.fullName ?: user.email?.substringBefore("@") ?: "User"
        } catch (e: Exception) {
            Log.e("BooksRepo", "Error user name: ${e.message}")
            "User"
        }
    }
}