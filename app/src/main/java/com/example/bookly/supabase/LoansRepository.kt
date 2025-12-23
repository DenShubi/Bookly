package com.example.bookly.supabase

import android.util.Log
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.text.SimpleDateFormat
import java.util.*

object LoansRepository {
    private const val TAG = "LoansRepository"

    @Serializable
    data class BorrowingRecordRow(
        val id: String = "",
        @SerialName("user_id") val userId: String = "",
        @SerialName("book_id") val bookId: String = "",
        @SerialName("borrowed_at") val borrowedAt: String? = null,
        @SerialName("due_at") val dueAt: String? = null,
        @SerialName("returned_at") val returnedAt: String? = null,
        val status: String = "borrowed",
        @SerialName("fine_amount") val fineAmount: Double? = 0.0
    )

    @Serializable
    data class BorrowingRecordInsert(
        @SerialName("user_id") val userId: String,
        @SerialName("book_id") val bookId: String,
        @SerialName("borrowed_at") val borrowedAt: String,
        @SerialName("due_at") val dueAt: String? = null
    )

    @Serializable
    data class BorrowingRecordUpdate(
        val status: String? = null,
        @SerialName("returned_at") val returnedAt: String? = null,
        @SerialName("due_at") val dueAt: String? = null
    )


    suspend fun borrowBook(bookId: String): Result<LoanRow> = withContext(Dispatchers.IO) {
        try {
            val userId = getCurrentUserId() ?: return@withContext Result.failure(Exception("User not authenticated"))

            // check book availability
            val bookResp = BooksRepository.getBookById(bookId)
            val bookRow = bookResp.getOrNull() ?: return@withContext Result.failure(Exception("Buku tidak ditemukan"))
            if (bookRow.availableCopies <= 0) return@withContext Result.failure(Exception("Buku tidak tersedia"))

            // prepare dates - use WIB timezone
            val duration = 14
            val wibTimeZone = TimeZone.getTimeZone("Asia/Jakarta")
            val now = Date()

            // Convert current WIB time to UTC for Supabase
            val borrowDateStr = getCurrentTimeInUTC()

            // Calculate due date in WIB, then convert to UTC
            val dueCalendar = Calendar.getInstance(wibTimeZone).apply {
                time = now
                add(Calendar.DAY_OF_YEAR, duration)
            }
            val dueDateStr = dateToUTC(dueCalendar.time)

            val client = SupabaseClientProvider.client
            val insertData = BorrowingRecordInsert(
                userId = userId,
                bookId = bookId,
                borrowedAt = borrowDateStr,
                dueAt = dueDateStr
            )

            // Insert and get response
            val createdRecords = client.from("borrowing_records").insert(insertData) {
                select()
            }.decodeList<BorrowingRecordRow>()

            val createdRecord = createdRecords.firstOrNull()
                ?: return@withContext Result.failure(Exception("Failed to create borrowing record"))

            // Build LoanRow
            val borrowedAtDate = parseIsoToDate(createdRecord.borrowedAt)
            val returnDeadline = parseIsoToDate(createdRecord.dueAt) ?: borrowedAtDate?.let { d ->
                Calendar.getInstance().apply { time = d; add(Calendar.DAY_OF_YEAR, duration) }.time
            }

            val bookRow2 = BooksRepository.getBookById(createdRecord.bookId).getOrNull()

            // Calculate late fee
            val lateFeeResult = calculateLateFee(createdRecord.id)
            val lateFee = lateFeeResult.getOrNull() ?: 0.0

            val loanRow = LoanRow(
                id = createdRecord.id,
                bookId = createdRecord.bookId,
                borrowedAt = borrowedAtDate,
                returnDeadline = returnDeadline,
                durationDays = duration,
                bookTitle = bookRow2?.title,
                bookAuthor = bookRow2?.author,
                coverImageUrl = bookRow2?.coverImageUrl,
                status = createdRecord.status,
                extensionCount = 0,
                maxExtensions = 2,
                lateFee = lateFee
            )

            Result.success(loanRow)
        } catch (t: Throwable) {
            Log.e(TAG, "Error borrowing book", t)
            Result.failure(t)
        }
    }

    suspend fun getUserLoans(): Result<List<LoanRow>> = withContext(Dispatchers.IO) {
        try {
            val userId = getCurrentUserId() ?: return@withContext Result.failure(Exception("User not authenticated"))

            val client = SupabaseClientProvider.client
            val records = client.from("borrowing_records").select {
                filter { eq("user_id", userId) }
                order("borrowed_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
            }.decodeList<BorrowingRecordRow>()

            val list = records.map { record ->
                val borrowedAtDate = parseIsoToDate(record.borrowedAt)
                val duration = 14
                val returnDeadline = parseIsoToDate(record.dueAt) ?: borrowedAtDate?.let { d ->
                    Calendar.getInstance().apply { time = d; add(Calendar.DAY_OF_YEAR, duration) }.time
                }
                val bookRow2 = BooksRepository.getBookById(record.bookId).getOrNull()

                // Calculate late fee
                val lateFeeResult = calculateLateFee(record.id)
                val lateFee = lateFeeResult.getOrNull() ?: 0.0

                LoanRow(
                    id = record.id,
                    bookId = record.bookId,
                    borrowedAt = borrowedAtDate,
                    returnDeadline = returnDeadline,
                    durationDays = duration,
                    bookTitle = bookRow2?.title,
                    bookAuthor = bookRow2?.author,
                    coverImageUrl = bookRow2?.coverImageUrl,
                    status = record.status,
                    extensionCount = 0,
                    maxExtensions = 2,
                    lateFee = lateFee
                )
            }

            Result.success(list)
        } catch (t: Throwable) {
            Log.e(TAG, "Error fetching user loans", t)
            Result.failure(t)
        }
    }

    suspend fun getLoanById(loanId: String): Result<LoanRow> = withContext(Dispatchers.IO) {
        try {
            val client = SupabaseClientProvider.client
            val record = client.from("borrowing_records").select {
                filter { eq("id", loanId) }
            }.decodeSingleOrNull<BorrowingRecordRow>()
                ?: return@withContext Result.failure(Exception("Loan not found"))

            val borrowedAt = parseIsoToDate(record.borrowedAt)
            val duration = 14
            val returnDeadline = parseIsoToDate(record.dueAt) ?: borrowedAt?.let { d ->
                Calendar.getInstance().apply { time = d; add(Calendar.DAY_OF_YEAR, duration) }.time
            }
            val bookRow2 = BooksRepository.getBookById(record.bookId).getOrNull()

            // Calculate late fee
            val lateFeeResult = calculateLateFee(record.id)
            val lateFee = lateFeeResult.getOrNull() ?: 0.0

            val loanRow = LoanRow(
                id = record.id,
                bookId = record.bookId,
                borrowedAt = borrowedAt,
                returnDeadline = returnDeadline,
                durationDays = duration,
                bookTitle = bookRow2?.title,
                bookAuthor = bookRow2?.author,
                coverImageUrl = bookRow2?.coverImageUrl,
                status = record.status,
                extensionCount = 0,
                maxExtensions = 2,
                lateFee = lateFee
            )
            Result.success(loanRow)
        } catch (t: Throwable) {
            Log.e(TAG, "Error getting loan by ID", t)
            Result.failure(t)
        }
    }

    suspend fun getActiveLoan(): Result<LoanRow> = withContext(Dispatchers.IO) {
        try {
            val userId = getCurrentUserId() ?: return@withContext Result.failure(Exception("User not authenticated"))

            val client = SupabaseClientProvider.client
            val record = client.from("borrowing_records").select {
                filter { eq("user_id", userId) }
                order("borrowed_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                limit(1)
            }.decodeSingleOrNull<BorrowingRecordRow>()
                ?: return@withContext Result.failure(Exception("No active loan"))

            val borrowedAt = parseIsoToDate(record.borrowedAt)
            val duration = 14
            val returnDeadline = parseIsoToDate(record.dueAt) ?: borrowedAt?.let { d ->
                Calendar.getInstance().apply { time = d; add(Calendar.DAY_OF_YEAR, duration) }.time
            }
            val bookRow2 = BooksRepository.getBookById(record.bookId).getOrNull()

            // Calculate late fee
            val lateFeeResult = calculateLateFee(record.id)
            val lateFee = lateFeeResult.getOrNull() ?: 0.0

            val loanRow = LoanRow(
                id = record.id,
                bookId = record.bookId,
                borrowedAt = borrowedAt,
                returnDeadline = returnDeadline,
                durationDays = duration,
                bookTitle = bookRow2?.title,
                bookAuthor = bookRow2?.author,
                coverImageUrl = bookRow2?.coverImageUrl,
                status = record.status,
                extensionCount = 0,
                maxExtensions = 2,
                lateFee = lateFee
            )
            Result.success(loanRow)
        } catch (t: Throwable) {
            Log.e(TAG, "Error getting active loan", t)
            Result.failure(t)
        }
    }

    suspend fun returnBook(loanId: String, @Suppress("UNUSED_PARAMETER") bookId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val client = SupabaseClientProvider.client
            val updateData = BorrowingRecordUpdate(
                status = "returned",
                returnedAt = getCurrentTimeInUTC()
            )

            client.from("borrowing_records").update(updateData) {
                filter { eq("id", loanId) }
            }

            Result.success(Unit)
        } catch (t: Throwable) {
            Log.e(TAG, "Error returning book", t)
            Result.failure(t)
        }
    }

    suspend fun extendLoan(loanId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val loanRes = getLoanById(loanId)
            val loan = loanRes.getOrNull() ?: return@withContext Result.failure(Exception("Loan not found"))
            val currentReturn = loan.returnDeadline ?: return@withContext Result.failure(Exception("No return deadline available"))
            if ((loan.extensionCount ?: 0) >= (loan.maxExtensions ?: 2)) {
                return@withContext Result.failure(Exception("Maximum extensions reached"))
            }

            // Extend in WIB timezone
            val wibTimeZone = TimeZone.getTimeZone("Asia/Jakarta")
            val newReturn = Calendar.getInstance(wibTimeZone).apply {
                time = currentReturn
                add(Calendar.DAY_OF_YEAR, loan.durationDays)
            }.time

            val client = SupabaseClientProvider.client
            val updateData = BorrowingRecordUpdate(
                dueAt = dateToUTC(newReturn)
            )

            client.from("borrowing_records").update(updateData) {
                filter { eq("id", loanId) }
            }

            Result.success(Unit)
        } catch (t: Throwable) {
            Log.e(TAG, "Error extending loan", t)
            Result.failure(t)
        }
    }

    /**
     * Calculate late fee for a borrowing record by calling Supabase function.
     * @param borrowingRecordId The ID of the borrowing record
     * @param hourlyRate The hourly rate for late fees (default: 0.10)
     * @return The calculated late fee amount
     */
    suspend fun calculateLateFee(borrowingRecordId: String, hourlyRate: Double = 0.10): Result<Double> = withContext(Dispatchers.IO) {
        try {
            val client = SupabaseClientProvider.client

            // Call the calculate_late_fee RPC function
            val params = buildJsonObject {
                put("p_borrowing_record_id", borrowingRecordId)
                put("p_hourly_rate", hourlyRate)
            }

            val response = client.postgrest.rpc("calculate_late_fee", params).decodeAs<Double>()

            Result.success(response)
        } catch (t: Throwable) {
            Log.e(TAG, "Error calculating late fee", t)
            Result.failure(t)
        }
    }

    /**
     * Get all overdue loans (for admin view)
     */
    suspend fun getAllOverdueLoans(): Result<List<LoanRow>> = withContext(Dispatchers.IO) {
        try {
            val client = SupabaseClientProvider.client
            val records = client.from("borrowing_records").select {
                filter { eq("status", "overdue") }
                order("due_at", io.github.jan.supabase.postgrest.query.Order.ASCENDING)
            }.decodeList<BorrowingRecordRow>()

            val list = records.map { record ->
                val borrowedAtDate = parseIsoToDate(record.borrowedAt)
                val duration = 14
                val returnDeadline = parseIsoToDate(record.dueAt) ?: borrowedAtDate?.let { d ->
                    Calendar.getInstance().apply { time = d; add(Calendar.DAY_OF_YEAR, duration) }.time
                }
                val bookRow2 = BooksRepository.getBookById(record.bookId).getOrNull()

                // Calculate late fee
                val lateFeeResult = calculateLateFee(record.id)
                val lateFee = lateFeeResult.getOrNull() ?: 0.0

                LoanRow(
                    id = record.id,
                    bookId = record.bookId,
                    borrowedAt = borrowedAtDate,
                    returnDeadline = returnDeadline,
                    durationDays = duration,
                    bookTitle = bookRow2?.title,
                    bookAuthor = bookRow2?.author,
                    coverImageUrl = bookRow2?.coverImageUrl,
                    status = record.status,
                    extensionCount = 0,
                    maxExtensions = 2,
                    lateFee = lateFee
                )
            }

            Result.success(list)
        } catch (t: Throwable) {
            Log.e(TAG, "Error fetching overdue loans", t)
            Result.failure(t)
        }
    }

    data class LoanRow(
        val id: String = "",
        val bookId: String = "",
        val borrowedAt: Date? = null,
        val returnDeadline: Date? = null, // maps to due_date
        val durationDays: Int = 14,
        val bookTitle: String? = null,
        val bookAuthor: String? = null,
        val coverImageUrl: String? = null,
        val status: String? = null,
        val extensionCount: Int? = 0,
        val maxExtensions: Int? = 2,
        val lateFee: Double? = null
    )

    /**
     * Parse ISO date string from Supabase (UTC) and convert to WIB (UTC+7)
     */
    private fun parseIsoToDate(iso: String?): Date? {
        if (iso == null) return null
        val candidates = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd"
        )
        for (fmt in candidates) {
            try {
                val sdf = SimpleDateFormat(fmt, Locale.US)
                // Parse as UTC (Supabase database timezone)
                sdf.timeZone = TimeZone.getTimeZone("UTC")
                val cleaned = iso
                // handle fractional seconds by splitting on '.'
                val toParse = cleaned.split(".")[0] + if (fmt.contains("'Z'") && cleaned.endsWith("Z")) "Z" else ""
                val utcDate = sdf.parse(toParse)

                // Convert UTC to WIB (UTC+7)
                // The Date object is already in UTC, we just return it
                // The conversion to WIB will happen when displaying
                return utcDate
            } catch (_: Throwable) {
                // try next
            }
        }
        return null
    }

    /**
     * Convert current WIB time to UTC for Supabase
     */
    private fun getCurrentTimeInUTC(): String {
        val now = Date()
        return SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(now)
    }

    /**
     * Convert Date to UTC string for Supabase
     */
    private fun dateToUTC(date: Date): String {
        return SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(date)
    }

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