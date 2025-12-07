package com.example.bookly

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import com.example.bookly.supabase.supabase
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

@Composable
fun RegisterScreen(navController: NavController) {
    // Renamed variables to avoid shadowing inside the builder lambda
    var emailInput by remember { mutableStateOf("") }
    var usernameInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var confirmPasswordInput by remember { mutableStateOf("") }

    var passwordVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(modifier = Modifier.height(64.dp))

        Text(
            text = stringResource(id = R.string.register_en),
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2E8B57)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(id = R.string.user_regist),
            fontSize = 18.sp,
            modifier = Modifier.padding(bottom = 32.dp)
        )

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
            value = usernameInput,
            onValueChange = { usernameInput = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(id = R.string.username)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
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
                val image = if (passwordVisible)
                    painterResource(id = R.drawable.baseline_visibility_off_24)
                else
                    painterResource(id = R.drawable.baseline_visibility_off_24)

                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(painter = image, contentDescription = "Toggle password visibility")
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = confirmPasswordInput,
            onValueChange = { confirmPasswordInput = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(id = R.string.password_confirm)) },
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                val image = if (passwordVisible)
                    painterResource(id = R.drawable.baseline_visibility_off_24)
                else
                    painterResource(id = R.drawable.baseline_visibility_off_24)

                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(painter = image, contentDescription = "Toggle password visibility")
                }
            }
        )

        Spacer(modifier = Modifier.height(32.dp))

        val coroutineScope = rememberCoroutineScope()
        var errorMessage by remember { mutableStateOf<String?>(null) }

        Button(
            onClick = {
                coroutineScope.launch {
                    // Simple password confirmation check
                    if (passwordInput != confirmPasswordInput) {
                        errorMessage = "Passwords do not match"
                        return@launch
                    }
                    // Use supabase shim auth.signUpWith
                    val result = supabase.auth.signUpWith {
                        // Explicit assignment: builder.email = localState.emailInput
                        email = emailInput
                        password = passwordInput
                        data = mapOf("username" to usernameInput)
                    }

                    if (result.user != null) {
                        navController.navigate("profile") {
                            popUpTo("register") { inclusive = true }
                        }
                    } else {
                        errorMessage = "Registration failed"
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E8B57))
        ) {
            Text(
                text = stringResource(id = R.string.sign_up),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (errorMessage != null) {
            AlertDialog(
                onDismissRequest = { errorMessage = null },
                confirmButton = {
                    TextButton(onClick = { errorMessage = null }) { Text("OK") }
                },
                title = { Text("Register error") },
                text = { Text(errorMessage ?: "Unknown error") }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = {
            navController.navigate("login")
        }) {
            Text(
                text = "Sudah punya akun? " + stringResource(id = R.string.register),
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

@Preview(showBackground = true)
@Composable
fun RegisterScreenPreview() {
    RegisterScreen(navController = rememberNavController())
}