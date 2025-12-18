package com.example.bookly.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookly.data.dto.BookRequestDto
import com.example.bookly.supabase.RequestBookRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

// Define State di file yang sama agar lebih ringkas (atau dipisah boleh)
data class RequestBookState(
    val title: String = "",
    val author: String = "",
    val year: String = "",
    val reason: String = "",
    val coverUri: Uri? = null,
    val supportingFileUri: Uri? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false
)

class RequestBookViewModel(
    private val repository: RequestBookRepository
) : ViewModel() {

    // StateFlow untuk menyimpan data UI
    private val _state = MutableStateFlow(RequestBookState())
    val state = _state.asStateFlow()

    // Fungsi untuk update input dari UI
    fun onTitleChange(value: String) = _state.update { it.copy(title = value) }
    fun onAuthorChange(value: String) = _state.update { it.copy(author = value) }
    fun onYearChange(value: String) = _state.update { it.copy(year = value) }
    fun onReasonChange(value: String) = _state.update { it.copy(reason = value) }

    // Fungsi untuk menyimpan URI gambar yang dipilih user
    fun onCoverSelected(uri: Uri?) = _state.update { it.copy(coverUri = uri) }
    fun onFileSelected(uri: Uri?) = _state.update { it.copy(supportingFileUri = uri) }

    // Fungsi Utama: Submit Data
    fun submitRequest(context: Context) {
        val currentState = _state.value

        // 1. Validasi Input
        if (currentState.title.isBlank() || currentState.author.isBlank() || currentState.reason.isBlank()) {
            _state.update { it.copy(errorMessage = "Judul, Penulis, dan Alasan wajib diisi.") }
            return
        }
        if (currentState.coverUri == null) {
            _state.update { it.copy(errorMessage = "Cover buku wajib diupload.") }
            return
        }

        // 2. Mulai Proses Upload (Background Thread)
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                // Ambil User ID
                val userId = repository.getCurrentUserId() ?: throw Exception("User belum login")

                // Proses Cover (Wajib)
                val coverBytes = context.contentResolver.openInputStream(currentState.coverUri!!)?.use {
                    it.readBytes()
                } ?: throw Exception("Gagal membaca file cover")

                val coverName = "$userId/${UUID.randomUUID()}.jpg"
                val coverUrl = repository.uploadFile("request-book-covers", coverName, coverBytes)

                // Proses File Pendukung (Opsional)
                var supportUrl: String? = null
                if (currentState.supportingFileUri != null) {
                    val supportBytes = context.contentResolver.openInputStream(currentState.supportingFileUri!!)?.use {
                        it.readBytes()
                    }
                    if (supportBytes != null) {
                        val supportName = "$userId/${UUID.randomUUID()}_doc"
                        supportUrl = repository.uploadFile("request-book-files", supportName, supportBytes)
                    }
                }

                // Simpan Data ke Database
                val requestDto = BookRequestDto(
                    user_id = userId,
                    title = currentState.title,
                    author = currentState.author,
                    year = currentState.year.toIntOrNull() ?: 0,
                    reason = currentState.reason,
                    cover_url = coverUrl,
                    supporting_file_url = supportUrl
                )

                repository.submitRequest(requestDto)

                // Sukses!
                _state.update { it.copy(isLoading = false, isSuccess = true) }

            } catch (e: Exception) {
                // Gagal :(
                e.printStackTrace()
                _state.update { it.copy(isLoading = false, errorMessage = "Gagal: ${e.message}") }
            }
        }
    }
}