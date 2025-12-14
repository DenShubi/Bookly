package com.example.bookly.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.bookly.viewmodel.ReviewViewModel

// --- Colors ---
val CardBackground = Color(0xFFF5F5F5)

@Composable
fun ReviewListScreen(
    navController: NavController,
    bookId: String,
    viewModel: ReviewViewModel = viewModel() // Inject ViewModel
) {
    // 1. Ambil state dari ViewModel
    val state by viewModel.uiState.collectAsState()

    // 2. Load data saat pertama kali dibuka
    LaunchedEffect(bookId) {
        viewModel.loadReviews(bookId)
    }

    ReviewListContent(
        reviews = state.reviews, // Kirim data asli ke UI
        isLoading = state.isLoading,
        errorMessage = state.errorMessage,
        onBackClick = { navController.navigateUp() },
        onWriteReviewClick = { navController.navigate("review_form/$bookId") },
        onRetryClick = { viewModel.loadReviews(bookId) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewListContent(
    reviews: List<com.example.bookly.supabase.ReviewRepository.ReviewRow>,
    isLoading: Boolean,
    errorMessage: String?,
    onBackClick: () -> Unit,
    onWriteReviewClick: () -> Unit,
    onRetryClick: () -> Unit
) {
    Scaffold(
        topBar = {
            ReviewListTopAppBar(onBackClick = onBackClick)
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onWriteReviewClick,
                containerColor = Color(0xFFE0E0E0),
                contentColor = Color.Black,
                shape = CircleShape,
                modifier = Modifier.size(64.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Write Review",
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        containerColor = Color.White
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                // Loading State
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = PrimaryGreen
                    )
                }

                // Error State
                errorMessage != null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = errorMessage, color = Color.Red)
                        Button(onClick = onRetryClick) {
                            Text("Coba Lagi")
                        }
                    }
                }

                // Empty State
                reviews.isEmpty() -> {
                    Text(
                        text = "Belum ada ulasan. Jadilah yang pertama!",
                        modifier = Modifier.align(Alignment.Center),
                        color = Color.Gray
                    )
                }

                // Success State (List Data)
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(reviews) { review ->
                            ReviewItemCard(
                                name = review.users?.fullName ?: "Tanpa Nama",
                                rating = review.rating,
                                review = review.reviewText ?: ""
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.Black
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Review & Rating",
                color = PrimaryGreen,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Divider(
            color = Color.LightGray.copy(alpha = 0.5f),
            thickness = 1.dp,
            modifier = Modifier.shadow(1.dp)
        )
    }
}

@Composable
fun ReviewItemCard(name: String, rating: Int, review: String) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Bintang
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

            Spacer(modifier = Modifier.height(12.dp))

            // Teks Review
            if (review.isNotEmpty()) {
                Text(
                    text = review,
                    color = Color.Black,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // User Profile
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFD3D3D3))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.Black
                )
            }
        }
    }
}