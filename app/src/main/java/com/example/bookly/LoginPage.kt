package com.example.bookly

import android.util.Log
import android.util.Patterns
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.bookly.supabase.SupabaseClientProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(navController: NavController) {
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    val isFormValid by derivedStateOf {
        emailInput.isNotBlank() && passwordInput.isNotBlank()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(64.dp))
        Text(text = stringResource(id = R.string.login), fontSize = 34.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E8B57))
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = stringResource(id = R.string.welcome_back), fontSize = 18.sp, modifier = Modifier.padding(bottom = 32.dp))

        OutlinedTextField(
            value = emailInput,
            onValueChange = { emailInput = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(id = R.string.email)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = passwordInput,
            onValueChange = { passwordInput = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(id = R.string.password)) },
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                val image = if (passwordVisible) painterResource(id = R.drawable.baseline_visibility_off_24)
                else painterResource(id = R.drawable.baseline_visibility_off_24)
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(painter = image, contentDescription = "Toggle password visibility")
                }
            }
        )

        TextButton(onClick = { /* TODO */ }, modifier = Modifier.align(Alignment.End)) {
            Text(text = stringResource(id = R.string.forgot_password))
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                if (!Patterns.EMAIL_ADDRESS.matcher(emailInput).matches()) {
                    errorMessage = "Format email tidak valid"
                    return@Button
                }
                if (passwordInput.length < 6) {
                    errorMessage = "Password minimal 6 karakter"
                    return@Button
                }

                isLoading = true
                coroutineScope.launch {
                    try {
                        val auth = SupabaseClientProvider.client.auth

                        // 1. Login
                        auth.signInWith(Email) {
                            email = emailInput
                            password = passwordInput
                        }

                        val session = auth.currentSessionOrNull()
                        if (session != null) {
                            // 2. Simpan Token Manual (untuk Wishlist/Profile)
                            SupabaseClientProvider.currentAccessToken = session.accessToken

                            Log.d("Login", "Sukses! Token disimpan manual.")

                            // 3. NAVIGASI KE HOME (BUKAN KATALOG LAGI)
                            navController.navigate("home") {
                                popUpTo("login") { inclusive = true }
                            }
                        } else {
                            errorMessage = "Login gagal, sesi tidak ditemukan"
                        }
                    } catch (e: Exception) {
                        Log.e("LoginError", e.message.toString())
                        errorMessage = if (e.message?.contains("Invalid login credentials") == true) {
                            "Email atau password salah"
                        } else {
                            "Login gagal: ${e.message}"
                        }
                    } finally {
                        isLoading = false
                    }
                }
            },
            enabled = isFormValid && !isLoading,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E8B57))
        ) {
            if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            else Text(text = stringResource(id = R.string.sign_in), fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        if (errorMessage != null) {
            AlertDialog(
                onDismissRequest = { errorMessage = null },
                confirmButton = { TextButton(onClick = { errorMessage = null }) { Text("OK") } },
                title = { Text("Login Gagal") },
                text = { Text(errorMessage ?: "Terjadi kesalahan") }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = { navController.navigate("register") }) {
            Text(text = stringResource(id = R.string.register), fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.weight(1f))
    }
}