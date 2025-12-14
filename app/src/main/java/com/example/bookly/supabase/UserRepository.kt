package com.example.bookly.supabase

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

data class UserProfile(val fullName: String?, val email: String?)

object UserRepository {

    private const val DEBUG = true

    // -----------------------------------------
    // REGISTER NEW USER
    // -----------------------------------------
    suspend fun register(fullName: String, email: String, password: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                // Use Supabase Kotlin SDK inside your app, not direct REST.
                // Direct REST signup is fine but heavy; still keeping your logic intact.
                val signupUrl = "${SupabaseClientProvider.SUPABASE_URL}/auth/v1/signup"
                val payload = JSONObject().apply {
                    put("email", email)
                    put("password", password)
                    put("data", JSONObject().put("full_name", fullName))
                }

                val conn = (URL(signupUrl).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("apikey", SupabaseClientProvider.SUPABASE_ANON_KEY)
                }

                val body = payload.toString().toByteArray()
                conn.setFixedLengthStreamingMode(body.size)
                conn.outputStream.use { it.write(body) }

                val code = conn.responseCode
                val resp = conn.inputStream.bufferedReader().readText()
                if (DEBUG) Log.d("UserRepo", "Signup response $code: $resp")

                if (code !in 200..299) return@withContext Result.failure(Exception(resp))

                // Extract access_token so user is immediately authenticated
                val json = JSONObject(resp)
                SupabaseClientProvider.currentAccessToken = json.optString("access_token", null)

                Result.success(Unit)
            } catch (t: Throwable) {
                Result.failure(t)
            }
        }



    // -----------------------------------------
    // LOGIN
    // -----------------------------------------
    suspend fun login(email: String, password: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val loginUrl = "${SupabaseClientProvider.SUPABASE_URL}/auth/v1/token?grant_type=password"

                val payload = JSONObject().apply {
                    put("email", email)
                    put("password", password)
                }

                val conn = (URL(loginUrl).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("apikey", SupabaseClientProvider.SUPABASE_ANON_KEY)
                }

                val body = payload.toString().toByteArray()
                conn.setFixedLengthStreamingMode(body.size)
                conn.outputStream.use { it.write(body) }

                val code = conn.responseCode
                val resp = conn.inputStream.bufferedReader().readText()
                if (DEBUG) Log.d("UserRepo", "Login response $code: $resp")

                if (code !in 200..299) return@withContext Result.failure(Exception(resp))

                val json = JSONObject(resp)
                SupabaseClientProvider.currentAccessToken = json.optString("access_token", null)

                Result.success(Unit)
            } catch (t: Throwable) {
                Result.failure(t)
            }
        }



    // -----------------------------------------
    // GET USER PROFILE (AUTH + public.users)
    // -----------------------------------------
    suspend fun getUserProfile(): Result<UserProfile> = withContext(Dispatchers.IO) {
        try {
            val token = SupabaseClientProvider.currentAccessToken
                ?: return@withContext Result.failure(Exception("Not authenticated"))

            // Fetch auth user info
            val authUser = fetchAuthUser(token)
                ?: return@withContext Result.failure(Exception("Failed fetching auth user"))

            val userId = authUser.optString("id", null)
            val email = authUser.optString("email", null)
            var fullName: String? = null

            // Query public.users with correct RLS token
            if (!userId.isNullOrBlank()) {
                val url =
                    "${SupabaseClientProvider.SUPABASE_URL}/rest/v1/users?select=full_name,email&id=eq.$userId"
                val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    setRequestProperty("apikey", SupabaseClientProvider.SUPABASE_ANON_KEY)
                    setRequestProperty("Authorization", "Bearer $token") // FIXED!!
                }

                val code = conn.responseCode
                val resp = conn.inputStream.bufferedReader().readText()

                if (DEBUG) Log.d("UserRepo", "Users table response: $resp")

                if (code in 200..299) {
                    val arr = JSONArray(resp)
                    if (arr.length() > 0)
                        fullName = arr.getJSONObject(0).optString("full_name", null)
                }
            }

            // Final fallback: read from auth metadata
            if (fullName.isNullOrBlank()) {
                fullName = authUser.optJSONObject("user_metadata")?.optString("full_name")
            }

            Result.success(UserProfile(fullName, email))
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }


    // -----------------------------------------
    // CHANGE PASSWORD
    // -----------------------------------------
    suspend fun changePassword(newPassword: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val token = SupabaseClientProvider.currentAccessToken
                    ?: return@withContext Result.failure(Exception("User not authenticated"))

                val url = "${SupabaseClientProvider.SUPABASE_URL}/auth/v1/user"
                val payload = JSONObject().apply {
                    put("password", newPassword)
                }

                val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                    requestMethod = "PUT"
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Authorization", "Bearer $token")
                    setRequestProperty("apikey", SupabaseClientProvider.SUPABASE_ANON_KEY)
                }

                val body = payload.toString().toByteArray()
                conn.setFixedLengthStreamingMode(body.size)
                conn.outputStream.use { it.write(body) }

                val code = conn.responseCode
                val resp = if (code in 200..299) {
                    conn.inputStream?.bufferedReader()?.readText() ?: ""
                } else {
                    conn.errorStream?.bufferedReader()?.readText() ?: ""
                }

                if (DEBUG) Log.d("UserRepo", "Change password response $code: $resp")

                if (code !in 200..299) {
                    return@withContext Result.failure(Exception("Failed to change password: $resp"))
                }

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
            val token = SupabaseClientProvider.currentAccessToken
                ?: return@withContext Result.success(Unit)

            val url = "${SupabaseClientProvider.SUPABASE_URL}/auth/v1/logout"
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("apikey", SupabaseClientProvider.SUPABASE_ANON_KEY)
            }

            conn.responseCode // We don't care, logout is best-effort
            SupabaseClientProvider.currentAccessToken = null

            Result.success(Unit)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }


    // -----------------------------------------
    // FETCH AUTH USER (correct token)
    // -----------------------------------------
    private fun fetchAuthUser(token: String): JSONObject? {
        return try {
            val url = "${SupabaseClientProvider.SUPABASE_URL}/auth/v1/user"
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("apikey", SupabaseClientProvider.SUPABASE_ANON_KEY)
            }

            val code = conn.responseCode
            val resp = conn.inputStream.bufferedReader().readText()

            if (code in 200..299) JSONObject(resp) else null
        } catch (t: Throwable) {
            null
        }
    }


    // Synchronous helper for preview / ViewModel unsafe calls
    fun getUserProfileBlocking(): UserProfile? = try {
        runBlocking { getUserProfile().getOrNull() }
    } catch (t: Throwable) {
        null
    }
}
