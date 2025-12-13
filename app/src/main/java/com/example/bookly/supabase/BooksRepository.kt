package com.example.bookly.supabase

import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

object BooksRepository {
    private val httpClient = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    suspend fun getBooks(): Result<List<BookRow>> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseClientProvider.SUPABASE_URL}/rest/v1/books?select=*"
            val response: HttpResponse = httpClient.get(url) {
                headers {
                    SupabaseClientProvider.headersWithAnon().forEach { (k, v) -> append(k, v) }
                }
                accept(ContentType.Application.Json)
            }
            val respText = response.bodyAsText()
            val list = Json.decodeFromString(ListSerializer(BookRow.serializer()), respText)
            Result.success(list)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    suspend fun getBookById(bookId: String): Result<BookRow?> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseClientProvider.SUPABASE_URL}/rest/v1/books?select=*&id=eq.$bookId"
            val response: HttpResponse = httpClient.get(url) {
                headers {
                    SupabaseClientProvider.headersWithAnon().forEach { (k, v) -> append(k, v) }
                }
                accept(ContentType.Application.Json)
            }
            val respText = response.bodyAsText()
            val list = Json.decodeFromString(ListSerializer(BookRow.serializer()), respText)
            Result.success(list.firstOrNull())
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    @Serializable
    data class BookRow(
        @SerialName("id") val id: String = "",
        @SerialName("title") val title: String = "",
        @SerialName("author") val author: String = "",
        @SerialName("publisher") val publisher: String? = null,
        @SerialName("publication_year") val publicationYear: Int? = null,
        @SerialName("language") val language: String? = null,
        @SerialName("pages") val pages: Int? = null,
        @SerialName("category") val category: String? = null,
        @SerialName("category_color") val categoryColor: String? = null,
        @SerialName("description") val description: String? = null,
        @SerialName("cover_image_url") val coverImageUrl: String? = null,
        @SerialName("rating") val rating: Float = 0f,
        @SerialName("total_copies") val totalCopies: Int = 1,
        @SerialName("available_copies") val availableCopies: Int = 1
    )
}
