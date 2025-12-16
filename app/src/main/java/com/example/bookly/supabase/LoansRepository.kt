package com.example.bookly.supabase

import android.util.Log
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.*

object LoansRepository {
    private const val TAG = "LoansRepository"

    @Serializable
    data class BorrowingRecordRow(
        val id: String = "",
        @SerialName("user_id") val userId: String = "",
        @SerialName("book_id") val bookId: String = "",
        @SerialName("borrow_date") val borrowDate: String? = null,
        @SerialName("due_date") val dueDate: String? = null,
        @SerialName("return_date") val returnDate: String? = null,
        val status: String = "active",
        @SerialName("extension_count") val extensionCount: Int = 0,
        @SerialName("max_extensions") val maxExtensions: Int = 2
    )

    @Serializable
    data class BorrowingRecordInsert(
        @SerialName("user_id") val userId: String,
        @SerialName("book_id") val bookId: String,
        @SerialName("borrow_date") val borrowDate: String,
        @SerialName("due_date") val dueDate: String? = null
    )

    @Serializable
    data class BorrowingRecordUpdate(
        val status: String? = null,
        @SerialName("return_date") val returnDate: String? = null,
        @SerialName("due_date") val dueDate: String? = null,
        @SerialName("extension_count") val extensionCount: Int? = null
    )


    suspend fun borrowBook(bookId: String): Result<LoanRow> = withContext(Dispatchers.IO) {
        try {
            val userId = getCurrentUserId() ?: return@withContext Result.failure(Exception("User not authenticated"))

            // check book availability
            val bookResp = BooksRepository.getBookById(bookId)
            val bookRow = bookResp.getOrNull() ?: return@withContext Result.failure(Exception("Buku tidak ditemukan"))
            if (bookRow.availableCopies <= 0) return@withContext Result.failure(Exception("Buku tidak tersedia"))

            // prepare dates
            val duration = 14
            val borrowDateStr = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }.format(Date())
            val borrowDateForCalc = parseIsoToDate(borrowDateStr)
            val dueDateStr = borrowDateForCalc?.let {
                Calendar.getInstance().apply { time = it; add(Calendar.DAY_OF_YEAR, duration) }.time.let { d ->
                    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                        timeZone = TimeZone.getTimeZone("UTC")
                    }.format(d)
                }
            }

            val client = SupabaseClientProvider.client
            val insertData = BorrowingRecordInsert(
                userId = userId,
                bookId = bookId,
                borrowDate = borrowDateStr,
                dueDate = dueDateStr
            )

            // Insert and get response
            val createdRecords = client.from("borrowing_records").insert(insertData) {
                select()
            }.decodeList<BorrowingRecordRow>()

            val createdRecord = createdRecords.firstOrNull()
                ?: return@withContext Result.failure(Exception("Failed to create borrowing record"))

            // Build LoanRow
            val borrowedAtDate = parseIsoToDate(createdRecord.borrowDate)
            val returnDeadline = parseIsoToDate(createdRecord.dueDate) ?: borrowedAtDate?.let { d ->
                Calendar.getInstance().apply { time = d; add(Calendar.DAY_OF_YEAR, duration) }.time
            }

            val bookRow2 = BooksRepository.getBookById(createdRecord.bookId).getOrNull()
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
                extensionCount = createdRecord.extensionCount,
                maxExtensions = createdRecord.maxExtensions
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
                order("borrow_date", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
            }.decodeList<BorrowingRecordRow>()

            val list = records.map { record ->
                val borrowedAtDate = parseIsoToDate(record.borrowDate)
                val duration = 14
                val returnDeadline = parseIsoToDate(record.dueDate) ?: borrowedAtDate?.let { d ->
                    Calendar.getInstance().apply { time = d; add(Calendar.DAY_OF_YEAR, duration) }.time
                }
                val bookRow2 = BooksRepository.getBookById(record.bookId).getOrNull()

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
                    extensionCount = record.extensionCount,
                    maxExtensions = record.maxExtensions
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

            val borrowedAt = parseIsoToDate(record.borrowDate)
            val duration = 14
            val returnDeadline = parseIsoToDate(record.dueDate) ?: borrowedAt?.let { d ->
                Calendar.getInstance().apply { time = d; add(Calendar.DAY_OF_YEAR, duration) }.time
            }
            val bookRow2 = BooksRepository.getBookById(record.bookId).getOrNull()

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
                extensionCount = record.extensionCount,
                maxExtensions = record.maxExtensions
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
                order("borrow_date", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                limit(1)
            }.decodeSingleOrNull<BorrowingRecordRow>()
                ?: return@withContext Result.failure(Exception("No active loan"))

            val borrowedAt = parseIsoToDate(record.borrowDate)
            val duration = 14
            val returnDeadline = parseIsoToDate(record.dueDate) ?: borrowedAt?.let { d ->
                Calendar.getInstance().apply { time = d; add(Calendar.DAY_OF_YEAR, duration) }.time
            }
            val bookRow2 = BooksRepository.getBookById(record.bookId).getOrNull()

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
                extensionCount = record.extensionCount,
                maxExtensions = record.maxExtensions
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
                returnDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
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

            val newReturn = Calendar.getInstance().apply {
                time = currentReturn
                add(Calendar.DAY_OF_YEAR, loan.durationDays)
            }.time
            val newCount = (loan.extensionCount ?: 0) + 1

            val client = SupabaseClientProvider.client
            val updateData = BorrowingRecordUpdate(
                dueDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }.format(newReturn),
                extensionCount = newCount
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
        val maxExtensions: Int? = 2
    )

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
                if (fmt.endsWith("'Z'")) sdf.timeZone = TimeZone.getTimeZone("UTC")
                val cleaned = iso
                // handle fractional seconds by splitting on '.'
                val toParse = cleaned.split(".")[0] + if (fmt.contains("'Z'") && cleaned.endsWith("Z")) "Z" else ""
                return sdf.parse(toParse)
            } catch (_: Throwable) {
                // try next
            }
        }
        return null
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