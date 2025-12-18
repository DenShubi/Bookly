package com.example.bookly.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.bookly.supabase.BookRequestRepository
import com.example.bookly.supabase.SupabaseClientProvider
import com.example.bookly.ui.theme.PoppinsFamily // Fixed: Changed from Poppins to PoppinsFamily
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookRequestScreen(
    navController: NavController,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { BookRequestRepository(SupabaseClientProvider.client) }
    val primaryColor = Color(0xFF329A71)

    var title by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imageUri = uri
    }

    fun submitRequest() {
        if (title.isBlank() || author.isBlank() || year.isBlank() || reason.isBlank()) {
            Toast.makeText(context, "Mohon lengkapi semua data wajib", Toast.LENGTH_SHORT).show()
            return
        }

        if (imageUri == null) {
            Toast.makeText(context, "Mohon upload foto cover buku", Toast.LENGTH_SHORT).show()
            return
        }

        scope.launch {
            isLoading = true
            try {
                val imageBytes = context.contentResolver.openInputStream(imageUri!!)?.use {
                    it.readBytes()
                }

                val result = repository.createRequest(title, author, year, reason, imageBytes)
                if (result.isSuccess) {
                    Toast.makeText(context, "Permintaan berhasil dikirim!", Toast.LENGTH_SHORT).show()
                    onBack()
                } else {
                    Toast.makeText(context, "Gagal mengirim: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Request Buku Baru",
                        fontFamily = PoppinsFamily, // Fixed usage
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Info Banner
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(primaryColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                        .border(1.dp, primaryColor, RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "💡 Ajukan permintaan buku yang belum tersedia di katalog. Admin akan meninjau dan menambahkan buku jika disetujui.",
                        fontFamily = PoppinsFamily, // Fixed usage
                        fontSize = 12.sp,
                        color = primaryColor
                    )
                }

                Text(
                    "Detail Buku",
                    fontFamily = PoppinsFamily, // Fixed usage
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                // Form Fields
                RequestInput(label = "Judul Buku", value = title, onValueChange = { title = it }, primaryColor = primaryColor)
                RequestInput(label = "Penulis", value = author, onValueChange = { author = it }, primaryColor = primaryColor)
                RequestInput(
                    label = "Tahun Terbit",
                    value = year,
                    onValueChange = { if (it.length <= 4) year = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    primaryColor = primaryColor
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Alasan Permintaan *",
                        fontFamily = PoppinsFamily, // Fixed usage
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        placeholder = { Text("Jelaskan mengapa buku ini penting...", fontFamily = PoppinsFamily, fontSize = 14.sp) }, // Fixed usage
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color(0xFFE0E0E0),
                            focusedBorderColor = primaryColor
                        )
                    )
                }

                // Image Upload
                Text(
                    "File Pendukung",
                    fontFamily = PoppinsFamily, // Fixed usage
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Foto Cover Buku *",
                        fontFamily = PoppinsFamily, // Fixed usage
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )

                    if (imageUri != null) {
                        Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                            Image(
                                painter = rememberAsyncImagePainter(imageUri),
                                contentDescription = "Cover Preview",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(10.dp)),
                                contentScale = ContentScale.Crop
                            )
                            IconButton(
                                onClick = { imageUri = null },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .size(32.dp)
                                    .background(Color.Red, RoundedCornerShape(16.dp))
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                                .background(primaryColor.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
                                .border(1.dp, primaryColor, RoundedCornerShape(10.dp))
                                .clickable { imagePickerLauncher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Image, contentDescription = null, tint = primaryColor, modifier = Modifier.size(48.dp))
                                Text("Upload Foto Cover", fontFamily = PoppinsFamily, fontWeight = FontWeight.SemiBold, color = primaryColor) // Fixed usage
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(80.dp))
            }

            // Bottom Button
            Surface(
                modifier = Modifier.align(Alignment.BottomCenter),
                shadowElevation = 8.dp
            ) {
                Box(modifier = Modifier.fillMaxWidth().background(Color.White).padding(20.dp)) {
                    Button(
                        onClick = { submitRequest() },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Text("Kirim Permintaan", fontFamily = PoppinsFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp) // Fixed usage
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RequestInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    primaryColor: Color
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "$label *",
            fontFamily = PoppinsFamily, // Fixed usage
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Color(0xFFE0E0E0),
                focusedBorderColor = primaryColor
            ),
            keyboardOptions = keyboardOptions,
            singleLine = true
        )
    }
}