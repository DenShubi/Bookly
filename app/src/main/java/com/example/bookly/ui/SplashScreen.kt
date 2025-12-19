package com.example.bookly.ui

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.bookly.R
import com.example.bookly.supabase.SupabaseClientProvider
import com.example.bookly.util.SessionManager
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(navController: NavController) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        // Show splash for at least 1.5 seconds
        delay(1500)

        try {
            // Check if user has valid Supabase session
            val currentSession = SupabaseClientProvider.client.auth.currentSessionOrNull()
            val currentUser = SupabaseClientProvider.client.auth.currentUserOrNull()

            if (currentSession != null && currentUser != null) {
                // Valid session exists
                Log.d("SplashScreen", "Valid session found for user: ${currentUser.email}")

                // Restore access token
                SupabaseClientProvider.currentAccessToken = currentSession.accessToken

                // Save session info
                val isAdmin = currentUser.email == "admin@mail.com"
                SessionManager.saveLoginSession(context, currentUser.email ?: "", isAdmin)

                // Navigate to appropriate screen
                val destination = if (isAdmin) "admin_dashboard" else "home"
                navController.navigate(destination) {
                    popUpTo("splash") { inclusive = true }
                }
            } else {
                // No valid session, go to login
                Log.d("SplashScreen", "No valid session found")
                SessionManager.clearSession(context)
                navController.navigate("login") {
                    popUpTo("splash") { inclusive = true }
                }
            }
        } catch (e: Exception) {
            // Error checking session, go to login
            Log.e("SplashScreen", "Error checking session: ${e.message}")
            SessionManager.clearSession(context)
            navController.navigate("login") {
                popUpTo("splash") { inclusive = true }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF329A71)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // App Logo (you can replace with actual logo)
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = "Bookly Logo",
                modifier = Modifier.size(150.dp)
            )

            Text(
                text = "Bookly",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(16.dp))

            CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

