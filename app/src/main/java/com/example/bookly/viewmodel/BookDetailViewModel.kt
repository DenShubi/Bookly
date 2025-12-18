package com.example.bookly.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookly.supabase.BooksRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class BookDetailData(
    val id: String,
    val title: String,
    val author: String,
    val publisher: String,
    val publicationYear: Int,
    val language: String,
    val pages: Int,
    val category: String?,
    val categoryColor: String?,
    val description: String,
    val coverImageUrl: String?,
    val rating: Float?,
    val totalCopies: Int,
    val availableCopies: Int
)

class BookDetailViewModel : ViewModel() {
    private val _bookDetail = MutableStateFlow<BookDetailData?>(null)
    val bookDetail: StateFlow<BookDetailData?> = _bookDetail.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun loadBookDetail(bookId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val result = BooksRepository.getBookById(bookId)
                if (result.isSuccess) {
                    val book = result.getOrNull()
                    if (book != null) {
                        _bookDetail.value = BookDetailData(
                            id = book.id,
                            title = book.title,
                            author = book.author,
                            publisher = book.publisher ?: "-",
                            publicationYear = book.publicationYear ?: 0,
                            language = book.language ?: "-",
                            pages = book.pages ?: 0,
                            category = book.category,
                            categoryColor = book.categoryColor,
                            description = book.description ?: "Tidak ada deskripsi tersedia.",
                            coverImageUrl = book.coverImageUrl,
                            rating = book.rating,
                            totalCopies = book.totalCopies,
                            availableCopies = book.availableCopies
                        )
                    } else {
                        _errorMessage.value = "Buku tidak ditemukan"
                    }
                } else {
                    _errorMessage.value = "Gagal memuat detail buku"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Terjadi kesalahan: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}

