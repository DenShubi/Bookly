package com.example.bookly.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookly.supabase.BooksRepository
import com.example.bookly.ui.BookDummy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val userName: String = "Loading...",
    val popularBooks: List<BookDummy> = emptyList(),
    val recommendedBooks: List<BookDummy> = emptyList(),
    val isLoading: Boolean = true
)

class HomeViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHomeData()
    }

    fun loadHomeData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            // 1. Ambil Nama User
            val name = BooksRepository.getCurrentUserName()

            // 2. Ambil Buku Populer
            val popularResult = BooksRepository.getPopularBooks()
            val popularList = popularResult.getOrDefault(emptyList()).map { it.toBookDummy() }

            // 3. Ambil Rekomendasi
            val recommendedResult = BooksRepository.getRecommendedBooks()
            val recommendedList = recommendedResult.getOrDefault(emptyList()).map { it.toBookDummy() }

            _uiState.value = HomeUiState(
                userName = name,
                popularBooks = popularList,
                recommendedBooks = recommendedList,
                isLoading = false
            )
        }
    }

    // Helper: Konversi dari Database Row ke UI Dummy Model
    private fun BooksRepository.BookRow.toBookDummy(): BookDummy {
        return BookDummy(
            id = this.id,
            title = this.title,
            author = this.author,
            rating = this.rating.toInt(),
            stock = this.availableCopies,
            category = this.category ?: "Umum",
            coverUrl = this.coverImageUrl ?: ""
        )
    }
}