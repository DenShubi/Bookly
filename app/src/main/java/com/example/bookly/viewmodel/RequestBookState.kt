package com.example.bookly.viewmodel

import android.net.Uri // <--- JANGAN LUPA INI

data class RequestBookState(
    val title: String = "",
    val author: String = "",
    val year: String = "",
    val reason: String = "",
    val coverUri: Uri? = null,
    val supportingFileUri: Uri? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false
)