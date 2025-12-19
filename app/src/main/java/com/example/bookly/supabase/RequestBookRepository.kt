package com.example.bookly.supabase

import com.example.bookly.data.dto.BookRequestDto
import io.github.jan.supabase.SupabaseClient
// Import wajib (Jangan dihapus)
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RequestBookRepository(private val supabase: SupabaseClient) {

    suspend fun uploadFile(
        bucketName: String,
        fileName: String,
        data: ByteArray
    ): String {
        return withContext(Dispatchers.IO) {
            val bucket = supabase.storage.from(bucketName)

            // --- PERBAIKAN DI SINI ---
            // Gunakan kurung kurawal untuk opsi 'upsert'
            bucket.upload(fileName, data) {
                upsert = true
            }
            // -------------------------

            bucket.publicUrl(fileName)
        }
    }

    suspend fun submitRequest(request: BookRequestDto) {
        withContext(Dispatchers.IO) {
            supabase.postgrest.from("book_requests").insert(request)
        }
    }

    fun getCurrentUserId(): String? {
        return supabase.auth.currentUserOrNull()?.id
    }
}