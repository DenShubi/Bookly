package com.example.bookly

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.bookly.supabase.UserRepository
import com.example.bookly.supabase.supabase
import com.example.bookly.ui.BottomNavigationBar
import com.example.bookly.util.SessionManager
import com.example.bookly.viewmodel.WishlistViewModel
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    wishlistViewModel: WishlistViewModel? = null
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Profil") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color.Black
                )
            )
        },
        bottomBar = { BottomNavigationBar(navController = navController, selected = "profile") },
        containerColor = Color.White
    ) { paddingValues ->
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()

        var fullName by remember { mutableStateOf<String?>(null) }
        var email by remember { mutableStateOf<String?>(null) }
        var avatarUrl by remember { mutableStateOf<String?>(null) }

        var showLogoutDialog by remember { mutableStateOf(false) }
        var isLoading by remember { mutableStateOf(true) }
        var isUploading by remember { mutableStateOf(false) }

        // Fetch User Profile
        fun loadProfile() {
            coroutineScope.launch {
                isLoading = true
                val result = UserRepository.getUserProfile()
                result.onSuccess { profile ->
                    fullName = profile.fullName
                    email = profile.email
                    avatarUrl = profile.avatarUrl
                    isLoading = false
                }.onFailure {
                    // Fallback to auth metadata if database fetch fails
                    val user = supabase.auth.currentUserOrNull()
                    email = user?.email
                    fullName = user?.userMetadata?.get("full_name")?.toString() ?: "User"
                    isLoading = false
                }
            }
        }

        LaunchedEffect(Unit) {
            loadProfile()
        }

        // --- Image Picker Logic ---
        var showImageSourceDialog by remember { mutableStateOf(false) }
        var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

        // Helper to check permission status
        fun checkCameraPermission(): Boolean {
            return ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.CAMERA
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }

        // 1. Gallery Launcher
        val galleryLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia()
        ) { uri: Uri? ->
            uri?.let {
                isUploading = true
                uploadImage(context, it, coroutineScope) { newUrl ->
                    avatarUrl = newUrl
                    isUploading = false
                }
            }
        }

        // 2. Camera Launcher
        val cameraLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.TakePicture()
        ) { success ->
            if (success && tempCameraUri != null) {
                isUploading = true
                uploadImage(context, tempCameraUri!!, coroutineScope) { newUrl ->
                    avatarUrl = newUrl
                    isUploading = false
                }
            }
        }

        // 3. Permission Launcher for Camera
        val cameraPermissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                val uri = ComposeFileProvider.getImageUri(context)
                tempCameraUri = uri
                cameraLauncher.launch(uri)
            } else {
                Toast.makeText(context, "Izin kamera diperlukan", Toast.LENGTH_SHORT).show()
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            ProfileImageSection(
                imageUrl = avatarUrl,
                isUploading = isUploading,
                onCameraClick = { showImageSourceDialog = true }
            )

            ProfileDetailsSection(fullName = fullName, email = email)

            ActivitySection(
                onViewFines = { /* Navigate to fines */ },
                onViewHistory = { /* Navigate to history */ }
            )

            VerificationSection(
                isVerified = false, // Replace with actual verification state if available
                onKYC = { /* Navigate to KYC */ }
            )

            SettingsSection(
                onChangePasswordClick = { navController.navigate("change_password") },
                onLogoutClick = { showLogoutDialog = true }
            )

            Spacer(modifier = Modifier.height(100.dp))
        }

        // --- Dialogs ---

        if (showImageSourceDialog) {
            AlertDialog(
                onDismissRequest = { showImageSourceDialog = false },
                title = { Text("Ubah Foto Profil") },
                text = { Text("Pilih sumber gambar") },
                confirmButton = {
                    TextButton(onClick = {
                        showImageSourceDialog = false
                        if (checkCameraPermission()) {
                            val uri = ComposeFileProvider.getImageUri(context)
                            tempCameraUri = uri
                            cameraLauncher.launch(uri)
                        } else {
                            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                        }
                    }) {
                        Text("Kamera")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showImageSourceDialog = false
                        galleryLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }) {
                        Text("Galeri")
                    }
                },
                containerColor = Color.White
            )
        }

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
                                // Clear session for auto-login
                                SessionManager.clearSession(context)
                                navController.navigate("login") { popUpTo(0) }
                            }
                        }
                    ) {
                        Text("Keluar", color = Color.Red)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutDialog = false }) { Text("Batal") }
                },
                containerColor = Color.White
            )
        }
    }
}

// --- Sections ---

@Composable
fun ProfileImageSection(
    imageUrl: String?,
    isUploading: Boolean,
    onCameraClick: () -> Unit
) {
    Box(modifier = Modifier.size(140.dp)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(Color(0xFFF5F5F5))
                .border(4.dp, Color(0xFF329a71), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (isUploading) {
                CircularProgressIndicator(color = Color(0xFF329a71))
            } else if (!imageUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Profile Picture",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Placeholder",
                    tint = Color(0xFF329a71),
                    modifier = Modifier.size(70.dp)
                )
            }
        }

        // Camera Button
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(44.dp)
                .background(Color(0xFF329a71), CircleShape)
                .border(2.dp, Color.White, CircleShape)
                .clickable { onCameraClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PhotoCamera,
                contentDescription = "Ubah Foto",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun ProfileDetailsSection(fullName: String? = null, email: String? = null) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Detail Profil", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE0E0E0))
        ) {
            Column {
                InfoRow(icon = Icons.Default.Email, label = "Email", value = email ?: "-")
                HorizontalDivider(modifier = Modifier.padding(horizontal = 0.dp), thickness = 1.dp, color = Color(0xFFE0E0E0))
                InfoRow(icon = Icons.Default.Person, label = "Nama", value = fullName ?: "-")
            }
        }
    }
}

@Composable
fun ActivitySection(
    onViewFines: () -> Unit,
    onViewHistory: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Aktivitas Peminjaman", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE0E0E0))
        ) {
            Column {
                SettingsRow(
                    icon = Icons.Default.AttachMoney,
                    text = "Daftar Denda",
                    onClick = onViewFines
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 0.dp), thickness = 1.dp, color = Color(0xFFE0E0E0))
                SettingsRow(
                    icon = Icons.Default.History,
                    text = "Riwayat Peminjaman",
                    onClick = onViewHistory
                )
            }
        }
    }
}

@Composable
fun VerificationSection(
    isVerified: Boolean,
    onKYC: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Verifikasi Identitas", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE0E0E0))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onKYC() }
                    .background(if (isVerified) Color(0xFFF0FDF4) else Color.Transparent)
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isVerified) Icons.Default.VerifiedUser else Icons.Default.Security,
                    contentDescription = null,
                    tint = if (isVerified) Color(0xFF329a71) else Color(0xFF828282),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isVerified) "Identitas Terverifikasi" else "Verifikasi Identitas",
                        color = if (isVerified) Color(0xFF329a71) else Color.Black,
                        fontSize = 14.sp
                    )
                    if (isVerified) {
                        Text(
                            text = "Akun Anda telah terverifikasi",
                            color = Color(0xFF329a71),
                            fontSize = 11.sp
                        )
                    }
                }

                if (isVerified) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF329a71), RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("Verified", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = Color(0xFF828282),
                        modifier = Modifier.size(20.dp)
                    )
                }
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
        Text("Pengaturan", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE0E0E0))
        ) {
            Column {
                SettingsRow(icon = Icons.Default.Notifications, text = "Atur Notifikasi")
                HorizontalDivider(modifier = Modifier.padding(horizontal = 0.dp), thickness = 1.dp, color = Color(0xFFE0E0E0))
                SettingsRow(
                    icon = Icons.Default.Lock,
                    text = "Ubah Kata Sandi",
                    onClick = onChangePasswordClick
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 0.dp), thickness = 1.dp, color = Color(0xFFE0E0E0))
                SettingsRow(
                    icon = Icons.AutoMirrored.Filled.ExitToApp,
                    text = "Keluar",
                    iconTint = Color(0xFFCC0707),
                    textColor = Color(0xFFCC0707),
                    onClick = onLogoutClick
                )
            }
        }
    }
}

// --- Shared Helper Components ---

@Composable
fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = Color(0xFF828282), modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = label, color = Color(0xFF828282), fontSize = 14.sp)
        Spacer(modifier = Modifier.weight(1f))
        Text(text = value, fontWeight = FontWeight.SemiBold, color = Color.Black, fontSize = 14.sp)
    }
}

@Composable
fun SettingsRow(
    icon: ImageVector,
    text: String,
    iconTint: Color = Color(0xFF828282),
    textColor: Color = Color.Black,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = text, tint = iconTint, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(text, color = textColor, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = if (textColor == Color(0xFFCC0707)) Color(0xFFCC0707) else Color(0xFF828282),
            modifier = Modifier.size(20.dp)
        )
    }
}

// Helper to process upload
fun uploadImage(
    context: Context,
    uri: Uri,
    scope: kotlinx.coroutines.CoroutineScope,
    onSuccess: (String) -> Unit
) {
    scope.launch {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val bytes = inputStream?.readBytes()
            inputStream?.close()

            if (bytes != null) {
                Toast.makeText(context, "Mengunggah...", Toast.LENGTH_SHORT).show()
                val result = UserRepository.uploadProfilePicture(bytes)
                result.onSuccess { url ->
                    onSuccess(url)
                    Toast.makeText(context, "Foto berhasil diperbarui", Toast.LENGTH_SHORT).show()
                }.onFailure {
                    Toast.makeText(context, "Gagal mengunggah: ${it.message}", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}

// Helper class for Camera URI
object ComposeFileProvider {
    fun getImageUri(context: Context): Uri {
        val directory = File(context.cacheDir, "images")
        directory.mkdirs()
        val file = File.createTempFile(
            "selected_image_",
            ".jpg",
            directory
        )
        val authority = context.packageName + ".provider"
        return FileProvider.getUriForFile(
            context,
            authority,
            file
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
    ProfileScreen(navController = rememberNavController())
}