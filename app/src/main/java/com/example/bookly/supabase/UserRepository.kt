package com.example.bookly.supabase

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.auth.auth
import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    val fullName: String?,
    val email: String?,
    val avatarUrl: String? = null
)

object UserRepository {
    private val supabase = SupabaseClientProvider.client

    // FIX: Upload Gambar Profile (Supaya ProfilePage.kt baris 60 tidak error)
    suspend fun uploadProfilePicture(uri: Uri, context: Context): Result<String> = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val bytes = inputStream?.readBytes() ?: throw Exception("Gagal baca gambar")
            inputStream.close()

            val user = supabase.auth.currentUserOrNull() ?: throw Exception("User tidak login")
            val fileName = "${user.id}/profile_${System.currentTimeMillis()}.jpg"
            val bucket = supabase.storage.from("profiles")

            bucket.upload(path = fileName, data = bytes) { upsert = true }
            val publicUrl = bucket.publicUrl(fileName)

            // Update ke tabel users
            supabase.from("users").update({ set("avatar_url", publicUrl) }) {
                filter { eq("id", user.id) }
            }
            Result.success(publicUrl)
        } catch (e: Exception) { Result.failure(e) }
    }

    // FIX: Get Profile (Hanya 1 fungsi - Menghilangkan Conflicting Overloads)
    suspend fun getUserProfile(): Result<UserProfile> = withContext(Dispatchers.IO) {
        try {
            val token = SupabaseClientProvider.currentAccessToken ?: return@withContext Result.failure(Exception("No Token"))
            val authUser = fetchAuthUser(token) ?: return@withContext Result.failure(Exception("Auth Failed"))

            val userId = authUser.optString("id", "")
            val email = authUser.optString("email", "")

            var fullName: String? = null
            var avatarUrl: String? = null

            if (userId.isNotEmpty()) {
                val url = "${SupabaseClientProvider.SUPABASE_URL}/rest/v1/users?select=full_name,email,avatar_url&id=eq.$userId"
                val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    SupabaseClientProvider.headersWithAuth().forEach { (k, v) -> setRequestProperty(k, v) }
                }

                if (conn.responseCode in 200..299) {
                    val arr = JSONArray(conn.inputStream.bufferedReader().readText())
                    if (arr.length() > 0) {
                        val obj = arr.getJSONObject(0)
                        fullName = if (obj.isNull("full_name")) null else obj.optString("full_name")
                        avatarUrl = if (obj.isNull("avatar_url")) null else obj.optString("avatar_url")
                    }
                }
            }
            Result.success(UserProfile(fullName, email, avatarUrl))
        } catch (t: Throwable) { Result.failure(t) }
    }

    // FIX: Fungsi Blocking untuk SupabaseShim (Fix image_46df9a)
    fun getUserProfileBlocking(): UserProfile? = runBlocking { getUserProfile().getOrNull() }

    // LOGIN
    suspend fun login(email: String, password: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val loginUrl = "${SupabaseClientProvider.SUPABASE_URL}/auth/v1/token?grant_type=password"
            val payload = JSONObject().apply { put("email", email); put("password", password) }
            val conn = (URL(loginUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("apikey", SupabaseClientProvider.SUPABASE_ANON_KEY)
            }
            conn.outputStream.use { it.write(payload.toString().toByteArray()) }
            if (conn.responseCode in 200..299) {
                val json = JSONObject(conn.inputStream.bufferedReader().readText())
                SupabaseClientProvider.currentAccessToken = json.optString("access_token", "")
                Result.success(Unit)
            } else Result.failure(Exception("Login Gagal"))
        } catch (t: Throwable) { Result.failure(t) }
    }

    // REGISTER
    suspend fun register(fullName: String, email: String, password: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseClientProvider.SUPABASE_URL}/auth/v1/signup"
            val payload = JSONObject().apply {
                put("email", email); put("password", password)
                put("data", JSONObject().put("full_name", fullName))
            }
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("apikey", SupabaseClientProvider.SUPABASE_ANON_KEY)
            }
            conn.outputStream.use { it.write(payload.toString().toByteArray()) }
            if (conn.responseCode in 200..299) Result.success(Unit) else Result.failure(Exception("Signup Gagal"))
        } catch (t: Throwable) { Result.failure(t) }
    }

    // CHANGE PASSWORD (Fix image_467a42)
    suspend fun changePassword(newPassword: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseClientProvider.SUPABASE_URL}/auth/v1/user"
            val payload = JSONObject().apply { put("password", newPassword) }
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "PUT"
                doOutput = true
                SupabaseClientProvider.headersWithAuth().forEach { (k, v) -> setRequestProperty(k, v) }
            }
            conn.outputStream.use { it.write(payload.toString().toByteArray()) }
            if (conn.responseCode in 200..299) Result.success(Unit) else Result.failure(Exception("Gagal Ubah Password"))
        } catch (t: Throwable) { Result.failure(t) }
    }

    suspend fun signOut(): Result<Unit> {
        SupabaseClientProvider.currentAccessToken = null
        supabase.auth.signOut()
        return Result.success(Unit)
    }

    private fun fetchAuthUser(token: String): JSONObject? {
        return try {
            val conn = (URL("${SupabaseClientProvider.SUPABASE_URL}/auth/v1/user").openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                SupabaseClientProvider.headersWithAuth().forEach { (k, v) -> setRequestProperty(k, v) }
            }
            if (conn.responseCode in 200..299) JSONObject(conn.inputStream.bufferedReader().readText()) else null
        } catch (t: Throwable) { null }
    }
}