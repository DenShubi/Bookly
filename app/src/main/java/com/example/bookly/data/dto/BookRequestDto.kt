package com.example.bookly.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class BookRequestDto(
    val user_id: String,
    val title: String,
    val author: String,
    val year: Int,
    val reason: String,
    val cover_url: String,
    val supporting_file_url: String? = null,
    val status: String = "pending"
)