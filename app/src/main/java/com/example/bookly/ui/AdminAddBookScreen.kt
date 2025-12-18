package com.example.bookly.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.bookly.viewmodel.AdminAddBookViewModel
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAddBookScreen(
    navController: NavController,
    viewModel: AdminAddBookViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Navigate back on successful save
    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            navController.previousBackStackEntry
                ?.savedStateHandle
                ?.set("BOOKS_UPDATED", true)

            navController.navigateUp()
        }
    }

    // Error dialog
    if (uiState.errorMessage != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text("Error") },
            text = { Text(uiState.errorMessage ?: "") },
            confirmButton = {
                TextButton(onClick = { viewModel.clearError() }) {
                    Text("OK")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Tambah Buku Baru",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color.Black,
                    navigationIconContentColor = Color.Black
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Title Field
                InputField(
                    label = "Judul Buku *",
                    value = uiState.title,
                    onValueChange = { viewModel.updateTitle(it) },
                    placeholder = "Masukkan judul buku",
                    leadingIcon = Icons.Default.Book
                )

                // Author Field
                InputField(
                    label = "Penulis *",
                    value = uiState.author,
                    onValueChange = { viewModel.updateAuthor(it) },
                    placeholder = "Masukkan nama penulis",
                    leadingIcon = Icons.Default.Person
                )

                // Category Field
                DropdownField(
                    label = "Kategori",
                    value = uiState.category,
                    options = viewModel.categoryOptions.map { it.name },
                    onValueChange = { viewModel.updateCategory(it) },
                    leadingIcon = Icons.Default.LocalOffer
                )

                // Image Upload Field
                ImageUploadField(
                    label = "Gambar Buku",
                    imageUri = uiState.imageUri,
                    onImageSelected = { uri, bytes ->
                        viewModel.updateImage(uri, bytes)
                    }
                )

                // Publisher Field
                InputField(
                    label = "Penerbit *",
                    value = uiState.publisher,
                    onValueChange = { viewModel.updatePublisher(it) },
                    placeholder = "Masukkan nama penerbit",
                    leadingIcon = Icons.Default.Description
                )

                // Year Field
                InputField(
                    label = "Tahun Terbit",
                    value = uiState.year.toString(),
                    onValueChange = { viewModel.updateYear(it.toIntOrNull() ?: 0) },
                    placeholder = "2024",
                    leadingIcon = Icons.Default.CalendarToday,
                    keyboardType = KeyboardType.Number
                )

                // Language Field
                DropdownField(
                    label = "Bahasa",
                    value = uiState.language,
                    options = listOf("Indonesia", "English", "Lainnya"),
                    onValueChange = { viewModel.updateLanguage(it) },
                    leadingIcon = Icons.Default.Language
                )

                // Total Copies Field
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    InputField(
                        label = "Total Buku *",
                        value = uiState.totalCopies.toString(),
                        onValueChange = { viewModel.updateTotalCopies(it.toIntOrNull() ?: 0) },
                        placeholder = "10",
                        leadingIcon = Icons.Default.Inventory,
                        keyboardType = KeyboardType.Number
                    )
                    Text(
                        text = "Jumlah buku tersedia akan sama dengan total buku",
                        fontSize = 12.sp,
                        color = Color(0xFF828282)
                    )
                }

                // Description Field
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Deskripsi",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black
                    )
                    OutlinedTextField(
                        value = uiState.description,
                        onValueChange = { viewModel.updateDescription(it) },
                        placeholder = {
                            Text(
                                "Masukkan deskripsi buku",
                                color = Color(0xFFBDBDBD)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF329A71),
                            unfocusedBorderColor = Color(0xFFE0E0E0),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        textStyle = LocalTextStyle.current.copy(
                            fontSize = 14.sp,
                            color = Color.Black
                        )
                    )
                }

                // Submit Button
                Button(
                    onClick = { viewModel.saveBook() },
                    enabled = !uiState.isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 4.dp,
                            shape = RoundedCornerShape(12.dp)
                        ),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF329A71),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            "Simpan Buku",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text
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
                    placeholder,
                    color = Color(0xFFBDBDBD)
                )
            },
            leadingIcon = {
                Icon(
                    leadingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = Color(0xFF828282)
                )
            },
            modifier = Modifier
                .fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF329A71),
                unfocusedBorderColor = Color(0xFFE0E0E0),
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp),
            textStyle = LocalTextStyle.current.copy(
                fontSize = 14.sp,
                color = Color.Black
            ),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType)
        )
    }
}

@Composable
fun DropdownField(
    label: String,
    value: String,
    options: List<String>,
    onValueChange: (String) -> Unit,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.Black
        )
        // 1. Wrap in a Box with clickable
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = {},
                // 2. Set enabled to false so clicks pass through to the Box
                enabled = false,
                readOnly = true,
                leadingIcon = {
                    Icon(
                        leadingIcon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = Color(0xFF828282)
                    )
                },
                trailingIcon = {
                    Icon(
                        if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                        contentDescription = "Expand",
                        tint = Color.Black
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                // 3. Override disabled colors to make it look normal
                colors = OutlinedTextFieldDefaults.colors(
                    disabledBorderColor = Color(0xFFE0E0E0),
                    disabledTextColor = Color.Black,
                    disabledPlaceholderColor = Color(0xFFBDBDBD),
                    disabledLeadingIconColor = Color(0xFF828282),
                    disabledTrailingIconColor = Color.Black,
                    disabledContainerColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                textStyle = LocalTextStyle.current.copy(
                    fontSize = 14.sp,
                    color = Color.Black
                ),
                singleLine = true
            )

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                // Use fillMaxWidth(0.9f) or similar to match the field width
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onValueChange(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ImageUploadField(
    label: String,
    imageUri: Uri?,
    onImageSelected: (Uri?, ByteArray?) -> Unit
) {
    val context = LocalContext.current
    var showError by remember { mutableStateOf<String?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            // Read image as bytes
            val bytes = context.contentResolver
                .openInputStream(uri)
                ?.use { stream -> stream.readBytes() }

            if (bytes == null) {
                showError = "Gagal membaca file gambar"
                return@rememberLauncherForActivityResult
            }

            // Check file size (max 5MB)
            if (bytes.size > 5 * 1024 * 1024) {
                showError = "Ukuran file maksimal 5MB!"
                return@rememberLauncherForActivityResult
            }

            // Store URI for preview and bytes for upload
            onImageSelected(uri, bytes)
            showError = null
        } catch (e: Exception) {
            showError = "Error: ${e.message}"
            onImageSelected(null, null)
        }
    }

    // Error dialog
    if (showError != null) {
        AlertDialog(
            onDismissRequest = { showError = null },
            title = { Text("Error") },
            text = { Text(showError!!) },
            confirmButton = {
                TextButton(onClick = { showError = null }) {
                    Text("OK")
                }
            }
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.Black
        )

        // Image Preview
        if (imageUri != null) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = imageUri,
                    contentDescription = "Preview",
                    modifier = Modifier
                        .width(128.dp)
                        .height(180.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .shadow(4.dp, RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Crop
                )
            }
        }

        // Upload Button
        Button(
            onClick = { imagePickerLauncher.launch("image/*") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFF329A71)),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF329A71).copy(alpha = 0.1f),
                contentColor = Color(0xFF329A71)
            )
        ) {
            Icon(Icons.Default.Upload, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(if (imageUri != null) "Ubah Gambar" else "Upload Gambar")
        }
    }
}


