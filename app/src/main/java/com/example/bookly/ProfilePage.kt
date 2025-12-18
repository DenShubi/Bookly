package com.example.bookly

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import com.example.bookly.supabase.UserRepository
import com.example.bookly.supabase.supabase
import com.example.bookly.ui.BottomNavigationBar
import com.example.bookly.viewmodel.WishlistViewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    wishlistViewModel: WishlistViewModel? = null
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Profile") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        bottomBar = { BottomNavigationBar(navController = navController, selected = "profile") }
    ) { paddingValues ->
        val coroutineScope = rememberCoroutineScope()
        var fullName by remember { mutableStateOf<String?>(null) }
        var email by remember { mutableStateOf<String?>(null) }
        var showLogoutDialog by remember { mutableStateOf(false) }
        var isLoading by remember { mutableStateOf(true) }

        LaunchedEffect(Unit) {
            // Fetch user profile from database
            val result = UserRepository.getUserProfile()
            result.onSuccess { profile ->
                fullName = profile.fullName
                email = profile.email
                isLoading = false
            }.onFailure {
                // Fallback to auth metadata if database fetch fails
                val user = supabase.auth.currentUserOrNull()
                email = user?.email
                fullName = user?.userMetadata?.get("full_name")
                isLoading = false
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ProfileImageSection()
            Spacer(modifier = Modifier.height(32.dp))
            ProfileDetailsSection(fullName = fullName, email = email)
            Spacer(modifier = Modifier.height(32.dp))
            SettingsSection(
                onChangePasswordClick = { navController.navigate("change_password") },
                onLogoutClick = { showLogoutDialog = true }
            )
        }

        // Logout Confirmation Dialog
        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                title = { Text("Konfirmasi Logout") },
                text = { Text("Apakah Anda yakin ingin keluar?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showLogoutDialog = false
                            coroutineScope.launch {
                                UserRepository.signOut()
                                wishlistViewModel?.clearWishlist()
                                navController.navigate("login") {
                                    popUpTo(0)
                                }
                            }
                        }
                    ) {
                        Text("Keluar", color = Color.Red)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutDialog = false }) {
                        Text("Batal")
                    }
                }
            )
        }
    }
}

@Composable
fun ProfileImageSection() {
    Box(modifier = Modifier.size(140.dp)) {
        Image(
            painter = painterResource(id = R.drawable.herp),
            contentDescription = "Profile Picture",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .border(2.dp, Color(0xFF2E8B57), CircleShape)
        )
        Icon(
            imageVector = Icons.Default.PhotoCamera,
            contentDescription = "Ubah Foto",
            tint = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp)
                .background(Color(0xFF2E8B57), CircleShape)
                .padding(8.dp)
                .size(24.dp)
        )
    }
}

@Composable
fun ProfileDetailsSection(fullName: String? = null, email: String? = null) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Detail Profil", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, Color.LightGray)
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                InfoRow(icon = Icons.Default.Email, label = "Email", value = email ?: "-")
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                InfoRow(icon = Icons.Default.Person, label = "Nama", value = fullName ?: "-")
            }
        }
    }
}

@Composable
fun SettingsSection(
    onChangePasswordClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Pengaturan", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, Color.LightGray)
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                SettingsRow(icon = Icons.Default.Notifications, text = "Atur Notifikasi")
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                SettingsRow(
                    icon = Icons.Default.MoreHoriz,
                    text = "Ubah Kata Sandi",
                    onClick = onChangePasswordClick
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                SettingsRow(
                    icon = Icons.AutoMirrored.Filled.ExitToApp,
                    text = "Keluar",
                    iconTint = Color.Red,
                    textColor = Color.Red,
                    onClick = onLogoutClick
                )
            }
        }
    }
}

@Composable
fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.weight(1f))
        Text(text = value, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun SettingsRow(
    icon: ImageVector,
    text: String,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = text, tint = iconTint)
        Spacer(modifier = Modifier.width(16.dp))
        Text(text, color = textColor)
        Spacer(modifier = Modifier.weight(1f))
        Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
    ProfileScreen(navController = rememberNavController())
}