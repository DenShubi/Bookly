# 📚 Bookly - Library Management System

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9+-purple.svg)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-UI-blue.svg)](https://developer.android.com/jetpack/compose)
[![Supabase](https://img.shields.io/badge/Backend-Supabase-green.svg)](https://supabase.com)

Modern Android application untuk manajemen perpustakaan digital dengan fitur lengkap untuk peminjaman buku, wishlist, review, dan sistem denda.

## ✨ Features

### 📖 Book Management
- **Katalog Buku** - Browse dan search koleksi buku dengan kategori
- **Detail Buku** - Informasi lengkap buku (penulis, penerbit, tahun terbit, deskripsi)
- **Rating & Reviews** - Sistem rating bintang dengan foto dan text review
- **Book Request** - User dapat request buku baru ke admin

### ❤️ User Features
- **Wishlist** - Simpan buku favorit untuk dipinjam nanti
- **Borrowing System** - Pinjam buku dengan tracking due date dan status
- **Extension** - Perpanjang masa peminjaman (max 2x)
- **Fines Management** - Sistem denda otomatis untuk keterlambatan
- **Payment Submission** - Upload bukti pembayaran denda

### 👤 Account & Authentication
- **Login/Register** - Email & password authentication via Supabase Auth
- **Profile Management** - Update nama, avatar, phone number
- **KYC Verification** - Verifikasi identitas dengan selfie
- **Change Password** - Ubah password dengan validasi

### 👨‍💼 Admin Features
- **Admin Dashboard** - Kelola buku, user, dan transaksi
- **Book Management** - Add, edit, delete buku
- **Request Approval** - Approve/reject book requests
- **Fine Management** - Kelola denda user
- **Payment Verification** - Verifikasi pembayaran denda

## 🏗️ Tech Stack

### Frontend
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose (Material 3)
- **Architecture**: MVVM (Model-View-ViewModel)
- **Navigation**: Navigation Component
- **Image Loading**: Coil
- **Coroutines**: Kotlin Coroutines + Flow

### Backend
- **BaaS**: Supabase
- **Database**: PostgreSQL
- **Authentication**: Supabase Auth
- **Storage**: Supabase Storage (book covers, review images, avatars)
- **Security**: Row Level Security (RLS) Policies

### Key Libraries
```gradle
// Compose
implementation("androidx.compose.ui:ui")
implementation("androidx.compose.material3:material3")

// Supabase
implementation("io.github.jan-tennert.supabase:postgrest-kt")
implementation("io.github.jan-tennert.supabase:auth-kt")
implementation("io.github.jan-tennert.supabase:storage-kt")

// Image Loading
implementation("io.coil-kt:coil-compose")

// Navigation
implementation("androidx.navigation:navigation-compose")
```

## 📂 Project Structure

```
app/src/main/java/com/example/bookly/
├── ui/                          # UI Screens (Compose)
│   ├── HomeScreen.kt
│   ├── BookCatalogScreen.kt
│   ├── BookDetailScreen.kt
│   ├── WishlistScreen.kt
│   ├── ReviewListScreen.kt
│   ├── SharedComponents.kt
│   └── ...
├── viewmodel/                   # ViewModels (MVVM)
│   ├── HomeViewModel.kt
│   ├── BookDetailViewModel.kt
│   ├── WishlistViewModel.kt
│   └── ...
├── supabase/                    # Data Layer
│   ├── SupabaseClientProvider.kt
│   ├── BooksRepository.kt
│   ├── ReviewRepository.kt
│   └── ...
└── navigation/                  # Navigation
    └── AppNav.kt
```
