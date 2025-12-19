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


class RequestBookViewModel(
    private val repository: RequestBookRepository
) : ViewModel() {

    private val _state = MutableStateFlow(RequestBookState())
    val state = _state.asStateFlow()

    fun onTitleChange(value: String) = _state.update { it.copy(title = value) }
    fun onAuthorChange(value: String) = _state.update { it.copy(author = value) }
    fun onYearChange(value: String) = _state.update { it.copy(year = value) }
    fun onReasonChange(value: String) = _state.update { it.copy(reason = value) }
    fun onCoverSelected(uri: Uri?) = _state.update { it.copy(coverUri = uri) }
    fun onFileSelected(uri: Uri?) = _state.update { it.copy(supportingFileUri = uri) }

    fun submitRequest(context: Context) {
        val currentState = _state.value

        // Validasi
        if (currentState.title.isBlank() || currentState.author.isBlank() || currentState.reason.isBlank()) {
            _state.update { it.copy(errorMessage = "Mohon lengkapi semua data wajib.") }
            return
        }
        if (currentState.coverUri == null) {
            _state.update { it.copy(errorMessage = "Cover buku wajib diupload.") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val userId = repository.getCurrentUserId() ?: throw Exception("User belum login")

                // Upload Cover
                val coverBytes = context.contentResolver.openInputStream(currentState.coverUri!!)?.use { it.readBytes() }
                    ?: throw Exception("Gagal membaca gambar cover")
                val coverName = "$userId/${UUID.randomUUID()}.jpg"
                val coverUrl = repository.uploadFile("request-book-covers", coverName, coverBytes)

                // Upload File Pendukung (Opsional)
                var supportUrl: String? = null
                if (currentState.supportingFileUri != null) {
                    val supportBytes = context.contentResolver.openInputStream(currentState.supportingFileUri!!)?.use { it.readBytes() }
                    if (supportBytes != null) {
                        val supportName = "$userId/${UUID.randomUUID()}_doc"
                        supportUrl = repository.uploadFile("request-book-files", supportName, supportBytes)
                    }
                }

                // Simpan ke Database
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

                _state.update { it.copy(isLoading = false, isSuccess = true) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, errorMessage = "Gagal: ${e.message}") }
            }
        }
    }
}