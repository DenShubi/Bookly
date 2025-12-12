package com.example.bookly.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookly.supabase.BooksRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BookCatalogViewModel : ViewModel() {
    private val _books = MutableStateFlow<List<BooksRepository.BookRow>>(emptyList())
    val books: StateFlow<List<BooksRepository.BookRow>> = _books.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        // Load books initially
        refreshBooks()
    }

    fun refreshBooks() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = BooksRepository.getBooks()
                if (result.isSuccess) {
                    _books.value = result.getOrNull().orEmpty()
                } else {
                    // Keep existing books or empty list on failure
                    _books.value = emptyList()
                }
            } catch (e: Exception) {
                // Handle exception, keep existing books
                _books.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
}

