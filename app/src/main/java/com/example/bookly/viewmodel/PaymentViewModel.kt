package com.example.bookly.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookly.supabase.DendaRepository
import kotlinx.coroutines.launch

class PaymentViewModel : ViewModel() {

    private val repository = DendaRepository()

    fun uploadBuktiPembayaran(
        dendaId: String,
        imageBytes: ByteArray,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                repository.uploadBuktiPembayaran(dendaId, imageBytes)
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Upload gagal")
            }
        }
    }
}
