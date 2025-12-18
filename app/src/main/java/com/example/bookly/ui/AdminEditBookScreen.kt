package com.example.bookly.ui

import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.bookly.viewmodel.AdminEditBookViewModel
import androidx.compose.material.icons.filled.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminEditBookScreen(
    navController: NavController,
    bookId: String,
    viewModel: AdminEditBookViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Load book data on first composition
    LaunchedEffect(bookId) {
        viewModel.loadBook(bookId)
    }

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

    // Loading state while fetching book data
    if (uiState.isLoadingBook) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                color = Color(0xFF329A71)
            )
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Edit Buku",
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
                        text = "Tersedia: ${uiState.availableCopies} buku",
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
                    onClick = { viewModel.updateBook() },
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
                            "Perbarui Buku",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

