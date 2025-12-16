package com.example.bookly.supabase

import android.util.Log
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Repository for managing wishlist operations with Supabase.
 * Handles CRUD operations for the wishlist table.
 */
object WishlistRepository {
    private const val DEBUG_LOG = true
    private const val TAG = "WishlistRepository"

    @Serializable
    data class WishlistRow(
        @SerialName("book_id") val bookId: String
    )

    @Serializable
    data class WishlistInsert(
        @SerialName("user_id") val userId: String,
        @SerialName("book_id") val bookId: String
    )

    /**
     * Fetches all wishlist items for the currently authenticated user.
     * @return Result containing list of book IDs in the user's wishlist
     */
    suspend fun getWishlist(): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val userId = getCurrentUserId()
                ?: return@withContext Result.failure(Exception("User not authenticated"))

            val client = SupabaseClientProvider.client
            val wishlistItems = client.from("wishlist").select {
                filter { eq("user_id", userId) }
            }.decodeList<WishlistRow>()

            val bookIds = wishlistItems.map { it.bookId }

            if (DEBUG_LOG) Log.d(TAG, "getWishlist success: ${bookIds.size} items")

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

            val client = SupabaseClientProvider.client
            val wishlistItem = WishlistInsert(userId = userId, bookId = bookId)

            client.from("wishlist").insert(wishlistItem) {
                // Handle duplicates gracefully - upsert will ignore if exists
                select()
            }

            if (DEBUG_LOG) Log.d(TAG, "addToWishlist success for bookId=$bookId")

            Result.success(Unit)
        } catch (t: Throwable) {
            if (DEBUG_LOG) Log.e(TAG, "Error adding to wishlist", t)
            // If error is duplicate key, treat as success
            if (t.message?.contains("duplicate key", ignoreCase = true) == true) {
                Result.success(Unit)
            } else {
                Result.failure(t)
            }
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

            val client = SupabaseClientProvider.client
            client.from("wishlist").delete {
                filter {
                    eq("user_id", userId)
                    eq("book_id", bookId)
                }
            }

            if (DEBUG_LOG) Log.d(TAG, "removeFromWishlist success for bookId=$bookId")

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
            val client = SupabaseClientProvider.client
            client.auth.currentUserOrNull()?.id
        } catch (t: Throwable) {
            if (DEBUG_LOG) Log.e(TAG, "Error getting user ID", t)
            null
        }
    }
}

