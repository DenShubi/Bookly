package com.example.bookly.supabase

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.ExperimentalTime

// Added OptIn to resolve the compiler error for Instant if required by your version
@OptIn(ExperimentalTime::class)
@Serializable
data class BookRequest(
    val id: String,
    @SerialName("user_id") val userId: String,
    val title: String,
    val author: String,
    @SerialName("publication_year") val publicationYear: String,
    val reason: String,
    @SerialName("cover_image_url") val coverImageUrl: String? = null,
    val status: String, // pending, approved, rejected
    @SerialName("review_note") val reviewNote: String? = null,
    @SerialName("submitted_at") val submittedAt: Instant
)

@Serializable
data class CreateBookRequestDto(
    @SerialName("user_id") val userId: String,
    val title: String,
    val author: String,
    @SerialName("publication_year") val publicationYear: String,
    val reason: String,
    @SerialName("cover_image_url") val coverImageUrl: String? = null,
    val status: String = "pending"
)

@Serializable
data class UpdateBookRequestStatusDto(
    val status: String,
    @SerialName("review_note") val reviewNote: String?
)

class BookRequestRepository(private val supabase: SupabaseClient) {

    suspend fun createRequest(
        title: String,
        author: String,
        year: String,
        reason: String,
        coverImageBytes: ByteArray?
    ): Result<Unit> {
        return try {
            // Corrected: Uses 'auth' plugin import instead of 'gotrue'
            val userId = supabase.auth.currentUserOrNull()?.id ?: throw Exception("User not logged in")

            var coverUrl: String? = null
            if (coverImageBytes != null) {
                // Using 'book-covers' bucket
                val fileName = "requests/${userId}_${System.currentTimeMillis()}.jpg"
                val bucket = supabase.storage.from("book-covers")
                bucket.upload(fileName, coverImageBytes) {
                    upsert = true
                }
                coverUrl = bucket.publicUrl(fileName)
            }

            val request = CreateBookRequestDto(
                userId = userId,
                title = title,
                author = author,
                publicationYear = year,
                reason = reason,
                coverImageUrl = coverUrl
            )

            // Corrected: Uses 'from' import for cleaner syntax matching your UserRepository
            supabase.from("book_requests").insert(request)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    @OptIn(ExperimentalTime::class)
    suspend fun getRequestsForAdmin(): Result<List<BookRequest>> {
        return try {
            val requests = supabase.from("book_requests")
                .select()
                .decodeList<BookRequest>()
                .sortedByDescending { it.submittedAt }
            Result.success(requests)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateStatus(requestId: String, status: String, note: String?): Result<Unit> {
        return try {
            val update = UpdateBookRequestStatusDto(status, note)
            supabase.from("book_requests")
                .update(update) {
                    filter { eq("id", requestId) }
                }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}