package com.example.bookly.supabase

import android.util.Log
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

/**
 * Repository for admin-specific book operations.
 * Provides CRUD operations for books table.
 */
@Serializable
private data class BookUpdateDto(
    val title: String,
    val author: String,
    val publisher: String?,
    val publication_year: Int?,
    val language: String?,
    val category: String?,
    val category_color: String?,
    val description: String?,
    val cover_image_url: String?,
    val total_copies: Int,
    val available_copies: Int
)
object AdminBooksRepository {
    private const val TAG = "AdminBooksRepository"

    /**
     * Fetch all books from database
     */
    suspend fun getAllBooks(): Result<List<BooksRepository.BookRow>> = withContext(Dispatchers.IO) {
        try {
            val client = SupabaseClientProvider.client
            val books = client.from("books").select {
                order("title", Order.DESCENDING)
            }.decodeList<BooksRepository.BookRow>()

            Result.success(books)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching books", e)
            Result.failure(e)
        }
    }

    /**
     * Delete a book by ID
     */
    suspend fun deleteBook(bookId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val client = SupabaseClientProvider.client
            client.from("books").delete {
                filter { eq("id", bookId) }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting book", e)
            Result.failure(e)
        }
    }

    /**
     * Insert a new book
     */
    suspend fun insertBook(book: BooksRepository.BookRow): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val client = SupabaseClientProvider.client
            client.from("books").insert(book)

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting book", e)
            Result.failure(e)
        }
    }

    /**
     * Upload book cover image to Supabase storage
     * @param byteArray Image data as byte array
     * @param fileName Unique file name for the image
     * @return Public URL of the uploaded image
     */
    suspend fun uploadBookCover(byteArray: ByteArray, fileName: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val client = SupabaseClientProvider.client
            val bucket = client.storage.from("book-covers")

            // Upload the image
            bucket.upload(fileName, byteArray)

            // Get the public URL
            val publicUrl = bucket.publicUrl(fileName)
            Result.success(publicUrl)
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading book cover", e)
            Result.failure(e)
        }
    }

    /**
     * Get a book by ID
     */
    suspend fun getBookById(bookId: String): Result<BooksRepository.BookRow?> = withContext(Dispatchers.IO) {
        try {
            val client = SupabaseClientProvider.client
            val book = client.from("books").select {
                filter { eq("id", bookId) }
            }.decodeSingleOrNull<BooksRepository.BookRow>()

            Result.success(book)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching book by ID", e)
            Result.failure(e)
        }
    }

    /**
     * Update an existing book
     */
    suspend fun updateBook(
        bookId: String,
        state: BooksRepository.BookRow
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val client = SupabaseClientProvider.client

            val dto = BookUpdateDto(
                title = state.title,
                author = state.author,
                publisher = state.publisher,
                publication_year = state.publicationYear,
                language = state.language,
                category = state.category,
                category_color = state.categoryColor,
                description = state.description,
                cover_image_url = state.coverImageUrl,
                total_copies = state.totalCopies,
                available_copies = state.availableCopies
            )

            client.from("books").update(dto) {
                filter { eq("id", bookId) }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating book", e)
            Result.failure(e)
        }
    }


}

