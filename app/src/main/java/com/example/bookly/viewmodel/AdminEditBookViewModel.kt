package com.example.bookly.viewmodel

import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookly.supabase.AdminBooksRepository
import com.example.bookly.supabase.BooksRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class AdminEditBookUiState(
    val bookId: String = "",
    val title: String = "",
    val author: String = "",
    val category: String = "Novel",
    val categoryColor: String = "#a5ed99",
    val imageUri: Uri? = null,
    val imageBytes: ByteArray? = null,
    val publisher: String = "",
    val year: Int = 2024,
    val language: String = "Indonesia",
    val totalCopies: Int = 0,
    val availableCopies: Int = 0,
    val description: String = "",
    val isLoading: Boolean = false,
    val isLoadingBook: Boolean = false,
    val errorMessage: String? = null,
    val isSaved: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AdminEditBookUiState

        if (bookId != other.bookId) return false
        if (title != other.title) return false
        if (author != other.author) return false
        if (category != other.category) return false
        if (categoryColor != other.categoryColor) return false
        if (imageUri != other.imageUri) return false
        if (imageBytes != null) {
            if (other.imageBytes == null) return false
            if (!imageBytes.contentEquals(other.imageBytes)) return false
        } else if (other.imageBytes != null) return false
        if (publisher != other.publisher) return false
        if (year != other.year) return false
        if (language != other.language) return false
        if (totalCopies != other.totalCopies) return false
        if (availableCopies != other.availableCopies) return false
        if (description != other.description) return false
        if (isLoading != other.isLoading) return false
        if (isLoadingBook != other.isLoadingBook) return false
        if (errorMessage != other.errorMessage) return false
        if (isSaved != other.isSaved) return false

        return true
    }

    override fun hashCode(): Int {
        var result = bookId.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + author.hashCode()
        result = 31 * result + category.hashCode()
        result = 31 * result + categoryColor.hashCode()
        result = 31 * result + (imageUri?.hashCode() ?: 0)
        result = 31 * result + (imageBytes?.contentHashCode() ?: 0)
        result = 31 * result + publisher.hashCode()
        result = 31 * result + year
        result = 31 * result + language.hashCode()
        result = 31 * result + totalCopies
        result = 31 * result + availableCopies
        result = 31 * result + description.hashCode()
        result = 31 * result + isLoading.hashCode()
        result = 31 * result + isLoadingBook.hashCode()
        result = 31 * result + (errorMessage?.hashCode() ?: 0)
        result = 31 * result + isSaved.hashCode()
        return result
    }
}

class AdminEditBookViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(AdminEditBookUiState())
    val uiState: StateFlow<AdminEditBookUiState> = _uiState.asStateFlow()

    val categoryOptions = listOf(
        CategoryOption("Novel", "#a5ed99"),
        CategoryOption("Bisnis", "#ffd6d6"),
        CategoryOption("Pendidikan", "#d6e9ff"),
        CategoryOption("Sejarah", "#ffe9d6"),
        CategoryOption("Self-Help", "#e9d6ff"),
        CategoryOption("Teknologi", "#fff9d6"),
        CategoryOption("Sains", "#d6fff9"),
        CategoryOption("Agama", "#ffe9f0")
    )

    fun loadBook(bookId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                bookId = bookId,
                isLoadingBook = true,
                errorMessage = null
            )

            val result = AdminBooksRepository.getBookById(bookId)
            result.onSuccess { book ->
                if (book != null) {
                    _uiState.value = _uiState.value.copy(
                        title = book.title,
                        author = book.author,
                        category = book.category ?: "Novel",
                        categoryColor = book.categoryColor ?: "#a5ed99",
                        imageUri = book.coverImageUrl?.takeIf { it.isNotBlank() }?.toUri(),
                        publisher = book.publisher ?: "",
                        year = book.publicationYear ?: 2024,
                        language = book.language ?: "Indonesia",
                        totalCopies = book.totalCopies,
                        availableCopies = book.availableCopies,
                        description = book.description ?: "",
                        isLoadingBook = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoadingBook = false,
                        errorMessage = "Buku tidak ditemukan"
                    )
                }
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoadingBook = false,
                    errorMessage = "Gagal memuat data buku: ${error.message}"
                )
            }
        }
    }

    fun updateTitle(title: String) {
        _uiState.value = _uiState.value.copy(title = title)
    }

    fun updateAuthor(author: String) {
        _uiState.value = _uiState.value.copy(author = author)
    }

    fun updateCategory(categoryName: String) {
        val category = categoryOptions.find { it.name == categoryName }
        if (category != null) {
            _uiState.value = _uiState.value.copy(
                category = category.name,
                categoryColor = category.color
            )
        }
    }

    fun updateImage(uri: Uri?, bytes: ByteArray?) {
        _uiState.value = _uiState.value.copy(
            imageUri = uri,
            imageBytes = bytes
        )
    }

    fun updatePublisher(publisher: String) {
        _uiState.value = _uiState.value.copy(publisher = publisher)
    }

    fun updateYear(year: Int) {
        _uiState.value = _uiState.value.copy(year = year)
    }

    fun updateLanguage(language: String) {
        _uiState.value = _uiState.value.copy(language = language)
    }

    fun updateTotalCopies(total: Int) {
        val state = _uiState.value
        val difference = total - state.totalCopies
        val newAvailableCopies = (state.availableCopies + difference).coerceAtLeast(0)

        _uiState.value = state.copy(
            totalCopies = total,
            availableCopies = newAvailableCopies
        )
    }

    fun updateDescription(description: String) {
        _uiState.value = _uiState.value.copy(description = description)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun updateBook() {
        val state = _uiState.value

        // Validation
        if (state.title.isBlank() || state.author.isBlank() || state.publisher.isBlank()) {
            _uiState.value = state.copy(errorMessage = "Judul, penulis, dan penerbit harus diisi!")
            return
        }

        if (state.totalCopies <= 0) {
            _uiState.value = state.copy(errorMessage = "Total buku harus lebih dari 0!")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            // Upload new image if provided
            var coverImageUrl = state.imageUri?.toString() ?: ""
            if (state.imageBytes != null) {
                val fileName = "book_${UUID.randomUUID()}.jpg"
                val uploadResult = AdminBooksRepository.uploadBookCover(state.imageBytes, fileName)

                uploadResult.onSuccess { url ->
                    coverImageUrl = url
                }.onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Gagal upload gambar: ${error.message}"
                    )
                    return@launch
                }
            }

            val book = BooksRepository.BookRow(
                id = state.bookId,
                title = state.title,
                author = state.author,
                publisher = state.publisher,
                publicationYear = state.year,
                language = state.language,
                pages = 0,
                category = state.category,
                categoryColor = state.categoryColor,
                description = state.description,
                coverImageUrl = coverImageUrl,
                rating = 0f,
                totalCopies = state.totalCopies,
                availableCopies = state.availableCopies
            )

            val result = AdminBooksRepository.updateBook(state.bookId, book)
            result.onSuccess {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isSaved = true
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = error.message ?: "Gagal memperbarui buku"
                )
            }
        }
    }
}

