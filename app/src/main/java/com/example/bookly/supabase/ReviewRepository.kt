package com.example.bookly.supabase

import android.util.Log
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.storage.storage
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

object ReviewRepository {

    // --- Model Data untuk Database (Sesuai tabel 'reviews' + join 'users') ---
    @Serializable
    data class ReviewRow(
        val id: String,
        @SerialName("user_id") val userId: String,
        @SerialName("book_id") val bookId: String,
        val rating: Int,
        @SerialName("review_text") val reviewText: String? = null,
        @SerialName("photo_urls") val photoUrls: List<String> = emptyList(),
        @SerialName("created_at") val createdAt: String,

        // Relasi ke tabel users (untuk ambil nama)
        // Supabase akan mengembalikan ini sebagai object nested jika kita minta
        val users: UserData? = null
    )

    @Serializable
    data class UserData(
        @SerialName("full_name") val fullName: String?
    )

    // --- Model untuk Input Data (Insert) ---
    @Serializable
    data class ReviewInsert(
        @SerialName("user_id") val userId: String,
        @SerialName("book_id") val bookId: String,
        val rating: Int,
        @SerialName("review_text") val reviewText: String,
        @SerialName("photo_urls") val photoUrls: List<String>
    )

    // 1. Ambil Review berdasarkan ID Buku
    suspend fun getReviewsByBookId(bookId: String): Result<List<ReviewRow>> {
        return try {
            val client = SupabaseClientProvider.client ?: throw Exception("Supabase not initialized")

            // Kita select kolom reviews dan join ke users untuk ambil full_name
            val reviews = client.from("reviews").select(
                columns = Columns.list(
                    "id", "user_id", "book_id", "rating", "review_text", "photo_urls", "created_at",
                    "users(full_name)" // Join ke tabel users
                )
            ) {
                filter {
                    eq("book_id", bookId)
                }
                order("created_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
            }.decodeList<ReviewRow>()

            Result.success(reviews)
        } catch (e: Exception) {
            Log.e("ReviewRepository", "Error fetching reviews: ${e.message}")
            Result.failure(e)
        }
    }

    // 2. Kirim Review Baru
    suspend fun submitReview(
        bookId: String,
        rating: Int,
        reviewText: String,
        photoUrls: List<String> = emptyList()
    ): Result<Boolean> {
        return try {
            val client = SupabaseClientProvider.client ?: throw Exception("Supabase not initialized")
            val currentUser = client.auth.currentUserOrNull() ?: throw Exception("User not logged in")

            val newReview = ReviewInsert(
                userId = currentUser.id,
                bookId = bookId,
                rating = rating,
                reviewText = reviewText,
                photoUrls = photoUrls
            )

            client.from("reviews").insert(newReview)
            Result.success(true)
        } catch (e: Exception) {
            Log.e("ReviewRepository", "Error submitting review: ${e.message}")
            Result.failure(e)
        }
    }

    // 3. Upload Foto (Nanti dipanggil sebelum submitReview jika ada foto)
    suspend fun uploadReviewPhoto(byteArray: ByteArray, fileName: String): Result<String> {
        return try {
            val client = SupabaseClientProvider.client ?: throw Exception("Supabase not initialized")
            val bucket = client.storage.from("review-images")

            bucket.upload(fileName, byteArray)

            // Dapatkan URL Public
            val publicUrl = bucket.publicUrl(fileName)
            Result.success(publicUrl)
        } catch (e: Exception) {
            Log.e("ReviewRepository", "Error uploading photo: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun deleteReview(reviewId: String): Result<Boolean> {
        return try {
            val client = SupabaseClientProvider.client
            // Kita tidak perlu cek user manual, karena Supabase RLS di SQL akan menolak otomatis jika bukan miliknya
            client.from("reviews").delete {
                filter {
                    eq("id", reviewId)
                }
            }
            Result.success(true)
        } catch (e: Exception) {
            Log.e("ReviewRepository", "Error deleting review: ${e.message}")
            Result.failure(e)
        }
    }

    // Helper untuk mendapatkan ID user yang sedang login (untuk pengecekan di UI)
    fun getCurrentUserId(): String? {
        return SupabaseClientProvider.client.auth.currentUserOrNull()?.id
    }
}