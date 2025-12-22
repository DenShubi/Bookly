package com.example.bookly.supabase

import io.github.jan.supabase.storage.storage


class PaymentRepository {

    private val client = SupabaseClientProvider.client

    suspend fun uploadBukti(fileName: String, bytes: ByteArray): String {
        val bucket = client.storage.from("payment-proof")

        bucket.upload(
            path = fileName,
            data = bytes
        ) {
            upsert = true
        }

        return bucket.publicUrl(fileName)
    }
}
