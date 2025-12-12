package com.example.bookly.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookly.supabase.WishlistRepository
import com.example.bookly.ui.Book
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for managing wishlist state with server synchronization.
 * Implements optimistic UI pattern:
 * 1. UI updates instantly
 * 2. Background job sends request to server
 * 3. If request fails, revert UI and show error
 */
class WishlistViewModel : ViewModel() {
    private val _wishlist = MutableStateFlow<List<Book>>(emptyList())
    val wishlist: StateFlow<List<Book>> = _wishlist.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    companion object {
        private const val TAG = "WishlistViewModel"
    }

    /**
     * Load wishlist from server for the current user.
     * Should be called after user login or when app starts.
     */
    fun loadWishlist(allBooks: List<Book>) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = WishlistRepository.getWishlist()
                result.onSuccess { bookIds ->
                    // Filter books that are in the wishlist
                    val wishlistedBooks = allBooks.filter { book -> bookIds.contains(book.id) }
                    _wishlist.value = wishlistedBooks
                    Log.d(TAG, "Wishlist loaded: ${bookIds.size} items")
                }.onFailure { error ->
                    Log.e(TAG, "Failed to load wishlist", error)
                    _toastMessage.value = "Failed to load wishlist: ${error.message}"
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Clears the wishlist (e.g., when user logs out).
     */
    fun clearWishlist() {
        _wishlist.value = emptyList()
    }

    /**
     * Adds a book to wishlist with optimistic UI update.
     */
    fun addToWishlist(book: Book) {
        if (_wishlist.value.any { it.id == book.id }) return

        // Step 1: Update UI optimistically
        val previousList = _wishlist.value
        _wishlist.value = previousList + book

        // Step 2: Send request to server in background
        viewModelScope.launch {
            val result = WishlistRepository.addToWishlist(book.id)
            result.onFailure { error ->
                // Step 3: Revert UI on failure
                _wishlist.value = previousList
                _toastMessage.value = "Failed to add to wishlist: ${error.message}"
                Log.e(TAG, "Failed to add book ${book.id} to wishlist", error)
            }
        }
    }

    /**
     * Removes a book from wishlist with optimistic UI update.
     * Can accept either book ID or book title for backwards compatibility.
     */
    fun removeFromWishlist(bookIdentifier: String) {
        // Try to find by ID first, then by title
        val bookToRemove = _wishlist.value.find { it.id == bookIdentifier }
            ?: _wishlist.value.find { it.title == bookIdentifier }

        if (bookToRemove == null) return

        // Step 1: Update UI optimistically
        val previousList = _wishlist.value
        _wishlist.value = previousList.filter { it.id != bookToRemove.id }

        // Step 2: Send request to server in background
        viewModelScope.launch {
            val result = WishlistRepository.removeFromWishlist(bookToRemove.id)
            result.onFailure { error ->
                // Step 3: Revert UI on failure
                _wishlist.value = previousList
                _toastMessage.value = "Failed to remove from wishlist: ${error.message}"
                Log.e(TAG, "Failed to remove book ${bookToRemove.id} from wishlist", error)
            }
        }
    }

    /**
     * Toggles wishlist state for a book.
     */
    fun toggleWishlist(book: Book) {
        if (_wishlist.value.any { it.id == book.id }) {
            removeFromWishlist(book.id)
        } else {
            addToWishlist(book)
        }
    }

    /**
     * Checks if a book is in the wishlist.
     * Can accept either book ID or book title for backwards compatibility.
     */
    fun isWishlisted(identifier: String): Boolean {
        return _wishlist.value.any { it.id == identifier || it.title == identifier }
    }

    /**
     * Clears the toast message after it has been shown.
     */
    fun clearToastMessage() {
        _toastMessage.value = null
    }
}

