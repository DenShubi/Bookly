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

## 📱 Screenshots

<!-- Add your screenshots here -->
- Home Screen (Popular & Recommended Books)
- Book Catalog
- Book Detail & Reviews
- Wishlist
- Borrowing History
- Admin Dashboard

## 🚀 Getting Started

### Prerequisites
- Android Studio Hedgehog | 2023.1.1 or newer
- JDK 17
- Android SDK API Level 34
- Supabase Account

### Installation

1. **Clone repository**
```bash
git clone https://github.com/yourusername/Bookly.git
cd Bookly
```

2. **Setup Supabase**
   - Create a new project at [supabase.com](https://supabase.com)
   - Run database migrations from `/database` folder
   - Run SQL script untuk RLS policies dan triggers:
     ```sql
     -- Di Supabase SQL Editor, run:
     database/fix_rating_rls_policy.sql
     ```
   - Enable Authentication → Email provider
   - Create storage buckets: `book-covers`, `review-images`, `avatars`, `payment-proofs`

3. **Configure API Keys**
   
   Create `local.properties` di root project (jangan commit file ini):
   ```properties
   supabase.url=YOUR_SUPABASE_URL
   supabase.key=YOUR_SUPABASE_ANON_KEY
   ```

4. **Update SupabaseClientProvider.kt**
   ```kotlin
   // app/src/main/java/com/example/bookly/supabase/SupabaseClientProvider.kt
   object SupabaseClientProvider {
       val client = createSupabaseClient(
           supabaseUrl = "YOUR_SUPABASE_URL",
           supabaseKey = "YOUR_SUPABASE_ANON_KEY"
       ) {
           // ... config
       }
   }
   ```

5. **Build & Run**
```bash
./gradlew assembleDebug
# atau langsung run di Android Studio
```

## 🗄️ Database Schema

### Main Tables
- `users` - User profiles (linked to Supabase Auth)
- `books` - Book catalog with auto-calculated ratings
- `reviews` - User reviews with ratings (1-5) and photos
- `wishlist` - User's saved books
- `borrowing_records` - Book borrowing history with status tracking
- `fines` - Fines for overdue books
- `payment_submissions` - Payment proof uploads
- `book_requests` - User requests for new books
- `kyc_verifications` - User identity verification

### Key Features in Database
- **Auto-calculated Rating**: Database triggers update `books.rating` from average of reviews
- **RLS Policies**: Row-level security untuk data isolation
- **Foreign Keys**: Referential integrity untuk relasi antar tabel

## 🔐 Authentication & Security

### User Roles
- **Regular User** (default)
  - Browse & search books
  - Borrow & return books
  - Write reviews
  - Manage wishlist
  
- **Admin** (`admin@mail.com`)
  - All user permissions
  - Manage books
  - Approve/reject requests
  - Manage fines & payments

### Security Implementation
- Supabase RLS policies enforce data access rules
- JWT tokens untuk authenticated requests
- Server-side validation untuk critical operations
- Secure password reset via email

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

## 🐛 Known Issues & Solutions

### Rating Not Updating
If book ratings don't update after submitting reviews:
1. Run SQL fix: `database/fix_rating_rls_policy.sql`
2. This creates proper RLS policies and database triggers

### UI Not Refreshing
- Pull-to-refresh on catalog screen
- Navigate back and forth to detail screen
- Database triggers ensure rating updates automatically

## 🤝 Contributing

Contributions are welcome! Please follow these steps:
1. Fork the repository
2. Create feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to branch (`git push origin feature/AmazingFeature`)
5. Open Pull Request

## 📝 License

This project is for educational purposes. 

## 👥 Authors

- **Your Name** - Initial work - [YourGithub](https://github.com/yourusername)

## 🙏 Acknowledgments

- [Supabase](https://supabase.com) - Backend as a Service
- [Jetpack Compose](https://developer.android.com/jetpack/compose) - Modern UI toolkit
- [Coil](https://coil-kt.github.io/coil/) - Image loading library

---

Made with ❤️ using Kotlin & Jetpack Compose
