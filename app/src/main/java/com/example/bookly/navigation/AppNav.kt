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
import com.example.bookly.ui.BookCatalogScreen
import com.example.bookly.ui.BookDetailScreen
import com.example.bookly.ui.ChangePasswordScreen
import com.example.bookly.ui.PeminjamanScreen
import com.example.bookly.ui.WishlistScreen
import com.example.bookly.viewmodel.WishlistViewModel

@Composable
fun AppNav() {
    val navController = rememberNavController()
    val wishlistViewModel: WishlistViewModel = viewModel()

    NavHost(navController = navController, startDestination = "login") {
        composable("login") { LoginScreen(navController = navController) }
        composable("register") { RegisterScreen(navController = navController) }
        composable("profile") {
            ProfileScreen(
                navController = navController,
                wishlistViewModel = wishlistViewModel
            )
        }
        composable("change_password") {
            ChangePasswordScreen(navController = navController)
        }
        composable("katalog_buku") {
            BookCatalogScreen(
                navController = navController,
                wishlistViewModel = wishlistViewModel
            )
        }
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
        composable(
            route = "book_borrow_confirm/{bookId}",
            arguments = listOf(navArgument("bookId") { type = NavType.StringType })
        ) { backStackEntry ->
            val bookId = backStackEntry.arguments?.getString("bookId") ?: ""
            com.example.bookly.ui.Bookdetailscreen2(navController = navController, bookId = bookId)
        }
        composable("home") { Text(text = "Beranda Screen") }
        composable("peminjaman") { PeminjamanScreen(navController = navController) }
        composable(
            route = "peminjamanScreen1/{loanId}",
            arguments = listOf(navArgument("loanId") { type = NavType.StringType })
        ) { backStackEntry ->
            val loanId = backStackEntry.arguments?.getString("loanId") ?: ""
            com.example.bookly.ui.PeminjamanScreen1(navController = navController, loanId = loanId)
        }
        composable("wishlist") {
            WishlistScreen(
                navController = navController,
                wishlistViewModel = wishlistViewModel
            )
        }
        composable("notifikasi") { Text(text = "Notifikasi Screen") } // Kept for now, can be removed if not needed
    }
}
