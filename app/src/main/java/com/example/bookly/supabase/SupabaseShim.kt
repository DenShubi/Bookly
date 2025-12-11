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

            // Fetch profile for full_name metadata
            val profile = UserRepository.getUserProfile()
            val fullName = profile.getOrNull()?.fullName
            val user = SupabaseUser(id = "", email = profile.getOrNull()?.email, userMetadata = mapOf("full_name" to (fullName ?: "")))
            AuthResult(user = user)
        }

        suspend fun signUpWith(block: SignUpBuilder.() -> Unit): AuthResult = withContext(Dispatchers.IO) {
            val b = SignUpBuilder().apply(block)
            val fullName = b.data?.get("full_name")
            val res = UserRepository.register(fullName = fullName ?: "", email = b.email, password = b.password)
            if (res.isFailure) return@withContext AuthResult(null)

            // After register, fetch profile
            val profile = UserRepository.getUserProfile()
            val user = SupabaseUser(id = "", email = profile.getOrNull()?.email, userMetadata = mapOf("full_name" to (profile.getOrNull()?.fullName ?: "")))
            AuthResult(user = user)
        }

        fun currentUserOrNull(): SupabaseUser? {
            // Use stored token to fetch profile synchronously is not ideal; return simple mapping from provider
            val profileRes = try { UserRepository.getUserProfileBlocking() } catch (t: Throwable) { null }
            return profileRes?.let { SupabaseUser(id = "", email = it.email, userMetadata = mapOf("full_name" to (it.fullName ?: ""))) }
        }
    }
}
