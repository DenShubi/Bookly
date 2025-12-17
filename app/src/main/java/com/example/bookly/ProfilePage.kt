package com.example.bookly
import androidx.navigation.compose.rememberNavController
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.bookly.supabase.UserRepository
import com.example.bookly.supabase.supabase // Pastikan ini mengarah ke client supabase anda
import com.example.bookly.ui.BottomNavigationBar
import com.example.bookly.viewmodel.WishlistViewModel
import kotlinx.coroutines.launch
import androidx.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    wishlistViewModel: WishlistViewModel? = null
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // States
    var fullName by remember { mutableStateOf<String?>(null) }
    var email by remember { mutableStateOf<String?>(null) }
    var profileUrl by remember { mutableStateOf<String?>(null) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var isUploading by remember { mutableStateOf(false) }

    // Logic Pilih Gambar
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            coroutineScope.launch {
                isUploading = true
                // 1. & 2. Pilih & Upload ke Supabase
                val result = UserRepository.uploadProfilePicture(it, context)
                result.onSuccess { url ->
                    // 3. Simpan URL ke Database (logic ini sebaiknya di UserRepository)
                    profileUrl = url
                }
                isUploading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        val result = UserRepository.getUserProfile()
        result.onSuccess { profile ->
            fullName = profile.fullName
            email = profile.email
            profileUrl = profile.avatarUrl // Ambil URL dari DB
        }.onFailure {
            val user = supabase.auth.currentUserOrNull()
            email = user?.email
            fullName = user?.userMetadata?.get("full_name")?.toString()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Edit Profil", fontWeight = FontWeight.Bold, color = Color(0xFF2E8B57)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        },
        bottomBar = { BottomNavigationBar(navController = navController, selected = "profile") }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Section Foto Profil
            ProfileImageSection(
                imageUrl = profileUrl,
                isUploading = isUploading,
                onAddImageClick = { launcher.launch("image/*") }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 1. Detail Profil
            SectionTitle("Detail Profil")
            CardContainer {
                InfoRow(icon = Icons.Default.Email, label = "Email", value = email ?: "pk@mail.com")
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                InfoRow(icon = Icons.Default.Person, label = "Nama", value = fullName ?: "Pascal Love Khansa")
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 2. Aktivitas Peminjaman
            SectionTitle("Aktivitas Peminjaman")
            CardContainer {
                SettingsRow(
                    icon = Icons.Default.AttachMoney,
                    text = "Daftar Denda",
                    onClick = { navController.navigate("daftar_denda") } // Navigasi ke Daftar Denda
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                SettingsRow(icon = Icons.Default.History, text = "Riwayat Peminjaman")
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 3. Verifikasi Identitas
            SectionTitle("Verifikasi Identitas")
            CardContainer {
                SettingsRow(icon = Icons.Default.VerifiedUser, text = "Verifikasi Identitas")
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 4. Pengaturan
            SectionTitle("Pengaturan")
            CardContainer {
                SettingsRow(icon = Icons.Default.Lock, text = "Ubah Kata Sandi", onClick = { navController.navigate("change_password") })
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                SettingsRow(
                    icon = Icons.AutoMirrored.Filled.ExitToApp,
                    text = "Keluar",
                    iconTint = Color.Red,
                    textColor = Color.Red,
                    onClick = { showLogoutDialog = true }
                )
            }
        }

        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                title = { Text("Konfirmasi Logout") },
                text = { Text("Apakah Anda yakin ingin keluar?") },
                confirmButton = {
                    TextButton(onClick = {
                        coroutineScope.launch {
                            UserRepository.signOut()
                            navController.navigate("login") { popUpTo(0) }
                        }
                    }) { Text("Keluar", color = Color.Red) }
                },
                dismissButton = { TextButton(onClick = { showLogoutDialog = false }) { Text("Batal") } }
            )
        }
    }
}

@Composable
fun ProfileImageSection(imageUrl: String?, isUploading: Boolean, onAddImageClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(120.dp)
            .clickable { if (!isUploading) onAddImageClick() },
        contentAlignment = Alignment.BottomEnd
    ) {
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = "Profile Picture",
                modifier = Modifier.fillMaxSize().clip(CircleShape).border(2.dp, Color(0xFF2E8B57), CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize().clip(CircleShape).border(2.dp, Color(0xFF2E8B57), CircleShape).background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(60.dp), tint = Color.LightGray)
            }
        }

        if (isUploading) {
            CircularProgressIndicator(modifier = Modifier.matchParentSize(), color = Color(0xFF2E8B57))
        }

        Box(
            modifier = Modifier.size(32.dp).background(Color(0xFF2E8B57), CircleShape).padding(6.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
    )
}

@Composable
fun CardContainer(content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFEEEEEE)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(content = content)
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
        Icon(imageVector = icon, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = label, color = Color.Gray, fontSize = 14.sp)
        Spacer(modifier = Modifier.weight(1f))
        Text(text = value, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Black)
    }
}

@Composable
fun SettingsRow(
    icon: ImageVector,
    text: String,
    iconTint: Color = Color.Gray,
    textColor: Color = Color.Black,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = text, color = textColor, fontSize = 14.sp)
        Spacer(modifier = Modifier.weight(1f))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = Color.LightGray
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ProfileScreenPreview() {
    // NavController dummy agar tidak error saat preview
    val navController = rememberNavController()

    MaterialTheme {
        // Mocking data untuk preview agar terlihat seperti di gambar
        Surface(color = Color(0xFFF8F8F8)) {
            ProfileScreen(navController = navController)
        }
    }
}

// InfoRow dan SettingsRow tetap sama dengan kode Anda sebelumnya,
// hanya sedikit penyesuaian padding agar mirip UI desain.