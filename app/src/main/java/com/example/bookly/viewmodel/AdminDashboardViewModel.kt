package com.example.bookly.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookly.supabase.AdminBooksRepository
import com.example.bookly.supabase.BooksRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AdminDashboardUiState(
    val books: List<BooksRepository.BookRow> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class AdminDashboardViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(AdminDashboardUiState())
    val uiState: StateFlow<AdminDashboardUiState> = _uiState.asStateFlow()

    init {
        loadBooks()
    }

    fun loadBooks() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            val result = AdminBooksRepository.getAllBooks()
            result.onSuccess { books ->
                _uiState.value = _uiState.value.copy(
                    books = books,
                    isLoading = false
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = error.message ?: "Gagal memuat buku"
                )
            }
        }
    }

    fun deleteBook(bookId: String) {
        viewModelScope.launch {
            val result = AdminBooksRepository.deleteBook(bookId)
            result.onSuccess {
                // Reload books after successful deletion
                loadBooks()
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = error.message ?: "Gagal menghapus buku"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}

