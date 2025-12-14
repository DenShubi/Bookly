package com.example.bookly.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.bookly.R
import com.example.bookly.viewmodel.ReviewViewModel

// --- Colors ---
 val PrimaryGreen = Color(0xFF2D9C6D)
 val BackgroundGray = Color(0xFFF5F5F5)

@Composable
fun ReviewScreen(
    navController: NavController,
    viewModel: ReviewViewModel = viewModel()
) {
    val context = LocalContext.current
    val navBackStackEntry = navController.currentBackStackEntry
    val bookId = navBackStackEntry?.arguments?.getString("bookId") ?: ""

    // 1. Ambil State UI dari ViewModel
    val state by viewModel.uiState.collectAsState()

    // 2. Load Info Buku saat layar dibuka
    LaunchedEffect(bookId) {
        viewModel.loadBookInfo(bookId)
    }

    // --- STATE LOKAL FORM ---
    var rating by remember { mutableIntStateOf(0) }
    var reviewText by remember { mutableStateOf("") }
    // Simpan List URI Gambar dari Galeri
    val selectedImageUris = remember { mutableStateListOf<Uri>() }

    // --- PHOTO PICKER LAUNCHER ---
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 3) // Max 3 foto
    ) { uris ->
        selectedImageUris.addAll(uris)
    }

    // Efek Samping: Jika Submit Sukses -> Kembali
    LaunchedEffect(state.isSubmitSuccess) {
        if (state.isSubmitSuccess) {
            viewModel.resetSubmitStatus()
            navController.popBackStack()
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { msg -> snackbarHostState.showSnackbar(msg) }
    }

    Scaffold(
        topBar = { ReviewTopAppBar(onBackClick = { navController.navigateUp() }) },
        bottomBar = {
            SubmitButton(
                isLoading = state.isLoading,
                onClick = {
                    // Panggil fungsi submit dengan foto di ViewModel
                    viewModel.submitReviewWithPhotos(
                        context = context,
                        bookId = bookId,
                        rating = rating,
                        reviewText = reviewText,
                        photoUris = selectedImageUris
                    )
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.White
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(20.dp))

                // 3. Tampilkan Data Buku Asli
                BookInfoCard(
                    title = state.bookTitle.ifEmpty { "Memuat..." }, // Tampilkan loading text jika belum ada
                    author = state.bookAuthor,
                    coverUrl = state.bookCoverUrl
                )

                Spacer(modifier = Modifier.height(30.dp))

                // 4. Rating Bintang (Interaktif)
                RatingSection(
                    rating = rating,
                    onRatingChanged = { newRating -> rating = newRating }
                )

                Spacer(modifier = Modifier.height(30.dp))

                // 5. Input Text
                ReviewInputSection(text = reviewText, onTextChanged = { reviewText = it })

                Spacer(modifier = Modifier.height(24.dp))

                // 6. Foto Section dengan Picker
                PhotoSection(
                    photoUris = selectedImageUris,
                    onAddClick = {
                        // Buka Galeri HP
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onRemoveClick = { uri -> selectedImageUris.remove(uri) }
                )

                Spacer(modifier = Modifier.height(100.dp))
            }

            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = PrimaryGreen)
            }
        }
    }
}

// --- Sub Komponen yang Diperbarui ---

@Composable
fun BookInfoCard(title: String, author: String, coverUrl: String?) {
    Card(
        modifier = Modifier.fillMaxWidth().shadow(elevation = 8.dp, shape = RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            // Gambar Buku Asli
            AsyncImage(
                model = coverUrl,
                contentDescription = title,
                modifier = Modifier.size(width = 60.dp, height = 80.dp).clip(RoundedCornerShape(4.dp)),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(id = R.drawable.book_cover), // Pastikan ada drawable ini
                error = painterResource(id = R.drawable.book_cover)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
                Text(text = author, fontSize = 14.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
fun PhotoSection(photoUris: List<Uri>, onAddClick: () -> Unit, onRemoveClick: (Uri) -> Unit) {
    Column {
        Text(text = "Foto (Opsional)", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(bottom = 12.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            items(photoUris) { uri ->
                Box(modifier = Modifier.size(100.dp)) {
                    // Tampilkan Gambar dari URI
                    AsyncImage(
                        model = uri,
                        contentDescription = "Review Photo",
                        modifier = Modifier
                            .padding(top = 8.dp, end = 8.dp)
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.LightGray),
                        contentScale = ContentScale.Crop
                    )
                    // Tombol Hapus
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Remove",
                        tint = Color.White,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(24.dp)
                            .background(Color(0xFFFF6F6F), CircleShape)
                            .padding(2.dp)
                            .clickable { onRemoveClick(uri) }
                    )
                }
            }
            // Tombol Tambah
            item {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFE0E0E0))
                        .clickable { onAddClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Photo", tint = Color.Black)
                }
            }
        }
    }
}

// --- Komponen Lainnya Tetap Sama (ReviewTopAppBar, SubmitButton, RatingSection, ReviewInputSection) ---
// (Pastikan kode komponen tersebut ada di bawah file ini agar tidak error)
@Composable
fun ReviewTopAppBar(onBackClick: () -> Unit) {
    Column(modifier = Modifier.background(Color.White)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.Black)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Review & Rating", color = PrimaryGreen, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        }
        Divider(color = Color.LightGray.copy(alpha = 0.5f), thickness = 1.dp, modifier = Modifier.shadow(1.dp))
    }
}

@Composable
fun SubmitButton(isLoading: Boolean, onClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().padding(16.dp).background(Color.White)) {
        Button(
            onClick = onClick,
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
            shape = RoundedCornerShape(28.dp)
        ) {
            if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            else Text("Kirim Ulasan", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun RatingSection(rating: Int, onRatingChanged: (Int) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Bagaimana Bukunya?",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = Color.Black // Pastikan pakai Color.Black atau TextBlack
        )
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            for (i in 1..5) {
                // Tentukan warna: Emas jika dipilih, Abu-abu jika tidak
                val starColor = if (i <= rating) Color(0xFFFFC107) else Color.LightGray

                // Tentukan Icon: Full Star jika dipilih, bisa pakai Full juga untuk abu-abu agar rapi
                // atau pakai Outlined jika ingin yang belum dipilih terlihat kosong
                val starIcon = Icons.Filled.Star

                Icon(
                    imageVector = starIcon,
                    contentDescription = "Star $i",
                    modifier = Modifier
                        .size(48.dp)
                        .clickable { onRatingChanged(i) }
                        .padding(4.dp),
                    tint = starColor // <--- Ini kuncinya
                )
            }
        }
    }
}

@Composable
fun ReviewInputSection(text: String, onTextChanged: (String) -> Unit) {
    Column {
        Text(text = "Tulis Ulasan Mu", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(bottom = 12.dp))
        Box(modifier = Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(12.dp)).background(BackgroundGray).padding(16.dp)) {
            BasicTextField(
                value = text, onValueChange = onTextChanged,
                textStyle = TextStyle(fontSize = 14.sp, color = Color.Black),
                modifier = Modifier.fillMaxSize(),
                decorationBox = { innerTextField -> if (text.isEmpty()) Text("Tulis pendapatmu...", color = Color.Gray); innerTextField() }
            )
        }
    }
}