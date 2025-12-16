package com.example.bookly.supabase

import android.util.Log
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

data class UserProfile(val fullName: String?, val email: String?)

@Serializable
data class UserData(
    @SerialName("full_name") val fullName: String? = null,
    val email: String? = null
)

object UserRepository {

    private const val DEBUG = true

    // -----------------------------------------
    // REGISTER NEW USER
    // -----------------------------------------
    suspend fun register(fullName: String, email: String, password: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val client = SupabaseClientProvider.client

                // Sign up with metadata
                client.auth.signUpWith(Email) {
                    this.email = email
                    this.password = password
                    data = buildJsonObject {
                        put("full_name", fullName)
                    }
                }

                // Get session and store token for backward compatibility
                val session = client.auth.currentSessionOrNull()
                if (session != null) {
                    SupabaseClientProvider.currentAccessToken = session.accessToken
                    if (DEBUG) Log.d("UserRepo", "Signup success! Token stored.")
                }

                Result.success(Unit)
            } catch (t: Throwable) {
                if (DEBUG) Log.e("UserRepo", "Signup error: ${t.message}", t)
                Result.failure(t)
            }
        }



    // -----------------------------------------
    // LOGIN
    // -----------------------------------------
    suspend fun login(email: String, password: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val client = SupabaseClientProvider.client

                client.auth.signInWith(Email) {
                    this.email = email
                    this.password = password
                }

                // Store token for backward compatibility
                val session = client.auth.currentSessionOrNull()
                if (session != null) {
                    SupabaseClientProvider.currentAccessToken = session.accessToken
                    if (DEBUG) Log.d("UserRepo", "Login success! Token stored.")
                }

                Result.success(Unit)
            } catch (t: Throwable) {
                if (DEBUG) Log.e("UserRepo", "Login error: ${t.message}", t)
                Result.failure(t)
            }
        }



    // -----------------------------------------
    // GET USER PROFILE (AUTH + public.users)
    // -----------------------------------------
    suspend fun getUserProfile(): Result<UserProfile> = withContext(Dispatchers.IO) {
        try {
            val client = SupabaseClientProvider.client
            val currentUser = client.auth.currentUserOrNull()
                ?: return@withContext Result.failure(Exception("Not authenticated"))

            val userId = currentUser.id
            val email = currentUser.email
            var fullName: String? = null

            // Query public.users table
            try {
                val userData = client.from("users").select(columns = Columns.list("full_name", "email")) {
                    filter { eq("id", userId) }
                }.decodeSingleOrNull<UserData>()

                fullName = userData?.fullName

                if (DEBUG) Log.d("UserRepo", "Users table query: fullName=$fullName")
            } catch (e: Exception) {
                if (DEBUG) Log.w("UserRepo", "Failed to query users table: ${e.message}")
            }

            // Fallback: read from auth metadata
            if (fullName.isNullOrBlank()) {
                val metadata = currentUser.userMetadata
                fullName = when (val nameValue = metadata?.get("full_name")) {
                    is String -> nameValue
                    else -> nameValue?.toString()?.removeSurrounding("\"")
                }
            }

            Result.success(UserProfile(fullName, email))
        } catch (t: Throwable) {
            if (DEBUG) Log.e("UserRepo", "Error getting user profile", t)
            Result.failure(t)
        }
    }


    // -----------------------------------------
    // CHANGE PASSWORD
    // -----------------------------------------
    suspend fun changePassword(newPassword: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val client = SupabaseClientProvider.client

                // Ensure user is logged in
                client.auth.currentUserOrNull()
                    ?: return@withContext Result.failure(Exception("User not authenticated"))

                // Update password using SDK
                client.auth.updateUser {
                    password = newPassword
                }

                if (DEBUG) Log.d("UserRepo", "Change password success")

                Result.success(Unit)
            } catch (t: Throwable) {
                if (DEBUG) Log.e("UserRepo", "Error changing password", t)
                Result.failure(t)
            }
        }


    // -----------------------------------------
    // LOGOUT
    // -----------------------------------------
    suspend fun signOut(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val client = SupabaseClientProvider.client

            client.auth.signOut()

            // Clear manual token for backward compatibility
            SupabaseClientProvider.currentAccessToken = null

            if (DEBUG) Log.d("UserRepo", "Sign out success")

            Result.success(Unit)
        } catch (t: Throwable) {
            if (DEBUG) Log.e("UserRepo", "Error signing out", t)
            // Even on error, clear local state
            SupabaseClientProvider.currentAccessToken = null
            Result.failure(t)
        }
    }




    // Synchronous helper for preview / ViewModel unsafe calls
    fun getUserProfileBlocking(): UserProfile? = try {
        runBlocking { getUserProfile().getOrNull() }
    } catch (_: Throwable) {
        null
    }
}
