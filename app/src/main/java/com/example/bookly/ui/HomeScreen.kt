package com.example.bookly.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.example.bookly.R
import com.example.bookly.viewmodel.HomeViewModel

// --- Data Models (Digunakan oleh ViewModel) ---
data class CategoryData(val name: String, val iconRes: Int)

// BookDummy sekarang digunakan sebagai UI Model hasil mapping dari DB
data class BookDummy(
    val id: String,
    val title: String,
    val author: String,
    val rating: Int,
    val stock: Int,
    val category: String,
    val coverUrl: String
)

// --- Colors ---
//val PrimaryGreen = Color(0xFF2D9C6D)
val LightGreenBg = Color(0xFFE8F5E9)

@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = viewModel() // Inject ViewModel
) {
    // Ambil Data Real dari ViewModel
    val state by viewModel.uiState.collectAsState()

    // Data Kategori (Statis tidak apa-apa)
    val categories = listOf(
        CategoryData("Pendidikan", R.drawable.ic_launcher_foreground),
        CategoryData("Bisnis", R.drawable.ic_launcher_foreground),
        CategoryData("Sains", R.drawable.ic_launcher_foreground),
        CategoryData("Novel", R.drawable.ic_launcher_foreground),
        CategoryData("Sejarah", R.drawable.ic_launcher_foreground)
    )

    Scaffold(
        containerColor = Color(0xFFF8F9FA),
        bottomBar = {
            BottomNavigationBar(
                navController = navController,
                selected = "beranda"
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                // 1. Header & Kategori (Pakai Data User Asli)
                item {
                    TopSectionWithOverlappingCard(
                        userName = state.userName, // Nama Asli
                        categories = categories
                    )
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }

                // 2. Buku Populer (Pakai Data Asli)
                item {
                    SectionTitle("Buku Populer")

                    if (state.isLoading) {
                        // Loading Skeleton Sederhana
                        Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = PrimaryGreen)
                        }
                    } else if (state.popularBooks.isEmpty()) {
                        Text("Belum ada data buku populer.", modifier = Modifier.padding(horizontal = 24.dp), color = Color.Gray)
                    } else {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(state.popularBooks) { book ->
                                PopularBookCard(book) {
                                    navController.navigate("book_detail/${book.id}")
                                }
                            }
                        }
                    }
                }

                // 3. Rekomendasi (Pakai Data Asli - Buku Terbaru)
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    SectionTitle("Rekomendasi")
                }

                if (!state.isLoading && state.recommendedBooks.isNotEmpty()) {
                    items(state.recommendedBooks) { book ->
                        RecommendationBookItem(book) {
                            navController.navigate("book_detail/${book.id}")
                        }
                    }
                } else if (!state.isLoading && state.recommendedBooks.isEmpty()) {
                    item { Text("Belum ada rekomendasi.", modifier = Modifier.padding(horizontal = 24.dp), color = Color.Gray) }
                }

                // Spacer bawah
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

// --- KOMPONEN UI (Sama seperti sebelumnya) ---

@Composable
fun TopSectionWithOverlappingCard(userName: String, categories: List<CategoryData>) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter
    ) {
        // LAYER 1: Background Hijau
        Box(
            modifier = Modifier.fillMaxWidth().height(240.dp)
        ) {
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                val path = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(size.width, 0f)
                    lineTo(size.width, size.height - 80f)
                    quadraticBezierTo(size.width / 2, size.height, 0f, size.height - 80f)
                    close()
                }
                drawPath(path = path, color = PrimaryGreen)
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 40.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_launcher_background),
                        contentDescription = "Profile",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(50.dp).clip(CircleShape).background(Color.White)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(text = "Halo $userName!", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(text = "Mau pinjam buku apa hari ini?", color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp)
                    }
                }
                IconButton(onClick = { /* TODO */ }) {
                    Icon(Icons.Default.Notifications, contentDescription = "Notification", tint = Color.White)
                }
            }
        }

        // LAYER 2: Card Kategori
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 120.dp)
        ) {
            Column(modifier = Modifier.padding(vertical = 16.dp), horizontalAlignment = Alignment.Start) {
                Text(text = "Kategori Buku", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(start = 16.dp, bottom = 12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    categories.forEach { category ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier.size(48.dp).clip(CircleShape).background(LightGreenBg).clickable { },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Search, contentDescription = category.name, tint = PrimaryGreen, modifier = Modifier.size(24.dp))
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = category.name, fontSize = 11.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PopularBookCard(book: BookDummy, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.width(160.dp).clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            AsyncImage(
                model = book.coverUrl, contentDescription = book.title, contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(100.dp).clip(RoundedCornerShape(8.dp)).background(Color.LightGray)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = book.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(text = book.author, fontSize = 12.sp, color = Color.Gray, maxLines = 1)
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = LightGreenBg, shape = RoundedCornerShape(4.dp)) {
                    Text(text = book.category, fontSize = 10.sp, color = PrimaryGreen, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(text = "12 hr lagi", fontSize = 10.sp, color = Color.Red)
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(progress = 0.7f, color = PrimaryGreen, trackColor = Color.LightGray.copy(alpha = 0.3f), modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)))
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "${book.stock} Eksemplar", fontSize = 10.sp, color = Color.Gray)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFC107), modifier = Modifier.size(12.dp))
                    Text(text = "${book.rating}", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun RecommendationBookItem(book: BookDummy, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp).clickable { onClick() }
    ) {
        Row(modifier = Modifier.padding(12.dp)) {
            AsyncImage(
                model = book.coverUrl, contentDescription = book.title, contentScale = ContentScale.Crop,
                modifier = Modifier.width(80.dp).height(110.dp).clip(RoundedCornerShape(8.dp)).background(Color.LightGray)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Surface(color = LightGreenBg, shape = RoundedCornerShape(4.dp)) {
                        Text(text = book.category, fontSize = 10.sp, color = PrimaryGreen, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                    Surface(color = Color(0xFFFFEBEE), shape = RoundedCornerShape(4.dp)) {
                        Text(text = "Ingin Dibaca", fontSize = 10.sp, color = Color.Red, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = book.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(text = book.author, fontSize = 14.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    repeat(5) { Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFC107), modifier = Modifier.size(16.dp)) }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "${book.rating}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "${book.stock} Eksemplar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Icon(Icons.Default.BookmarkBorder, contentDescription = "Wishlist", tint = Color.Red)
                }
            }
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(text = title, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp))
}

@Preview(showBackground = true, heightDp = 1200)
@Composable
fun HomeScreenPreview() {
    HomeScreen(navController = rememberNavController())
}