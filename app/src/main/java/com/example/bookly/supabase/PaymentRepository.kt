package com.example.bookly.supabase

import io.github.jan.supabase.storage.upload
import com.example.bookly.supabase.SupabaseClientProvider
import io.github.jan.supabase.storage.upload


class PaymentRepository {

    private val client = SupabaseClientProvider.client

    suspend fun uploadBukti(fileName: String, bytes: ByteArray): String {
        val bucket = client.storage.from("payment-proof")

        bucket.upload(
            path = fileName,
            data = bytes,
            upsert = true
        )

        return bucket.publicUrl(fileName)
    }
}
