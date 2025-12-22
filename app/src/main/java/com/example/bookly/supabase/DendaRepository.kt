package com.example.bookly.supabase

class DendaRepository {

    private val paymentRepository = PaymentRepository()

    suspend fun uploadBuktiPembayaran(
        dendaId: String,
        imageBytes: ByteArray
    ): String {

        val fileName = "denda_${dendaId}_${System.currentTimeMillis()}.jpg"

        return paymentRepository.uploadBukti(fileName, imageBytes)
        // NEXT: simpan ke tabel pembayaran_denda jika perlu
    }
}
