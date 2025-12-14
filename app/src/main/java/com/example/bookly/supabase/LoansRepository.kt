package com.example.bookly.supabase

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

object LoansRepository {
    private const val TAG = "LoansRepository"

    private fun detectMissingTableError(resp: String): Exception? {
        return try {
            if (resp.contains("PGRST205") || resp.contains("Could not find the table")) {
                Exception("Tabel 'borrowing_records' tidak ditemukan di Supabase (PGRST205). Pastikan tabel 'borrowing_records' ada di schema public atau sesuaikan SUPABASE_URL.")
            } else null
        } catch (t: Throwable) { null }
    }

    suspend fun borrowBook(bookId: String): Result<LoanRow> = withContext(Dispatchers.IO) {
        try {
            val userId = getCurrentUserId() ?: return@withContext Result.failure(Exception("User not authenticated"))

            // check book availability
            val bookResp = BooksRepository.getBookById(bookId)
            val bookRow = bookResp.getOrNull() ?: return@withContext Result.failure(Exception("Buku tidak ditemukan"))
            if (bookRow.availableCopies <= 0) return@withContext Result.failure(Exception("Buku tidak tersedia"))

            // prepare payload using your columns: borrow_date and due_date
            val insertUrl = "${SupabaseClientProvider.SUPABASE_URL}/rest/v1/borrowing_records"
            val duration = 14
            val borrowDateStr = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date())
            val borrowDateForCalc = parseIsoToDate(borrowDateStr)
            val dueDateStr = borrowDateForCalc?.let {
                Calendar.getInstance().apply { time = it; add(Calendar.DAY_OF_YEAR, duration) }.time.let { d ->
                    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(d)
                }
            }

            val payload = JSONObject().apply {
                put("user_id", userId)
                put("book_id", bookId)
                put("borrow_date", borrowDateStr)
                dueDateStr?.let { put("due_date", it) }
            }

            // POST loan
            val insertConn = (URL(insertUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                SupabaseClientProvider.headersWithAuth().forEach { (k, v) -> setRequestProperty(k, v) }
                setRequestProperty("Prefer", "return=representation")
            }
            val body = payload.toString().toByteArray(Charsets.UTF_8)
            insertConn.setFixedLengthStreamingMode(body.size)
            insertConn.outputStream.use { it.write(body) }

            val insertCode = insertConn.responseCode
            val insertResp = BufferedReader(InputStreamReader(if (insertCode in 200..299) insertConn.inputStream else insertConn.errorStream)).use { it.readText() }
            if (insertCode !in 200..299) {
                Log.w(TAG, "borrowBook insert failed: code=$insertCode resp=$insertResp")
                detectMissingTableError(insertResp)?.let { return@withContext Result.failure(it) }
                return@withContext Result.failure(Exception("Gagal membuat peminjaman: $insertResp"))
            }

            // parse created loan (expect array representation)
            val createdLoanJson = try {
                val arr = JSONArray(insertResp)
                if (arr.length() > 0) arr.getJSONObject(0) else null
            } catch (t: Throwable) { null }

            // build LoanRow, prefer DB due_date if present
            val loanRow = createdLoanJson?.let { obj ->
                val id = obj.optString("id", "")
                val bookIdVal = obj.optString("book_id", "")
                val borrowDateResp = obj.optString("borrow_date", null)
                val dueDateResp = obj.optString("due_date", null)
                val status = obj.optString("status", "active")
                val extensionCount = obj.optInt("extension_count", 0)
                val maxExtensions = obj.optInt("max_extensions", 2)
                val borrowedAtDate = parseIsoToDate(borrowDateResp)
                val returnDeadline = parseIsoToDate(dueDateResp) ?: borrowedAtDate?.let { d ->
                    Calendar.getInstance().apply { time = d; add(Calendar.DAY_OF_YEAR, duration) }.time
                }
                val bookResp2 = BooksRepository.getBookById(bookIdVal)
                val bookRow2 = bookResp2.getOrNull()
                LoanRow(
                    id = id,
                    bookId = bookIdVal,
                    borrowedAt = borrowedAtDate,
                    returnDeadline = returnDeadline,
                    durationDays = duration,
                    bookTitle = bookRow2?.title,
                    bookAuthor = bookRow2?.author,
                    coverImageUrl = bookRow2?.coverImageUrl,
                    status = status,
                    extensionCount = extensionCount,
                    maxExtensions = maxExtensions
                )
            }

            if (loanRow != null) Result.success(loanRow) else Result.failure(Exception("Failed parsing created loan"))
        } catch (t: Throwable) {
            Log.e(TAG, "Error borrowing book", t)
            Result.failure(t)
        }
    }

    suspend fun getUserLoans(): Result<List<LoanRow>> = withContext(Dispatchers.IO) {
        try {
            val userId = getCurrentUserId() ?: return@withContext Result.failure(Exception("User not authenticated"))
            val url = "${SupabaseClientProvider.SUPABASE_URL}/rest/v1/borrowing_records?select=*&user_id=eq.$userId&order=borrow_date.desc"
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                SupabaseClientProvider.headersWithAuth().forEach { (k, v) -> setRequestProperty(k, v) }
            }
            val code = conn.responseCode
            val resp = BufferedReader(InputStreamReader(if (code in 200..299) conn.inputStream else conn.errorStream)).use { it.readText() }
            if (code !in 200..299) {
                detectMissingTableError(resp)?.let { return@withContext Result.failure(it) }
                return@withContext Result.failure(Exception("Failed fetching loans: $resp"))
            }

            val arr = JSONArray(resp)
            val list = mutableListOf<LoanRow>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val id = obj.optString("id", "")
                val bookIdVal = obj.optString("book_id", "")
                val borrowDate = obj.optString("borrow_date", "")
                val dueDateStr = obj.optString("due_date", null)
                val status = obj.optString("status", "active")
                val extensionCount = obj.optInt("extension_count", 0)
                val maxExtensions = obj.optInt("max_extensions", 2)
                val borrowedAtDate = parseIsoToDate(borrowDate)
                val duration = 14
                val returnDeadline = parseIsoToDate(dueDateStr) ?: borrowedAtDate?.let { d ->
                    Calendar.getInstance().apply { time = d; add(Calendar.DAY_OF_YEAR, duration) }.time
                }
                val bookResp2 = BooksRepository.getBookById(bookIdVal)
                val bookRow2 = bookResp2.getOrNull()
                list.add(
                    LoanRow(
                        id = id,
                        bookId = bookIdVal,
                        borrowedAt = borrowedAtDate,
                        returnDeadline = returnDeadline,
                        durationDays = duration,
                        bookTitle = bookRow2?.title,
                        bookAuthor = bookRow2?.author,
                        coverImageUrl = bookRow2?.coverImageUrl,
                        status = status,
                        extensionCount = extensionCount,
                        maxExtensions = maxExtensions
                    )
                )
            }

            Result.success(list)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    suspend fun getLoanById(loanId: String): Result<LoanRow> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseClientProvider.SUPABASE_URL}/rest/v1/borrowing_records?id=eq.$loanId&select=*"
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                SupabaseClientProvider.headersWithAuth().forEach { (k, v) -> setRequestProperty(k, v) }
            }
            val code = conn.responseCode
            val resp = BufferedReader(InputStreamReader(if (code in 200..299) conn.inputStream else conn.errorStream)).use { it.readText() }
            if (code !in 200..299) {
                detectMissingTableError(resp)?.let { return@withContext Result.failure(it) }
                return@withContext Result.failure(Exception(resp))
            }

            val arr = JSONArray(resp)
            if (arr.length() == 0) return@withContext Result.failure(Exception("Loan not found"))
            val obj = arr.getJSONObject(0)
            val id = obj.optString("id", "")
            val bookIdVal = obj.optString("book_id", "")
            val borrowedAt = parseIsoToDate(obj.optString("borrow_date", null))
            val dueDateStr = obj.optString("due_date", null)
            val status = obj.optString("status", "active")
            val extensionCount = obj.optInt("extension_count", 0)
            val maxExtensions = obj.optInt("max_extensions", 2)
            val duration = 14
            val returnDeadline = parseIsoToDate(dueDateStr) ?: borrowedAt?.let { d ->
                Calendar.getInstance().apply { time = d; add(Calendar.DAY_OF_YEAR, duration) }.time
            }
            val bookResp2 = BooksRepository.getBookById(bookIdVal)
            val bookRow2 = bookResp2.getOrNull()
            val loanRow = LoanRow(
                id = id,
                bookId = bookIdVal,
                borrowedAt = borrowedAt,
                returnDeadline = returnDeadline,
                durationDays = duration,
                bookTitle = bookRow2?.title,
                bookAuthor = bookRow2?.author,
                coverImageUrl = bookRow2?.coverImageUrl,
                status = status,
                extensionCount = extensionCount,
                maxExtensions = maxExtensions
            )
            Result.success(loanRow)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    suspend fun getActiveLoan(): Result<LoanRow> = withContext(Dispatchers.IO) {
        try {
            val userId = getCurrentUserId() ?: return@withContext Result.failure(Exception("User not authenticated"))
            val url = "${SupabaseClientProvider.SUPABASE_URL}/rest/v1/borrowing_records?select=*&user_id=eq.$userId&order=borrow_date.desc&limit=1"
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                SupabaseClientProvider.headersWithAuth().forEach { (k, v) -> setRequestProperty(k, v) }
            }
            val code = conn.responseCode
            val resp = BufferedReader(InputStreamReader(if (code in 200..299) conn.inputStream else conn.errorStream)).use { it.readText() }
            if (code !in 200..299) {
                detectMissingTableError(resp)?.let { return@withContext Result.failure(it) }
                return@withContext Result.failure(Exception(resp))
            }

            val arr = JSONArray(resp)
            if (arr.length() == 0) return@withContext Result.failure(Exception("No active loan"))
            val obj = arr.getJSONObject(0)
            val id = obj.optString("id", "")
            val bookIdVal = obj.optString("book_id", "")
            val borrowedAt = parseIsoToDate(obj.optString("borrow_date", null))
            val dueDateStr = obj.optString("due_date", null)
            val status = obj.optString("status", "active")
            val extensionCount = obj.optInt("extension_count", 0)
            val maxExtensions = obj.optInt("max_extensions", 2)
            val duration = 14
            val returnDeadline = parseIsoToDate(dueDateStr) ?: borrowedAt?.let { d ->
                Calendar.getInstance().apply { time = d; add(Calendar.DAY_OF_YEAR, duration) }.time
            }
            val bookResp2 = BooksRepository.getBookById(bookIdVal)
            val bookRow2 = bookResp2.getOrNull()
            val loanRow = LoanRow(
                id = id,
                bookId = bookIdVal,
                borrowedAt = borrowedAt,
                returnDeadline = returnDeadline,
                durationDays = duration,
                bookTitle = bookRow2?.title,
                bookAuthor = bookRow2?.author,
                coverImageUrl = bookRow2?.coverImageUrl,
                status = status,
                extensionCount = extensionCount,
                maxExtensions = maxExtensions
            )
            Result.success(loanRow)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    suspend fun returnBook(loanId: String, bookId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // mark loan as returned; server trigger will update book availability
            val payload = JSONObject().apply {
                put("status", "returned")
                put("return_date", SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()))
            }
            val patchUrl = "${SupabaseClientProvider.SUPABASE_URL}/rest/v1/borrowing_records?id=eq.$loanId"
            val conn = (URL(patchUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "PATCH"
                doOutput = true
                SupabaseClientProvider.headersWithAuth().forEach { (k, v) -> setRequestProperty(k, v) }
                setRequestProperty("Prefer", "return=representation")
            }
            val body = payload.toString().toByteArray(Charsets.UTF_8)
            conn.setFixedLengthStreamingMode(body.size)
            conn.outputStream.use { it.write(body) }
            val code = conn.responseCode
            val resp = BufferedReader(InputStreamReader(if (code in 200..299) conn.inputStream else conn.errorStream)).use { it.readText() }
            if (code !in 200..299) {
                detectMissingTableError(resp)?.let { return@withContext Result.failure(it) }
                return@withContext Result.failure(Exception("Failed updating loan return: $resp"))
            }

            Result.success(Unit)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    suspend fun extendLoan(loanId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val loanRes = getLoanById(loanId)
            val loan = loanRes.getOrNull() ?: return@withContext Result.failure(Exception("Loan not found"))
            val currentReturn = loan.returnDeadline ?: return@withContext Result.failure(Exception("No return deadline available"))
            if ((loan.extensionCount ?: 0) >= (loan.maxExtensions ?: 2)) return@withContext Result.failure(Exception("Maximum extensions reached"))
            val newReturn = Calendar.getInstance().apply { time = currentReturn; add(Calendar.DAY_OF_YEAR, loan.durationDays) }.time

            // update due_date and increment extension_count
            val patchUrl = "${SupabaseClientProvider.SUPABASE_URL}/rest/v1/borrowing_records?id=eq.$loanId"
            val newCount = (loan.extensionCount ?: 0) + 1
            val payload = JSONObject().apply {
                put("due_date", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(newReturn))
                put("extension_count", newCount)
            }
            val conn = (URL(patchUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "PATCH"
                doOutput = true
                SupabaseClientProvider.headersWithAuth().forEach { (k, v) -> setRequestProperty(k, v) }
                setRequestProperty("Prefer", "return=representation")
            }
            val body = payload.toString().toByteArray(Charsets.UTF_8)
            conn.setFixedLengthStreamingMode(body.size)
            conn.outputStream.use { it.write(body) }
            val code = conn.responseCode
            val resp = BufferedReader(InputStreamReader(if (code in 200..299) conn.inputStream else conn.errorStream)).use { it.readText() }
            if (code !in 200..299) {
                detectMissingTableError(resp)?.let { return@withContext Result.failure(it) }
                return@withContext Result.failure(Exception("Failed extending loan: $resp"))
            }
            Result.success(Unit)
        } catch (t: Throwable) {
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
            val token = SupabaseClientProvider.currentAccessToken ?: return null
            val url = "${SupabaseClientProvider.SUPABASE_URL}/auth/v1/user"
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("apikey", SupabaseClientProvider.SUPABASE_ANON_KEY)
            }
            val code = conn.responseCode
            val resp = BufferedReader(InputStreamReader(if (code in 200..299) conn.inputStream else conn.errorStream)).use { it.readText() }
            if (code in 200..299) {
                val json = JSONObject(resp)
                val userId = json.optString("id", "")
                if (userId.isNotBlank()) userId else null
            } else {
                null
            }
        } catch (t: Throwable) {
            null
        }
    }
}