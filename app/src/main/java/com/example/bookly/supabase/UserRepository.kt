package com.example.bookly.supabase

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL

data class UserProfile(val username: String?, val email: String?)

object UserRepository {

    private const val DEBUG_LOG = true


    suspend fun register(username: String, email: String, password: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                // Sign up user
                val signupUrl = "${SupabaseClientProvider.SUPABASE_URL}/auth/v1/signup"
                val signupPayload = JSONObject()
                signupPayload.put("email", email)
                signupPayload.put("password", password)

                val signupConn = (URL(signupUrl).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("apikey", SupabaseClientProvider.SUPABASE_ANON_KEY)
                    setRequestProperty("Authorization", "Bearer ${SupabaseClientProvider.SUPABASE_ANON_KEY}")
                }

                // Write JSON body with explicit content length to avoid truncated/empty request bodies
                val signupBody = signupPayload.toString()
                val signupBytes = signupBody.toByteArray(Charsets.UTF_8)
                signupConn.setFixedLengthStreamingMode(signupBytes.size)
                if (DEBUG_LOG) {
                    Log.d("UserRepository", "POST $signupUrl body=$signupBody headers=[Content-Type: application/json]")
                }
                signupConn.outputStream.use { os ->
                    os.write(signupBytes)
                    os.flush()
                }

                val signupRespCode = signupConn.responseCode
                val signupResp = BufferedReader(InputStreamReader(
                    if (signupRespCode in 200..299) signupConn.inputStream else signupConn.errorStream
                )).use { it.readText() }

                if (DEBUG_LOG) Log.d("UserRepository", "signup response code=$signupRespCode body=$signupResp")
                // Defensive parse: some Supabase setups may return empty or non-JSON bodies on signup
                val signupJson = try {
                    if (signupResp.isNullOrBlank()) JSONObject()
                    else JSONObject(signupResp)
                } catch (e: Throwable) {
                    if (DEBUG_LOG) Log.d("UserRepository", "Failed parsing signup response as JSON: ${e.message}")
                    JSONObject()
                }
                // Extract access token if present
                val accessToken = signupJson.optString("access_token", null)
                SupabaseClientProvider.currentAccessToken = accessToken

                // Ensure we have user id (try to fetch via /auth/v1/user)
                var userId: String? = signupJson.optJSONObject("user")?.optString("id")
                if (userId.isNullOrBlank() && !accessToken.isNullOrBlank()) {
                    val userInfo = fetchAuthUser(accessToken)
                    userId = userInfo?.optString("id")
                }

                // NOTE: removed direct insert into `users` table. Use Supabase auth trigger to populate users table.

                // If signup didn't return access token, try login to get session
                if (SupabaseClientProvider.currentAccessToken.isNullOrBlank()) {
                    val loginRes = login(email, password)
                    if (loginRes.isFailure) return@withContext Result.failure(loginRes.exceptionOrNull()!!)
                }

                Result.success(Unit)
            } catch (t: Throwable) {
                Result.failure(t)
            }
        }

    suspend fun login(email: String, password: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val loginUrl = "${SupabaseClientProvider.SUPABASE_URL}/auth/v1/token"

            // Supabase token endpoint expects 'email' parameter for password grant
            val form = "grant_type=password&email=${URLEncoder.encode(email, "UTF-8")}&password=${URLEncoder.encode(password, "UTF-8")}" 
            val conn = (URL(loginUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("apikey", SupabaseClientProvider.SUPABASE_ANON_KEY)
                setRequestProperty("Authorization", "Bearer ${SupabaseClientProvider.SUPABASE_ANON_KEY}")
            }

            // Ensure correct Content-Length for form body
            val formBody = form
            val formBytes = formBody.toByteArray(Charsets.UTF_8)
            conn.setFixedLengthStreamingMode(formBytes.size)
            if (DEBUG_LOG) Log.d("UserRepository", "POST $loginUrl body=$formBody headers=[Content-Type: application/x-www-form-urlencoded]")
            conn.outputStream.use { os ->
                os.write(formBytes)
                os.flush()
            }

            val code = conn.responseCode
            val resp = BufferedReader(InputStreamReader(
                if (code in 200..299) conn.inputStream else conn.errorStream
            )).use { it.readText() }
            if (DEBUG_LOG) Log.d("UserRepository", "login response code=$code body=$resp")
            if (code !in 200..299) return@withContext Result.failure(Exception(resp))

            val json = JSONObject(resp)
            val accessToken = json.optString("access_token", null)
            SupabaseClientProvider.currentAccessToken = accessToken

            Result.success(Unit)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    suspend fun getUserProfile(): Result<UserProfile> = withContext(Dispatchers.IO) {
        try {
            val token = SupabaseClientProvider.currentAccessToken
                ?: return@withContext Result.failure(Exception("Not authenticated"))

            // Get auth user to obtain email
            val userInfo = fetchAuthUser(token) ?: return@withContext Result.failure(Exception("Failed fetching auth user"))
            val email = userInfo.optString("email", null)
            val userId = userInfo.optString("id", null)

            var username: String? = null

            // Try fetching from users table by id
            if (!userId.isNullOrBlank()) {
                val url = "${SupabaseClientProvider.SUPABASE_URL}/rest/v1/users?select=username,email&id=eq.$userId"
                val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    setRequestProperty("apikey", SupabaseClientProvider.SUPABASE_ANON_KEY)
                    setRequestProperty("Authorization", "Bearer ${SupabaseClientProvider.SUPABASE_ANON_KEY}")
                }
                val code = conn.responseCode
                val resp = BufferedReader(InputStreamReader(
                    if (code in 200..299) conn.inputStream else conn.errorStream
                )).use { it.readText() }

                if (code in 200..299) {
                    val arr = JSONArray(resp)
                    if (arr.length() > 0) {
                        val obj = arr.getJSONObject(0)
                        username = obj.optString("username", null)
                    }
                }
            }

            // Fallback: try lookup by email
            if (username == null && !email.isNullOrBlank()) {
                val url = "${SupabaseClientProvider.SUPABASE_URL}/rest/v1/users?select=username,email&email=eq.${URLEncoder.encode(email, "UTF-8") }"
                val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    setRequestProperty("apikey", SupabaseClientProvider.SUPABASE_ANON_KEY)
                    setRequestProperty("Authorization", "Bearer ${SupabaseClientProvider.SUPABASE_ANON_KEY}")
                }
                val code = conn.responseCode
                val resp = BufferedReader(InputStreamReader(
                    if (code in 200..299) conn.inputStream else conn.errorStream
                )).use { it.readText() }

                if (code in 200..299) {
                    val arr = JSONArray(resp)
                    if (arr.length() > 0) {
                        val obj = arr.getJSONObject(0)
                        username = obj.optString("username", null)
                    }
                }
            }

            Result.success(UserProfile(username = username, email = email))
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    suspend fun signOut(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val token = SupabaseClientProvider.currentAccessToken ?: return@withContext Result.success(Unit)
            val url = "${SupabaseClientProvider.SUPABASE_URL}/auth/v1/logout"
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("apikey", SupabaseClientProvider.SUPABASE_ANON_KEY)
                setRequestProperty("Authorization", "Bearer $token")
            }
            val code = conn.responseCode
            // Clear token locally regardless of server response
            SupabaseClientProvider.currentAccessToken = null
            if (code in 200..299) return@withContext Result.success(Unit)
            val resp = BufferedReader(InputStreamReader(conn.errorStream)).use { it.readText() }
            Result.failure(Exception(resp))
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    private fun fetchAuthUser(token: String): JSONObject? {
        return try {
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
            if (code in 200..299) JSONObject(resp) else null
        } catch (t: Throwable) {
            null
        }
    }

    // Blocking helper for simple synchronous access from shim/UI when needed.
    fun getUserProfileBlocking(): UserProfile? {
        return try {
            runBlocking { getUserProfile().getOrNull() }
        } catch (t: Throwable) {
            null
        }
    }
}
