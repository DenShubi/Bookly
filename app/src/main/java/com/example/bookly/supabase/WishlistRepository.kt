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

/**
 * Repository for managing wishlist operations with Supabase.
 * Handles CRUD operations for the wishlist table.
 */
object WishlistRepository {
    private const val DEBUG_LOG = true
    private const val TAG = "WishlistRepository"

    /**
     * Fetches all wishlist items for the currently authenticated user.
     * @return Result containing list of book IDs in the user's wishlist
     */
    suspend fun getWishlist(): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val userId = getCurrentUserId()
                ?: return@withContext Result.failure(Exception("User not authenticated"))

            val url = "${SupabaseClientProvider.SUPABASE_URL}/rest/v1/wishlist?select=book_id&user_id=eq.$userId"
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                SupabaseClientProvider.headersWithAuth().forEach { (k, v) ->
                    setRequestProperty(k, v)
                }
            }

            val code = conn.responseCode
            val resp = BufferedReader(InputStreamReader(
                if (code in 200..299) conn.inputStream else conn.errorStream
            )).use { it.readText() }

            if (DEBUG_LOG) Log.d(TAG, "getWishlist response code=$code body=$resp")

            if (code !in 200..299) {
                return@withContext Result.failure(Exception("Failed to fetch wishlist: $resp"))
            }

            val arr = JSONArray(resp)
            val bookIds = mutableListOf<String>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val bookId = obj.optString("book_id", "")
                if (bookId.isNotBlank()) {
                    bookIds.add(bookId)
                }
            }

            Result.success(bookIds)
        } catch (t: Throwable) {
            if (DEBUG_LOG) Log.e(TAG, "Error fetching wishlist", t)
            Result.failure(t)
        }
    }

    /**
     * Adds a book to the user's wishlist.
     * @param bookId The UUID of the book to add
     * @return Result indicating success or failure
     */
    suspend fun addToWishlist(bookId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val userId = getCurrentUserId()
                ?: return@withContext Result.failure(Exception("User not authenticated"))

            val url = "${SupabaseClientProvider.SUPABASE_URL}/rest/v1/wishlist"
            val payload = JSONObject().apply {
                put("user_id", userId)
                put("book_id", bookId)
            }

            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                SupabaseClientProvider.headersWithAuth().forEach { (k, v) ->
                    setRequestProperty(k, v)
                }
                // Add Prefer header to handle conflicts gracefully
                setRequestProperty("Prefer", "resolution=ignore-duplicates")
            }

            val bodyBytes = payload.toString().toByteArray(Charsets.UTF_8)
            conn.setFixedLengthStreamingMode(bodyBytes.size)

            if (DEBUG_LOG) Log.d(TAG, "addToWishlist body=$payload")

            conn.outputStream.use { os ->
                os.write(bodyBytes)
                os.flush()
            }

            val code = conn.responseCode
            val resp = BufferedReader(InputStreamReader(
                if (code in 200..299) conn.inputStream else conn.errorStream
            )).use { it.readText() }

            if (DEBUG_LOG) Log.d(TAG, "addToWishlist response code=$code body=$resp")

            if (code !in 200..299) {
                return@withContext Result.failure(Exception("Failed to add to wishlist: $resp"))
            }

            Result.success(Unit)
        } catch (t: Throwable) {
            if (DEBUG_LOG) Log.e(TAG, "Error adding to wishlist", t)
            Result.failure(t)
        }
    }

    /**
     * Removes a book from the user's wishlist.
     * @param bookId The UUID of the book to remove
     * @return Result indicating success or failure
     */
    suspend fun removeFromWishlist(bookId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val userId = getCurrentUserId()
                ?: return@withContext Result.failure(Exception("User not authenticated"))

            val url = "${SupabaseClientProvider.SUPABASE_URL}/rest/v1/wishlist?user_id=eq.$userId&book_id=eq.$bookId"
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "DELETE"
                SupabaseClientProvider.headersWithAuth().forEach { (k, v) ->
                    setRequestProperty(k, v)
                }
            }

            val code = conn.responseCode
            val resp = if (code in 200..299) {
                conn.inputStream?.use { BufferedReader(InputStreamReader(it)).readText() } ?: ""
            } else {
                conn.errorStream?.use { BufferedReader(InputStreamReader(it)).readText() } ?: ""
            }

            if (DEBUG_LOG) Log.d(TAG, "removeFromWishlist response code=$code body=$resp")

            if (code !in 200..299) {
                return@withContext Result.failure(Exception("Failed to remove from wishlist: $resp"))
            }

            Result.success(Unit)
        } catch (t: Throwable) {
            if (DEBUG_LOG) Log.e(TAG, "Error removing from wishlist", t)
            Result.failure(t)
        }
    }

    /**
     * Gets the current authenticated user's ID from the access token.
     * @return User ID or null if not authenticated
     */
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
                if (DEBUG_LOG) Log.w(TAG, "Failed to get user ID: code=$code resp=$resp")
                null
            }
        } catch (t: Throwable) {
            if (DEBUG_LOG) Log.e(TAG, "Error getting user ID", t)
            null
        }
    }
}

