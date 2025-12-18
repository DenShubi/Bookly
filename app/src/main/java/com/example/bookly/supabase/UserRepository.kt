package com.example.bookly.supabase

import android.util.Log
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

data class UserProfile(val fullName: String?, val email: String?, val avatarUrl: String?)

@Serializable
data class UserData(
    @SerialName("full_name") val fullName: String? = null,
    val email: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null
)

@Serializable
data class UserAvatarUpdate(
    @SerialName("avatar_url") val avatarUrl: String
)

object UserRepository {

    private const val DEBUG = true
    private const val TAG = "UserRepo"

    // ... [register and login functions remain unchanged] ...

    suspend fun register(fullName: String, email: String, password: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val client = SupabaseClientProvider.client
                client.auth.signUpWith(Email) {
                    this.email = email
                    this.password = password
                    data = buildJsonObject { put("full_name", fullName) }
                }
                val session = client.auth.currentSessionOrNull()
                if (session != null) SupabaseClientProvider.currentAccessToken = session.accessToken
                Result.success(Unit)
            } catch (t: Throwable) {
                Result.failure(t)
            }
        }

    suspend fun login(email: String, password: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val client = SupabaseClientProvider.client
                client.auth.signInWith(Email) {
                    this.email = email
                    this.password = password
                }
                val session = client.auth.currentSessionOrNull()
                if (session != null) SupabaseClientProvider.currentAccessToken = session.accessToken
                Result.success(Unit)
            } catch (t: Throwable) {
                Result.failure(t)
            }
        }

    suspend fun getUserProfile(): Result<UserProfile> = withContext(Dispatchers.IO) {
        try {
            val client = SupabaseClientProvider.client
            val currentUser = client.auth.currentUserOrNull()
                ?: return@withContext Result.failure(Exception("Not authenticated"))

            val userId = currentUser.id
            val email = currentUser.email
            var fullName: String? = null
            var avatarUrl: String? = null

            try {
                val userData = client.from("users").select(columns = Columns.list("full_name", "email", "avatar_url")) {
                    filter { eq("id", userId) }
                }.decodeSingleOrNull<UserData>()

                fullName = userData?.fullName
                avatarUrl = userData?.avatarUrl
            } catch (e: Exception) {
                if (DEBUG) Log.w(TAG, "Failed to query users table: ${e.message}")
            }

            if (fullName.isNullOrBlank()) {
                val metadata = currentUser.userMetadata
                fullName = when (val nameValue = metadata?.get("full_name")) {
                    is String -> nameValue
                    else -> nameValue?.toString()?.removeSurrounding("\"")
                }
            }

            Result.success(UserProfile(fullName, email, avatarUrl))
        } catch (t: Throwable) {
            if (DEBUG) Log.e(TAG, "Error getting user profile", t)
            Result.failure(t)
        }
    }

    suspend fun changePassword(newPassword: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val client = SupabaseClientProvider.client
                client.auth.currentUserOrNull() ?: return@withContext Result.failure(Exception("User not authenticated"))
                client.auth.updateUser { password = newPassword }
                Result.success(Unit)
            } catch (t: Throwable) {
                Result.failure(t)
            }
        }

    // -----------------------------------------
    // UPLOAD PROFILE PICTURE (With Delete Old Logic)
    // -----------------------------------------
    suspend fun uploadProfilePicture(byteArray: ByteArray): Result<String> = withContext(Dispatchers.IO) {
        try {
            val client = SupabaseClientProvider.client
            val currentUser = client.auth.currentUserOrNull()
                ?: return@withContext Result.failure(Exception("Not authenticated"))

            val userId = currentUser.id
            val bucket = client.storage.from("profile-pictures")

            // 1. Get current avatar URL to delete later
            var oldFileName: String? = null
            try {
                val currentProfile = getUserProfile().getOrNull()
                val currentUrl = currentProfile?.avatarUrl
                if (!currentUrl.isNullOrEmpty()) {
                    // Extract filename from URL (e.g., .../profile-pictures/userId_123.jpg -> userId_123.jpg)
                    oldFileName = currentUrl.substringAfterLast("/")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not fetch old avatar for deletion", e)
            }

            // 2. Upload new image
            // Generate a unique filename: userId_timestamp.jpg
            val newFileName = "${userId}_${System.currentTimeMillis()}.jpg"

            bucket.upload(newFileName, byteArray) {
                upsert = true
            }

            val publicUrl = bucket.publicUrl(newFileName)

            // 3. Update DB
            updateUserAvatar(publicUrl)

            // 4. Delete old image if exists and different
            if (oldFileName != null && oldFileName != newFileName) {
                try {
                    bucket.delete(listOf(oldFileName))
                    if (DEBUG) Log.d(TAG, "Deleted old avatar: $oldFileName")
                } catch (e: Exception) {
                    // Log error but don't fail the upload result, as the new image is already live
                    Log.e(TAG, "Failed to delete old avatar: $oldFileName", e)
                }
            }

            Result.success(publicUrl)
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading profile picture", e)
            Result.failure(e)
        }
    }

    private suspend fun updateUserAvatar(url: String) {
        val client = SupabaseClientProvider.client
        val currentUser = client.auth.currentUserOrNull() ?: throw Exception("Not authenticated")

        client.from("users").update(UserAvatarUpdate(avatarUrl = url)) {
            filter { eq("id", currentUser.id) }
        }
    }

    suspend fun signOut(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val client = SupabaseClientProvider.client
            client.auth.signOut()
            SupabaseClientProvider.currentAccessToken = null
            Result.success(Unit)
        } catch (t: Throwable) {
            SupabaseClientProvider.currentAccessToken = null
            Result.failure(t)
        }
    }

    fun getUserProfileBlocking(): UserProfile? = try {
        runBlocking { getUserProfile().getOrNull() }
    } catch (_: Throwable) {
        null
    }
}