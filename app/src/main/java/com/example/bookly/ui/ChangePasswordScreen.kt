package com.example.bookly.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.bookly.supabase.UserRepository
import kotlinx.coroutines.launch

// Local colors for this screen to avoid conflicts
private val ChangePasswordGreen = Color(0xFF329a71)
private val ChangePasswordGreyText = Color(0xFF828282)
private val BorderGrey = Color(0xFFE0E0E0)
private val PlaceholderGrey = Color(0xFFBDBDBD)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(
    navController: NavController
) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showCurrentPassword by remember { mutableStateOf(false) }
    var showNewPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ubah Kata Sandi") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color.Black
                )
            )
        },
        bottomBar = { BottomNavigationBar(navController = navController, selected = "profile") },
        containerColor = Color.White,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 24.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Instruction Text
            Text(
                text = "Untuk keamanan akun Anda, pastikan kata sandi baru memiliki minimal 6 karakter.",
                fontSize = 14.sp,
                color = ChangePasswordGreyText,
                lineHeight = 20.sp
            )

            // Password Form
            Column(
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Current Password Field
                PasswordInputField(
                    label = "Kata Sandi Saat Ini",
                    value = currentPassword,
                    onValueChange = { currentPassword = it },
                    placeholder = "Masukkan kata sandi saat ini",
                    showPassword = showCurrentPassword,
                    onTogglePasswordVisibility = { showCurrentPassword = !showCurrentPassword }
                )

                // New Password Field
                PasswordInputField(
                    label = "Kata Sandi Baru",
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    placeholder = "Masukkan kata sandi baru",
                    showPassword = showNewPassword,
                    onTogglePasswordVisibility = { showNewPassword = !showNewPassword }
                )

                // Confirm Password Field
                PasswordInputField(
                    label = "Konfirmasi Kata Sandi Baru",
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    placeholder = "Konfirmasi kata sandi baru",
                    showPassword = showConfirmPassword,
                    onTogglePasswordVisibility = { showConfirmPassword = !showConfirmPassword }
                )

                // Submit Button
                Button(
                    onClick = {
                        coroutineScope.launch {
                            // Validation
                            when {
                                currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty() -> {
                                    snackbarHostState.showSnackbar("Semua field harus diisi!")
                                }
                                newPassword != confirmPassword -> {
                                    snackbarHostState.showSnackbar("Kata sandi baru dan konfirmasi tidak cocok!")
                                }
                                newPassword.length < 6 -> {
                                    snackbarHostState.showSnackbar("Kata sandi baru harus minimal 6 karakter!")
                                }
                                else -> {
                                    isLoading = true
                                    val result = UserRepository.changePassword(newPassword)
                                    isLoading = false

                                    result.onSuccess {
                                        snackbarHostState.showSnackbar("Kata sandi berhasil diubah!")
                                        navController.popBackStack()
                                    }.onFailure { error ->
                                        snackbarHostState.showSnackbar(
                                            error.message ?: "Gagal mengubah kata sandi"
                                        )
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ChangePasswordGreen,
                        contentColor = Color.White
                    ),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Simpan Perubahan",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PasswordInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    showPassword: Boolean,
    onTogglePasswordVisibility: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.Black
        )

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    text = placeholder,
                    color = PlaceholderGrey,
                    fontSize = 14.sp
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = ChangePasswordGreyText,
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = {
                IconButton(onClick = onTogglePasswordVisibility) {
                    Icon(
                        imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (showPassword) "Hide password" else "Show password",
                        tint = ChangePasswordGreyText,
                        modifier = Modifier.size(20.dp)
                    )
                }
            },
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ChangePasswordGreen,
                unfocusedBorderColor = BorderGrey,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black,
                cursorColor = ChangePasswordGreen
            ),
            singleLine = true
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ChangePasswordScreenPreview() {
    ChangePasswordScreen(navController = rememberNavController())
}

