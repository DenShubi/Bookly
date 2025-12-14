package com.example.bookly.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll // Import ini untuk scroll horizontal
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState // Import state scroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.bookly.viewmodel.ReviewViewModel

// --- Colors ---
val CardBackground = Color(0xFFF5F5F5)
val OwnReviewBackground = Color(0xFFE8F5E9)

@Composable
fun ReviewListScreen(
    navController: NavController,
    bookId: String,
    viewModel: ReviewViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(bookId) {
        viewModel.loadReviews(bookId)
    }

    ReviewListContent(
        reviews = state.reviews,
        currentUserId = state.currentUserId,
        isLoading = state.isLoading,
        errorMessage = state.errorMessage,
        onBackClick = { navController.navigateUp() },
        onWriteReviewClick = { navController.navigate("review_form/$bookId") },
        onRetryClick = { viewModel.loadReviews(bookId) },
        onDeleteReview = { reviewId ->
            viewModel.deleteReview(reviewId, bookId)
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewListContent(
    reviews: List<com.example.bookly.supabase.ReviewRepository.ReviewRow>,
    currentUserId: String?,
    isLoading: Boolean,
    errorMessage: String?,
    onBackClick: () -> Unit,
    onWriteReviewClick: () -> Unit,
    onRetryClick: () -> Unit,
    onDeleteReview: (String) -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var reviewIdToDelete by remember { mutableStateOf<String?>(null) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Hapus Ulasan") },
            text = { Text("Apakah Anda yakin ingin menghapus ulasan ini?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        reviewIdToDelete?.let { onDeleteReview(it) }
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) { Text("Hapus") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Batal") }
            }
        )
    }

    Scaffold(
        topBar = { ReviewListTopAppBar(onBackClick = onBackClick) },
        floatingActionButton = {
            val hasReviewed = reviews.any { it.userId == currentUserId }
            if (!hasReviewed) {
                FloatingActionButton(
                    onClick = onWriteReviewClick,
                    containerColor = Color(0xFFE0E0E0),
                    contentColor = Color.Black,
                    shape = CircleShape,
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = "Write Review", modifier = Modifier.size(28.dp))
                }
            }
        },
        containerColor = Color.White
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when {
                isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Color(0xFF2D9C6D))
                errorMessage != null -> {
                    Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = errorMessage, color = Color.Red)
                        Button(onClick = onRetryClick) { Text("Coba Lagi") }
                    }
                }
                reviews.isEmpty() -> {
                    Text(text = "Belum ada ulasan. Jadilah yang pertama!", modifier = Modifier.align(Alignment.Center), color = Color.Gray)
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(reviews) { review ->
                            val isOwnReview = review.userId == currentUserId
                            ReviewItemCard(
                                name = review.users?.fullName ?: "Tanpa Nama",
                                rating = review.rating,
                                review = review.reviewText ?: "",
                                photoUrls = review.photoUrls,
                                isOwnReview = isOwnReview,
                                onLongClick = {
                                    if (isOwnReview) {
                                        reviewIdToDelete = review.id
                                        showDeleteDialog = true
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- Komponen Pendukung ---

@Composable
fun ReviewListTopAppBar(onBackClick: () -> Unit) {
    Column(modifier = Modifier.background(Color.White)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) { Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.Black) }
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Review & Rating", color = Color(0xFF2D9C6D), fontSize = 28.sp, fontWeight = FontWeight.Bold)
        }
        Divider(color = Color.LightGray.copy(alpha = 0.5f), thickness = 1.dp, modifier = Modifier.shadow(1.dp))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReviewItemCard(
    name: String,
    rating: Int,
    review: String,
    photoUrls: List<String> = emptyList(),
    isOwnReview: Boolean = false,
    onLongClick: () -> Unit = {}
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (isOwnReview) OwnReviewBackground else CardBackground),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = {},
                onLongClick = { if (isOwnReview) onLongClick() }
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Bintang
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row {
                    repeat(5) { index ->
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = if (index < rating) Color(0xFFFFC107) else Color.Gray,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                }
                if (isOwnReview) {
                    Text(text = "(Tekan tahan untuk hapus)", fontSize = 10.sp, color = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (review.isNotEmpty()) {
                Text(text = review, color = Color.Black, fontSize = 14.sp, lineHeight = 20.sp)
            }

            // --- BAGIAN FOTO (DIPERBAIKI) ---
            if (photoUrls.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                // Ganti LazyRow dengan Row + horizontalScroll untuk mengatasi bug gambar hilang
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()), // Scrollable
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    photoUrls.forEach { url ->
                        // Placeholder Gambar dengan Rasio 4:3
                        Box(
                            modifier = Modifier
                                .height(100.dp)          // Tinggi Tetap
                                .aspectRatio(4f / 3f)    // Rasio 4:3 (Lebar : Tinggi) -> Hasilnya Lebar ~133dp
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.LightGray)
                        ) {
                            AsyncImage(
                                model = url,
                                contentDescription = "Review Image",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }
            // --------------------------------

            Spacer(modifier = Modifier.height(16.dp))

            // User Info
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFFD3D3D3)))
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = if (isOwnReview) "$name (Kamu)" else name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.Black
                )
            }
        }
    }
}