package com.example.bookly.ui

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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController

// --- Colors ---
val PrimaryGreen = Color(0xFF2D9C6D)
val BackgroundGray = Color(0xFFF5F5F5)
val TextBlack = Color.Black
val TextGray = Color(0xFF666666)

// --- Data Class Sederhana untuk UI ---
data class BookReviewData(
    val title: String,
    val author: String,
    val coverUrl: String? = null
)

// ==========================================
// 1. STATEFUL COMPOSABLE (Logic Wrapper)
// ==========================================
@Composable
fun ReviewScreen(
    navController: NavController
) {
    // --- State Hoisting ---
    var rating by remember { mutableIntStateOf(0) }
    var reviewText by remember { mutableStateOf("") }
    val photoList = remember { mutableStateListOf("123.jpg", "1234.jpg") }

    // --- Dummy Data ---
    val bookData = remember {
        BookReviewData("Laskar Pelangi", "Andrea Hirata")
    }

    // Panggil UI Murni
    ReviewContent(
        navController = navController,
        bookData = bookData,
        rating = rating,
        reviewText = reviewText,
        photoList = photoList,
        onRatingChanged = { rating = it },
        onReviewTextChanged = { reviewText = it },
        onAddPhotoClick = { /* Logic tambah foto */ },
        onRemovePhotoClick = { photo -> photoList.remove(photo) },
        onSubmitClick = { navController.popBackStack() }
    )
}

// ==========================================
// 2. STATELESS COMPOSABLE (Pure UI)
// ==========================================
@Composable
fun ReviewContent(
    navController: NavController,
    bookData: BookReviewData,
    rating: Int,
    reviewText: String,
    photoList: List<String>,
    onRatingChanged: (Int) -> Unit,
    onReviewTextChanged: (String) -> Unit,
    onAddPhotoClick: () -> Unit,
    onRemovePhotoClick: (String) -> Unit,
    onSubmitClick: () -> Unit
) {
    Scaffold(
        topBar = { ReviewTopAppBar(onBackClick = { navController.navigateUp() }) },
        bottomBar = { SubmitButton(onClick = onSubmitClick) },
        containerColor = Color.White
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // 1. Info Buku
            BookInfoCard(title = bookData.title, author = bookData.author)

            Spacer(modifier = Modifier.height(30.dp))

            // 2. Rating Bintang
            RatingSection(rating = rating, onRatingChanged = onRatingChanged)

            Spacer(modifier = Modifier.height(30.dp))

            // 3. Input Text
            ReviewInputSection(text = reviewText, onTextChanged = onReviewTextChanged)

            Spacer(modifier = Modifier.height(24.dp))

            // 4. Foto Section
            PhotoSection(
                photoList = photoList,
                onAddClick = onAddPhotoClick,
                onRemoveClick = onRemovePhotoClick
            )

            // Spacer bawah agar konten tidak tertutup tombol submit
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

// ==========================================
// 3. SUB-COMPONENTS (Modular)
// ==========================================

// --- PERBAIKAN DI SINI ---
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
fun SubmitButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(Color.White)
    ) {
        Button(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
            shape = RoundedCornerShape(28.dp)
        ) {
            Text("Kirim Ulasan", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun BookInfoCard(title: String, author: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(width = 60.dp, height = 80.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Gray)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = TextBlack
                )
                Text(
                    text = author,
                    fontSize = 14.sp,
                    color = TextGray
                )
            }
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
            color = TextBlack
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            for (i in 1..5) {
                Icon(
                    imageVector = if (i <= rating) Icons.Filled.Star else Icons.Outlined.Star,
                    contentDescription = "Star $i",
                    modifier = Modifier
                        .size(48.dp)
                        .clickable { onRatingChanged(i) }
                        .padding(4.dp),
                    tint = TextBlack
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Ketuk bintang untuk memberi rating",
            style = TextStyle(color = TextBlack, fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
fun ReviewInputSection(text: String, onTextChanged: (String) -> Unit) {
    Column {
        Text(
            text = "Tulis Ulasan Mu",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            modifier = Modifier
                .align(Alignment.Start)
                .padding(bottom = 12.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(BackgroundGray)
                .padding(16.dp)
        ) {
            BasicTextField(
                value = text,
                onValueChange = onTextChanged,
                textStyle = TextStyle(fontSize = 14.sp, color = TextBlack),
                modifier = Modifier.fillMaxSize(),
                decorationBox = { innerTextField ->
                    if (text.isEmpty()) {
                        Text("Tulis pendapatmu tentang buku ini...", color = TextGray)
                    }
                    innerTextField()
                }
            )
        }
    }
}

@Composable
fun PhotoSection(
    photoList: List<String>,
    onAddClick: () -> Unit,
    onRemoveClick: (String) -> Unit
) {
    Column {
        Text(
            text = "Foto (Opsional)",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            modifier = Modifier
                .align(Alignment.Start)
                .padding(bottom = 12.dp)
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(photoList) { photo ->
                PhotoItem(photoName = photo, onRemoveClick = { onRemoveClick(photo) })
            }
            item {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFE0E0E0))
                        .clickable { onAddClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Photo",
                        tint = TextBlack
                    )
                }
            }
        }
    }
}

@Composable
fun PhotoItem(photoName: String, onRemoveClick: () -> Unit) {
    Box(modifier = Modifier.size(100.dp)) {
        Box(
            modifier = Modifier
                .padding(top = 8.dp, end = 8.dp)
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFFFCDD2)),
            contentAlignment = Alignment.BottomStart
        ) {
            Text(
                text = photoName,
                fontSize = 10.sp,
                modifier = Modifier.padding(8.dp)
            )
        }
        Icon(
            imageVector = Icons.Default.Remove,
            contentDescription = "Remove",
            tint = Color.White,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(24.dp)
                .background(Color(0xFFFF6F6F), CircleShape)
                .padding(2.dp)
                .clickable { onRemoveClick() }
        )
    }
}

// ==========================================
// 4. PREVIEWS
// ==========================================

@Preview(showBackground = true, showSystemUi = true, name = "1. Full Screen")
@Composable
fun ReviewScreenPreview() {
    ReviewContent(
        navController = rememberNavController(),
        bookData = BookReviewData("Laskar Pelangi", "Andrea Hirata"),
        rating = 3,
        reviewText = "Buku yang sangat menginspirasi!",
        photoList = listOf("img1.jpg", "img2.jpg"),
        onRatingChanged = {},
        onReviewTextChanged = {},
        onAddPhotoClick = {},
        onRemovePhotoClick = {},
        onSubmitClick = {}
    )
}

@Preview(showBackground = true, name = "2. Top Bar")
@Composable
fun ReviewTopAppBarPreview() {
    ReviewTopAppBar(onBackClick = {})
}