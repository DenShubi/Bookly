package com.example.bookly.viewmodel


import android.net.Uri

data class RequestBookState(
    val title: String = "",
    val author: String = "",
    val year: String = "", // String dulu inputnya, nanti convert ke Int
    val reason: String = "",
    val coverUri: Uri? = null, // URI lokal dari Image Picker
    val supportingFileUri: Uri? = null, // URI lokal file pendukung
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false
)