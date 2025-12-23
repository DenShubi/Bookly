package com.example.bookly.supabase

import android.util.Log
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

object UsersRepository {
    private const val TAG = "UsersRepository"

    @Serializable
    data class UserRow(
        val id: String = "",
        val name: String? = null,
        val email: String? = null,
        @SerialName("phone_number") val phoneNumber: String? = null,
        val role: String? = "user",
        @SerialName("created_at") val createdAt: String? = null
    )

    /**
     * Get user by ID
     */
    suspend fun getUserById(userId: String): Result<UserRow> = withContext(Dispatchers.IO) {
        try {
            val client = SupabaseClientProvider.client
            val user = client.from("users").select {
                filter { eq("id", userId) }
            }.decodeSingleOrNull<UserRow>()
                ?: return@withContext Result.failure(Exception("User not found"))

            Result.success(user)
        } catch (t: Throwable) {
            Log.e(TAG, "Error fetching user by ID", t)
            Result.failure(t)
        }
    }

    /**
     * Get current user
     */
    suspend fun getCurrentUser(): Result<UserRow> = withContext(Dispatchers.IO) {
        try {
            val userId = getCurrentUserId()
                ?: return@withContext Result.failure(Exception("User not authenticated"))
            getUserById(userId)
        } catch (t: Throwable) {
            Log.e(TAG, "Error fetching current user", t)
            Result.failure(t)
        }
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

