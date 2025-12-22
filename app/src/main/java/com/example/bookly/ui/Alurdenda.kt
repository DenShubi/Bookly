package com.example.bookly.alur

import java.util.Date

enum class StatusDenda {
    BELUM_DIBAYAR,
    PENDING
}

data class DendaItem(
    val id: String,
    val judulBuku: String,
    val coverUrl: String?,
    val hariTerlambat: Int,
    val tanggal: Date,
    val nominal: Int,
    val status: StatusDenda
)
