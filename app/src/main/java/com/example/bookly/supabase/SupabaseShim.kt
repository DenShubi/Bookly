package com.example.bookly.supabase

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object supabase {
    val auth = Auth

    object Auth {
        class SignInBuilder { var email: String = ""; var password: String = "" }
        class SignUpBuilder { var email: String = ""; var password: String = ""; var data: Map<String, String>? = null }
        data class SupabaseUser(val id: String, val email: String?, val userMetadata: Map<String, String>)
        data class AuthResult(val user: SupabaseUser?)

        suspend fun signInWith(block: SignInBuilder.() -> Unit): AuthResult = withContext(Dispatchers.IO) {
            val b = SignInBuilder().apply(block)
            val res = UserRepository.login(b.email, b.password)
            if (res.isFailure) return@withContext AuthResult(null)
            val profile = UserRepository.getUserProfile().getOrNull()
            AuthResult(SupabaseUser("", profile?.email, mapOf("full_name" to (profile?.fullName ?: ""))))
        }

        suspend fun signUpWith(block: SignUpBuilder.() -> Unit): AuthResult = withContext(Dispatchers.IO) {
            val b = SignUpBuilder().apply(block)
            val res = UserRepository.register(b.data?.get("full_name") ?: "", b.email, b.password)
            if (res.isFailure) return@withContext AuthResult(null)
            val profile = UserRepository.getUserProfile().getOrNull()
            AuthResult(SupabaseUser("", profile?.email, mapOf("full_name" to (profile?.fullName ?: ""))))
        }

        fun currentUserOrNull(): SupabaseUser? {
            val profile = UserRepository.getUserProfileBlocking()
            return profile?.let { SupabaseUser("", it.email, mapOf("full_name" to (it.fullName ?: ""))) }
        }

        suspend fun signOut() { UserRepository.signOut() }
    }
}