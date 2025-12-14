package com.example.bookly.supabase

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

object LoansRepository {
    private const val TAG = "LoansRepository"

    suspend fun borrowBook(bookId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val userId = getCurrentUserId()
                ?: return@withContext Result.failure(Exception("User not authenticated"))

            val url = "${SupabaseClientProvider.SUPABASE_URL}/rest/v1/loans"
            val payload = JSONObject().apply {
                put("user_id", userId)
                put("book_id", bookId)
                put("borrowed_at", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date()))
            }

            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                SupabaseClientProvider.headersWithAuth().forEach { (k, v) -> setRequestProperty(k, v) }
            }

            val body = payload.toString().toByteArray(Charsets.UTF_8)
            conn.setFixedLengthStreamingMode(body.size)
            conn.outputStream.use { it.write(body) }

            val code = conn.responseCode
            val resp = BufferedReader(InputStreamReader(
                if (code in 200..299) conn.inputStream else conn.errorStream
            )).use { it.readText() }

            if (code !in 200..299) {
                Log.w(TAG, "borrowBook failed: code=$code resp=$resp")
                return@withContext Result.failure(Exception("Failed to create loan: $resp"))
            }

            Result.success(Unit)
        } catch (t: Throwable) {
            Log.e(TAG, "Error borrowing book", t)
            Result.failure(t)
        }
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
            val resp = BufferedReader(InputStreamReader(
                if (code in 200..299) conn.inputStream else conn.errorStream
            )).use { it.readText() }

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
