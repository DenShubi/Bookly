package com.example.bookly.supabase

import android.util.Log
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository for admin-specific book operations.
 * Provides CRUD operations for books table.
 */
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
}

