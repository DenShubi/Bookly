package com.example.bookly.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookly.supabase.AdminBooksRepository
import com.example.bookly.supabase.BooksRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class CategoryOption(
    val name: String,
    val color: String
)

data class AdminAddBookUiState(
    val title: String = "",
    val author: String = "",
    val category: String = "Novel",
    val categoryColor: String = "#a5ed99",
    val coverImageUrl: String = "",
    val publisher: String = "",
    val year: Int = 2024,
    val language: String = "Indonesia",
    val totalCopies: Int = 0,
    val description: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSaved: Boolean = false
)

class AdminAddBookViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(AdminAddBookUiState())
    val uiState: StateFlow<AdminAddBookUiState> = _uiState.asStateFlow()

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

    fun updateCoverImageUrl(url: String) {
        _uiState.value = _uiState.value.copy(coverImageUrl = url)
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
        _uiState.value = _uiState.value.copy(totalCopies = total)
    }

    fun updateDescription(description: String) {
        _uiState.value = _uiState.value.copy(description = description)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun saveBook() {
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

            val book = BooksRepository.BookRow(
                id = UUID.randomUUID().toString(),
                title = state.title,
                author = state.author,
                publisher = state.publisher,
                publicationYear = state.year,
                language = state.language,
                pages = 0,
                category = state.category,
                categoryColor = state.categoryColor,
                description = state.description,
                coverImageUrl = state.coverImageUrl,
                rating = 0f,
                totalCopies = state.totalCopies,
                availableCopies = state.totalCopies
            )

            val result = AdminBooksRepository.insertBook(book)
            result.onSuccess {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isSaved = true
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = error.message ?: "Gagal menyimpan buku"
                )
            }
        }
    }
}

