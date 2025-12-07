package com.example.bookly.supabase

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Minimal shim to provide the requested `supabase.auth` API used by UI code.
object supabase {
    val auth = Auth

    object Auth {
        class SignInBuilder {
            var email: String = ""
            var password: String = ""
        }

        class SignUpBuilder {
            var email: String = ""
            var password: String = ""
            var data: Map<String, String>? = null
        }

        data class SupabaseUser(val id: String, val email: String?, val userMetadata: Map<String, String>)
        data class AuthResult(val user: SupabaseUser?)

        suspend fun signInWith(block: SignInBuilder.() -> Unit): AuthResult = withContext(Dispatchers.IO) {
            val b = SignInBuilder().apply(block)
            val res = UserRepository.login(email = b.email, password = b.password)
            if (res.isFailure) return@withContext AuthResult(null)

            // Fetch profile for username metadata
            val profile = UserRepository.getUserProfile()
            val username = profile.getOrNull()?.username
            val user = SupabaseUser(id = "", email = profile.getOrNull()?.email, userMetadata = mapOf("username" to (username ?: "")))
            AuthResult(user = user)
        }

        suspend fun signUpWith(block: SignUpBuilder.() -> Unit): AuthResult = withContext(Dispatchers.IO) {
            val b = SignUpBuilder().apply(block)
            val username = b.data?.get("username")
            val res = UserRepository.register(username = username ?: "", email = b.email, password = b.password)
            if (res.isFailure) return@withContext AuthResult(null)

            // After register, fetch profile
            val profile = UserRepository.getUserProfile()
            val user = SupabaseUser(id = "", email = profile.getOrNull()?.email, userMetadata = mapOf("username" to (profile.getOrNull()?.username ?: "")))
            AuthResult(user = user)
        }

        fun currentUserOrNull(): SupabaseUser? {
            // Use stored token to fetch profile synchronously is not ideal; return simple mapping from provider
            val profileRes = try { UserRepository.getUserProfileBlocking() } catch (t: Throwable) { null }
            return profileRes?.let { SupabaseUser(id = "", email = it.email, userMetadata = mapOf("username" to (it.username ?: ""))) }
        }
    }
}
