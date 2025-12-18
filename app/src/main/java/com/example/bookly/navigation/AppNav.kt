package com.example.bookly.navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.bookly.LoginScreen
import com.example.bookly.ProfileScreen
import com.example.bookly.RegisterScreen
// Pastikan import ini tidak merah (artinya file sudah dibuat dengan benar)
import com.example.bookly.supabase.RequestBookRepository
import com.example.bookly.supabase.SupabaseClientProvider
import com.example.bookly.ui.RequestBookScreen
import com.example.bookly.viewmodel.RequestBookViewModel
// -----------------------------------------------------------------------
import com.example.bookly.ui.AdminAddBookScreen
import com.example.bookly.ui.AdminDashboardScreen
import com.example.bookly.ui.ChangePasswordScreen
import com.example.bookly.ui.BookCatalogScreen
import com.example.bookly.ui.BookDetailScreen
import com.example.bookly.ui.HomeScreen
import com.example.bookly.ui.PeminjamanScreen
import com.example.bookly.ui.ReviewListScreen
import com.example.bookly.ui.ReviewScreen
import com.example.bookly.ui.WishlistScreen
import com.example.bookly.viewmodel.WishlistViewModel

@Composable
fun AppNav() {
    val navController = rememberNavController()
    // WishlistViewModel dishare untuk akses global
    val wishlistViewModel: WishlistViewModel = viewModel()

    NavHost(navController = navController, startDestination = "login") {

        composable("login") { LoginScreen(navController = navController) }
        composable("register") { RegisterScreen(navController = navController) }

        // --- ADMIN ROUTES ---
        composable("admin_dashboard") { AdminDashboardScreen(navController = navController) }
        composable("admin_add_book") { AdminAddBookScreen(navController = navController) }
        composable(
            route = "admin_edit_book/{bookId}",
            arguments = listOf(navArgument("bookId") { type = NavType.StringType })
        ) { backStackEntry ->
            val bookId = backStackEntry.arguments?.getString("bookId") ?: ""
            com.example.bookly.ui.AdminEditBookScreen(navController = navController, bookId = bookId)
        }

        // --- USER ROUTES ---
        composable("home") { HomeScreen(navController = navController) }

        composable("katalog_buku") {
            BookCatalogScreen(
                navController = navController,
                wishlistViewModel = wishlistViewModel
            )
        }

        // --- REQUEST BUKU BARU (FIXED) ---
        composable("request_book") {
            // Kita gunakan Factory agar ViewModel tidak error saat minta Repository
            val viewModel: RequestBookViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        val repo = RequestBookRepository(SupabaseClientProvider.client)
                        return RequestBookViewModel(repo) as T
                    }
                }
            )
            RequestBookScreen(navController = navController, viewModel = viewModel)
        }
        // ---------------------------------

        composable("change_password") { ChangePasswordScreen(navController = navController) }

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

        composable("peminjaman") { PeminjamanScreen(navController = navController) }
        composable("wishlist") { WishlistScreen(navController = navController, wishlistViewModel = wishlistViewModel) }
        composable("profile") { ProfileScreen(navController = navController, wishlistViewModel = wishlistViewModel) }
        composable("notifikasi") { Text(text = "Notifikasi Screen") }

        // --- FITUR LAINNYA ---
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
        composable(
            route = "review/{bookId}",
            arguments = listOf(navArgument("bookId") { type = NavType.StringType })
        ) { backStackEntry ->
            val bookId = backStackEntry.arguments?.getString("bookId") ?: ""
            ReviewListScreen(navController = navController, bookId = bookId)
        }
        composable(
            route = "review_form/{bookId}",
            arguments = listOf(navArgument("bookId") { type = NavType.StringType })
        ) { backStackEntry ->
            val bookId = backStackEntry.arguments?.getString("bookId") ?: ""
            ReviewScreen(navController = navController)
        }
    }
}