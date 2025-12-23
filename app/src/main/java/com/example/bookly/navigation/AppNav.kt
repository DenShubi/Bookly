package com.example.bookly.navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.bookly.LoginScreen
import com.example.bookly.ProfileScreen
import com.example.bookly.RegisterScreen
import com.example.bookly.ui.AdminAddBookScreen
import com.example.bookly.ui.AdminDashboardScreen
import com.example.bookly.ui.BookCatalogScreen
import com.example.bookly.ui.BookDetailScreen
import com.example.bookly.ui.BookRequestScreen
import com.example.bookly.ui.ChangePasswordScreen
import com.example.bookly.ui.HomeScreen
import com.example.bookly.ui.PeminjamanScreen
import com.example.bookly.ui.ReviewListScreen
import com.example.bookly.ui.ReviewScreen
import com.example.bookly.ui.SplashScreen
import com.example.bookly.ui.WishlistScreen
import com.example.bookly.viewmodel.WishlistViewModel

@Composable
fun AppNav() {
    val navController = rememberNavController()
    val wishlistViewModel: WishlistViewModel = viewModel()

    // Start Destination dimulai dari splash untuk check session
    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") { SplashScreen(navController = navController) }
        composable("login") { LoginScreen(navController = navController) }
        composable("register") { RegisterScreen(navController = navController) }

        // --- ADMIN DASHBOARD ---
        composable("admin_dashboard") { AdminDashboardScreen(navController = navController) }

        // --- ADMIN ADD BOOK ---
        composable("admin_add_book") { AdminAddBookScreen(navController = navController) }

        // --- ADMIN EDIT BOOK ---
        composable(
                route = "admin_edit_book/{bookId}",
                arguments = listOf(navArgument("bookId") { type = NavType.StringType })
        ) { backStackEntry ->
            val bookId = backStackEntry.arguments?.getString("bookId") ?: ""
            com.example.bookly.ui.AdminEditBookScreen(
                    navController = navController,
                    bookId = bookId
            )
        }

        // --- HOME SCREEN (BERANDA) ---
        composable("home") {
            HomeScreen(navController = navController, wishlistViewModel = wishlistViewModel)
        }

        // --- KATALOG BUKU ---
        composable("katalog_buku") {
            BookCatalogScreen(navController = navController, wishlistViewModel = wishlistViewModel)
        }

        // --- BOOK REQUEST (NEW) ---
        composable("request_book") {
            BookRequestScreen(
                    navController = navController,
                    onBack = { navController.popBackStack() }
            )
        }

        composable("change_password") { ChangePasswordScreen(navController = navController) }

        // --- DETAIL BUKU ---
        composable(
                route = "book_detail/{bookId}",
                arguments = listOf(navArgument("bookId") { type = NavType.StringType })
        ) { backStackEntry ->
            val bookId = backStackEntry.arguments?.getString("bookId") ?: ""
            BookDetailScreen(
                    navController = navController,
                    bookId = bookId,
                    wishlistViewModel = wishlistViewModel
            )
        }

        // --- PEMINJAMAN ---
        composable("peminjaman") { PeminjamanScreen(navController = navController) }

        // --- WISHLIST ---
        composable("wishlist") {
            WishlistScreen(navController = navController, wishlistViewModel = wishlistViewModel)
        }

        // --- PROFILE ---
        composable("profile") {
            ProfileScreen(navController = navController, wishlistViewModel = wishlistViewModel)
        }

        // --- NOTIFIKASI (Opsional) ---
        composable("notifikasi") { Text(text = "Notifikasi Screen") }

        // --- FITUR TAMBAHAN (Loan, Review, dll) ---
        composable(
                route = "book_borrow/{bookId}",
                arguments = listOf(navArgument("bookId") { type = NavType.StringType })
        ) { backStackEntry ->
            val bookId = backStackEntry.arguments?.getString("bookId") ?: ""
            com.example.bookly.ui.BookDetailScreen(navController = navController, bookId = bookId)
        }

        composable(
                route = "book_borrow_confirm/{bookId}",
                arguments = listOf(navArgument("bookId") { type = NavType.StringType })
        ) { backStackEntry ->
            val bookId = backStackEntry.arguments?.getString("bookId") ?: ""
            com.example.bookly.ui.LendingScreen(navController = navController, bookId = bookId)
        }

        composable(
                route = "peminjamanScreen1/{loanId}",
                arguments = listOf(navArgument("loanId") { type = NavType.StringType })
        ) { backStackEntry ->
            val loanId = backStackEntry.arguments?.getString("loanId") ?: ""
            com.example.bookly.ui.PeminjamanScreen1(navController = navController, loanId = loanId)
        }

        composable(
                route = "status_peminjaman/{loanId}",
                arguments = listOf(navArgument("loanId") { type = NavType.StringType })
        ) { backStackEntry ->
            val loanId = backStackEntry.arguments?.getString("loanId") ?: ""
            com.example.bookly.ui.StatusPeminjaman(navController = navController, loanId = loanId)
        }

        // --- FINE PAYMENT DETAIL ---
        composable(
                route = "fine_payment_detail/{fineId}",
                arguments = listOf(navArgument("fineId") { type = NavType.StringType })
        ) { backStackEntry ->
            val fineId = backStackEntry.arguments?.getString("fineId") ?: ""
            com.example.bookly.ui.FinePaymentDetailScreen(navController = navController, fineId = fineId)
        }

        // --- ADMIN PAYMENT VERIFICATION ---
        composable("admin_payment_verification") {
            com.example.bookly.ui.AdminPaymentVerificationScreen(navController = navController)
        }

        // --- REVIEW LIST ---
        composable(
                route = "review/{bookId}",
                arguments = listOf(navArgument("bookId") { type = NavType.StringType })
        ) { backStackEntry ->
            val bookId = backStackEntry.arguments?.getString("bookId") ?: ""
            ReviewListScreen(navController = navController, bookId = bookId)
        }

        // --- REVIEW FORM ---
        composable(
                route = "review_form/{bookId}",
                arguments = listOf(navArgument("bookId") { type = NavType.StringType })
        ) { backStackEntry ->
            val bookId = backStackEntry.arguments?.getString("bookId") ?: ""
            ReviewScreen(navController = navController)
        }
    }
}
