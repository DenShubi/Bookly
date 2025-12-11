package com.example.bookly.viewmodel

import androidx.lifecycle.ViewModel
import com.example.bookly.ui.Book
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class WishlistViewModel : ViewModel() {
    private val _wishlist = MutableStateFlow<List<Book>>(emptyList())
    val wishlist: StateFlow<List<Book>> = _wishlist.asStateFlow()

    fun addToWishlist(book: Book) {
        if (!isWishlisted(book.title)) {
            _wishlist.value = _wishlist.value + book
        }
    }

    fun removeFromWishlist(bookTitle: String) {
        _wishlist.value = _wishlist.value.filter { it.title != bookTitle }
    }

    fun toggleWishlist(book: Book) {
        if (isWishlisted(book.title)) {
            removeFromWishlist(book.title)
        } else {
            addToWishlist(book)
        }
    }

    fun isWishlisted(bookTitle: String): Boolean {
        return _wishlist.value.any { it.title == bookTitle }
    }
}

