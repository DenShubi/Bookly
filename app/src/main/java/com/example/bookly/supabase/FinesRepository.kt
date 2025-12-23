package com.example.bookly.supabase

import android.util.Log
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.storage.upload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.*

object FinesRepository {
    private const val TAG = "FinesRepository"

    @Serializable
    data class FineRow(
        val id: String = "",
        @SerialName("user_id") val userId: String = "",
        @SerialName("borrowing_record_id") val borrowingRecordId: String? = null,
        @SerialName("book_id") val bookId: String? = null,
        @SerialName("due_date") val dueDate: String? = null,
        @SerialName("issued_date") val issuedDate: String? = null,
        val amount: Double = 0.0,
        val description: String = "",
        @SerialName("payment_proof_url") val paymentProofUrl: String? = null,
        @SerialName("verification_note") val verificationNote: String? = null,
        val status: String = "unpaid", // unpaid, pending, paid, rejected
        @SerialName("created_at") val createdAt: String? = null,
        @SerialName("updated_at") val updatedAt: String? = null
    )

    @Serializable
    data class FineInsert(
        @SerialName("user_id") val userId: String,
        @SerialName("borrowing_record_id") val borrowingRecordId: String? = null,
        @SerialName("book_id") val bookId: String? = null,
        @SerialName("due_date") val dueDate: String? = null,
        val amount: Double,
        val description: String,
        val status: String = "unpaid"
    )

    @Serializable
    data class FineUpdate(
        val status: String? = null,
        @SerialName("payment_proof_url") val paymentProofUrl: String? = null,
        @SerialName("verification_note") val verificationNote: String? = null
    )

    @Serializable
    data class PaymentSubmissionRow(
        val id: String = "",
        @SerialName("user_id") val userId: String = "",
        @SerialName("fine_id") val fineId: String = "",
        @SerialName("amount_paid") val amountPaid: Double = 0.0,
        @SerialName("payment_method") val paymentMethod: String? = null,
        @SerialName("payment_proof_url") val paymentProofUrl: String? = null,
        @SerialName("verification_status") val verificationStatus: String = "pending", // pending, approved, rejected
        @SerialName("verification_notes") val verificationNotes: String? = null,
        @SerialName("verified_by") val verifiedBy: String? = null,
        @SerialName("verified_at") val verifiedAt: String? = null,
        @SerialName("submission_date") val submissionDate: String? = null,
        @SerialName("created_at") val createdAt: String? = null,
        @SerialName("updated_at") val updatedAt: String? = null
    )

    @Serializable
    data class PaymentSubmissionInsert(
        @SerialName("user_id") val userId: String,
        @SerialName("fine_id") val fineId: String,
        @SerialName("amount_paid") val amountPaid: Double,
        @SerialName("payment_method") val paymentMethod: String? = "Bank Transfer",
        @SerialName("payment_proof_url") val paymentProofUrl: String
    )

    /**
     * Get all fines for the current user
     */
    suspend fun getUserFines(): Result<List<FineRow>> = withContext(Dispatchers.IO) {
        try {
            val userId = getCurrentUserId()
                ?: return@withContext Result.failure(Exception("User not authenticated"))

            val client = SupabaseClientProvider.client
            val fines = client.from("fines").select {
                filter { eq("user_id", userId) }
                order("created_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
            }.decodeList<FineRow>()

            Result.success(fines)
        } catch (t: Throwable) {
            Log.e(TAG, "Error fetching user fines", t)
            Result.failure(t)
        }
    }

    /**
     * Get a specific fine by ID
     */
    suspend fun getFineById(fineId: String): Result<FineRow> = withContext(Dispatchers.IO) {
        try {
            val client = SupabaseClientProvider.client
            val fine = client.from("fines").select {
                filter { eq("id", fineId) }
            }.decodeSingleOrNull<FineRow>()
                ?: return@withContext Result.failure(Exception("Fine not found"))

            Result.success(fine)
        } catch (t: Throwable) {
            Log.e(TAG, "Error fetching fine by ID", t)
            Result.failure(t)
        }
    }

    /**
     * Get fine by borrowing record ID
     */
    suspend fun getFineByBorrowingRecordId(borrowingRecordId: String): Result<FineRow?> = withContext(Dispatchers.IO) {
        try {
            val client = SupabaseClientProvider.client
            val fine = client.from("fines").select {
                filter {
                    eq("borrowing_record_id", borrowingRecordId)
                }
                order("created_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                limit(1)
            }.decodeSingleOrNull<FineRow>()

            Result.success(fine)
        } catch (t: Throwable) {
            Log.e(TAG, "Error fetching fine by borrowing record ID", t)
            Result.failure(t)
        }
    }

    /**
     * Upload payment proof image to Supabase storage
     */
    suspend fun uploadPaymentProof(fineId: String, imageBytes: ByteArray): Result<String> = withContext(Dispatchers.IO) {
        try {
            val userId = getCurrentUserId()
                ?: return@withContext Result.failure(Exception("User not authenticated"))

            val client = SupabaseClientProvider.client
            val bucket = client.storage.from("payment-proof")

            val timestamp = System.currentTimeMillis()
            val fileName = "payment_${userId}_${fineId}_${timestamp}.jpg"

            bucket.upload(fileName, imageBytes) {
                upsert = false
            }

            // Get public URL
            val publicUrl = bucket.publicUrl(fileName)

            Result.success(publicUrl)
        } catch (t: Throwable) {
            Log.e(TAG, "Error uploading payment proof", t)
            Result.failure(t)
        }
    }

    /**
     * Submit payment proof for a fine
     */
    suspend fun submitPaymentProof(
        fineId: String,
        imageBytes: ByteArray,
        amountPaid: Double
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val userId = getCurrentUserId()
                ?: return@withContext Result.failure(Exception("User not authenticated"))

            // Upload image first
            val uploadResult = uploadPaymentProof(fineId, imageBytes)
            if (uploadResult.isFailure) {
                return@withContext Result.failure(uploadResult.exceptionOrNull()
                    ?: Exception("Failed to upload image"))
            }

            val imageUrl = uploadResult.getOrNull()!!

            val client = SupabaseClientProvider.client

            // Create payment submission
            val submission = PaymentSubmissionInsert(
                userId = userId,
                fineId = fineId,
                amountPaid = amountPaid,
                paymentProofUrl = imageUrl,
                paymentMethod = "Bank Transfer - BCA"
            )

            client.from("payment_submissions").insert(submission)

            // Update fine status to pending
            val fineUpdate = FineUpdate(
                status = "pending",
                paymentProofUrl = imageUrl
            )

            client.from("fines").update(fineUpdate) {
                filter { eq("id", fineId) }
            }

            Result.success(Unit)
        } catch (t: Throwable) {
            Log.e(TAG, "Error submitting payment proof", t)
            Result.failure(t)
        }
    }

    /**
     * Create or update fine for overdue loan
     */
    suspend fun createOrUpdateFineForLoan(
        borrowingRecordId: String,
        bookId: String,
        amount: Double,
        description: String
    ): Result<FineRow> = withContext(Dispatchers.IO) {
        try {
            val userId = getCurrentUserId()
                ?: return@withContext Result.failure(Exception("User not authenticated"))

            val client = SupabaseClientProvider.client

            // Check if fine already exists for this borrowing record
            val existingFine = client.from("fines").select {
                filter {
                    eq("borrowing_record_id", borrowingRecordId)
                    eq("status", "unpaid")
                }
            }.decodeSingleOrNull<FineRow>()

            if (existingFine != null) {
                // Update existing fine amount
                val update = mapOf(
                    "amount" to amount,
                    "description" to description
                )
                client.from("fines").update(update) {
                    filter { eq("id", existingFine.id) }
                }

                val updated = getFineById(existingFine.id).getOrNull()
                    ?: return@withContext Result.failure(Exception("Failed to get updated fine"))
                Result.success(updated)
            } else {
                // Create new fine
                val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

                val fineInsert = FineInsert(
                    userId = userId,
                    borrowingRecordId = borrowingRecordId,
                    bookId = bookId,
                    dueDate = currentDate,
                    amount = amount,
                    description = description,
                    status = "unpaid"
                )

                val created = client.from("fines").insert(fineInsert) {
                    select()
                }.decodeSingle<FineRow>()

                Result.success(created)
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Error creating/updating fine", t)
            Result.failure(t)
        }
    }

    /**
     * Get all pending payment submissions (for admin)
     */
    suspend fun getAllPendingSubmissions(): Result<List<PaymentSubmissionWithDetails>> = withContext(Dispatchers.IO) {
        try {
            val client = SupabaseClientProvider.client
            val submissions = client.from("payment_submissions").select {
                filter { eq("verification_status", "pending") }
                order("submission_date", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
            }.decodeList<PaymentSubmissionRow>()

            val withDetails = submissions.map { submission ->
                val fine = getFineById(submission.fineId).getOrNull()
                val user = fine?.userId?.let {
                    UsersRepository.getUserById(it).getOrNull()
                }
                val book = fine?.bookId?.let {
                    BooksRepository.getBookById(it).getOrNull()
                }

                PaymentSubmissionWithDetails(
                    submission = submission,
                    fine = fine,
                    userName = user?.name,
                    userEmail = user?.email,
                    bookTitle = book?.title
                )
            }

            Result.success(withDetails)
        } catch (t: Throwable) {
            Log.e(TAG, "Error fetching pending submissions", t)
            Result.failure(t)
        }
    }

    /**
     * Verify payment submission (admin only)
     */
    suspend fun verifyPaymentSubmission(
        submissionId: String,
        approved: Boolean,
        notes: String?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val adminId = getCurrentUserId()
                ?: return@withContext Result.failure(Exception("User not authenticated"))

            val client = SupabaseClientProvider.client

            // Get submission to find fine ID
            val submission = client.from("payment_submissions").select {
                filter { eq("id", submissionId) }
            }.decodeSingle<PaymentSubmissionRow>()

            val currentTime = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }.format(Date())

            // Update payment submission
            val submissionUpdate = mapOf(
                "verification_status" to if (approved) "approved" else "rejected",
                "verification_notes" to (notes ?: ""),
                "verified_by" to adminId,
                "verified_at" to currentTime
            )

            client.from("payment_submissions").update(submissionUpdate) {
                filter { eq("id", submissionId) }
            }

            // Update fine status
            val fineUpdate = if (approved) {
                FineUpdate(
                    status = "paid",
                    verificationNote = notes
                )
            } else {
                FineUpdate(
                    status = "rejected",
                    verificationNote = notes
                )
            }

            client.from("fines").update(fineUpdate) {
                filter { eq("id", submission.fineId) }
            }

            Result.success(Unit)
        } catch (t: Throwable) {
            Log.e(TAG, "Error verifying payment", t)
            Result.failure(t)
        }
    }

    data class PaymentSubmissionWithDetails(
        val submission: PaymentSubmissionRow,
        val fine: FineRow?,
        val userName: String?,
        val userEmail: String?,
        val bookTitle: String?
    )

    private fun getCurrentUserId(): String? {
        return try {
            val client = SupabaseClientProvider.client
            client.auth.currentUserOrNull()?.id
        } catch (t: Throwable) {
            Log.e(TAG, "Error getting current user ID", t)
            null
        }
    }
}

