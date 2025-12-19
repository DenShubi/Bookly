package com.example.bookly.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookly.supabase.BooksRepository
import com.example.bookly.supabase.ReviewRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

// Tambahkan field untuk info buku di UI State
data class ReviewUiState(
    val reviews: List<ReviewRepository.ReviewRow> = emptyList(),
    val currentUserId: String? = null,

    // Data Buku untuk tampilan Form
    val bookTitle: String = "",
    val bookAuthor: String = "",
    val bookCoverUrl: String? = null,

    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val isSubmitSuccess: Boolean = false
)

class ReviewViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ReviewUiState())
    val uiState: StateFlow<ReviewUiState> = _uiState.asStateFlow()

    fun loadBookInfo(bookId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val result = BooksRepository.getBookById(bookId)

            result.fold(
                onSuccess = { book ->

                    if (book != null) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            bookTitle = book.title,           // Sudah benar
                            bookAuthor = book.author,         // Sudah benar
                            bookCoverUrl = book.coverImageUrl // Sudah benar (sesuai BookRow kamu)
                        )
                    } else {
                        // Jika buku tidak ditemukan di database
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Buku tidak ditemukan"
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.localizedMessage
                    )
                }
            )
        }
    }

    // --- 2. Load Review (Sama seperti sebelumnya) ---
    fun loadReviews(bookId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            // Ambil ID user yang sedang login
            val userId = ReviewRepository.getCurrentUserId()

            val result = ReviewRepository.getReviewsByBookId(bookId)
            result.fold(
                onSuccess = { reviews ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        reviews = reviews,
                        currentUserId = userId // Simpan di state
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = error.localizedMessage)
                }
            )
        }
    }

    fun deleteReview(reviewId: String, bookId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            // Panggil repository dengan bookId
            val result = ReviewRepository.deleteReview(reviewId, bookId)
            if (result.isSuccess) {
                // Reload reviews setelah delete
                loadReviews(bookId)
                _uiState.value = _uiState.value.copy(
                    successMessage = "Review berhasil dihapus",
                    isLoading = false
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    errorMessage = result.exceptionOrNull()?.message ?: "Gagal menghapus review",
                    isLoading = false
                )
            }
        }
    }

    // --- 3. Upload Foto & Submit Review ---
    fun submitReviewWithPhotos(
        context: Context, // Butuh Context untuk baca file gambar
        bookId: String,
        rating: Int,
        reviewText: String,
        photoUris: List<Uri> // Menerima List URI dari Galeri HP
    ) {
        viewModelScope.launch {
            if (rating == 0) {
                _uiState.value = _uiState.value.copy(errorMessage = "Mohon beri rating bintang")
                return@launch
            }

            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, isSubmitSuccess = false)

            try {
                val uploadedUrls = mutableListOf<String>()

                // A. Loop setiap URI gambar dan Upload ke Supabase
                photoUris.forEach { uri ->
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val bytes = inputStream?.readBytes()

                    if (bytes != null) {
                        // Buat nama file unik: "review_TIMESTAMP_UUID.jpg"
                        val fileName = "review_${System.currentTimeMillis()}_${UUID.randomUUID()}.jpg"

                        val uploadResult = ReviewRepository.uploadReviewPhoto(bytes, fileName)
                        uploadResult.onSuccess { url ->
                            uploadedUrls.add(url)
                        }
                    }
                }

                // B. Setelah semua foto terupload, kirim data review ke Database
                val submitResult = ReviewRepository.submitReview(
                    bookId = bookId,
                    rating = rating,
                    reviewText = reviewText,
                    photoUrls = uploadedUrls // Masukkan list URL foto
                )

                submitResult.fold(
                    onSuccess = {
                        _uiState.value = _uiState.value.copy(isLoading = false, isSubmitSuccess = true)
                        loadReviews(bookId) // Refresh list
                    },
                    onFailure = { e ->
                        _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = "Gagal kirim review: ${e.message}")
                    }
                )

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = "Error: ${e.message}")
            }
        }
    }

    fun resetSubmitStatus() {
        _uiState.value = _uiState.value.copy(isSubmitSuccess = false, errorMessage = null)
    }
}