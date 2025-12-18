package com.example.bookly.supabase

import com.example.bookly.data.dto.BookRequestDto
import io.github.jan.supabase.SupabaseClient
// --- IMPORT PENTING (JANGAN DIHAPUS) ---
import io.github.jan.supabase.auth.auth             // Agar supabase.auth dikenali
import io.github.jan.supabase.postgrest.postgrest   // Agar supabase.postgrest dikenali
import io.github.jan.supabase.storage.storage       // Agar supabase.storage dikenali
// ---------------------------------------
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
            // Upload file (overwrite jika ada nama sama)
            bucket.upload(fileName, data, upsert = true)
            // Kembalikan URL publik
            bucket.publicUrl(fileName)
        }
    }

    suspend fun submitRequest(request: BookRequestDto) {
        withContext(Dispatchers.IO) {
            // Gunakan .from() untuk memilih tabel
            supabase.postgrest.from("book_requests").insert(request)
        }
    }

    fun getCurrentUserId(): String? {
        // Mengambil ID user yang sedang login
        return supabase.auth.currentUserOrNull()?.id
    }
}