package com.example.bookly.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.example.bookly.R
import com.example.bookly.viewmodel.BookDetailViewModel
import com.example.bookly.viewmodel.WishlistViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailScreen(
    navController: NavController,
    bookId: String,
    detailViewModel: BookDetailViewModel = viewModel(),
    wishlistViewModel: WishlistViewModel = viewModel()
) {
    val bookDetail by detailViewModel.bookDetail.collectAsState()
    val isLoading by detailViewModel.isLoading.collectAsState()
    val errorMessage by detailViewModel.errorMessage.collectAsState()
    val wishlistBooks by wishlistViewModel.wishlist.collectAsState()
    val toastMessage by wishlistViewModel.toastMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    // Load book details
    LaunchedEffect(bookId) {
        detailViewModel.loadBookDetail(bookId)
    }

    // Show toast message
    LaunchedEffect(toastMessage) {
        toastMessage?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
            wishlistViewModel.clearToastMessage()
        }
    }

    Scaffold(
        topBar = {
            BookDetailTopAppBar(navController)
        },
        bottomBar = {
            BottomNavigationBar(navController = navController, selected = "buku")
        },
        containerColor = Color.White,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = Green
                    )
                }
                errorMessage != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = errorMessage ?: "Terjadi kesalahan",
                            color = Color.Red,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { detailViewModel.loadBookDetail(bookId) },
                            colors = ButtonDefaults.buttonColors(containerColor = Green)
                        ) {
                            Text("Coba Lagi")
                        }
                    }
                }
                bookDetail != null -> {
                    val book = bookDetail!!
                    val isWishlisted = wishlistBooks.any { it.id == book.id }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        item {
                            // Book Cover Image
                            AsyncImage(
                                model = book.coverImageUrl,
                                contentDescription = book.title,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(320.dp)
                                    .clip(RoundedCornerShape(16.dp)),
                                contentScale = ContentScale.Crop,
                                placeholder = painterResource(id = R.drawable.book_cover),
                                error = painterResource(id = R.drawable.book_cover)
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            // Book Title
                            Text(
                                text = book.title,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Author Name
                            Text(
                                text = book.author,
                                fontSize = 16.sp,
                                color = GreyText,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Category Chip and Rating Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val categoryColor = when (book.category?.lowercase()) {
                                    "novel" -> NovelColor
                                    "bisnis" -> BisnisColor
                                    "pendidikan" -> PendidikanColor
                                    "sejarah" -> SejarahColor
                                    else -> NovelColor
                                }

                                Box(
                                    modifier = Modifier
                                        .background(categoryColor, CircleShape)
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = book.category ?: "Novel",
                                        color = Green.copy(alpha = 0.8f),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                // Rating
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Star,
                                        contentDescription = "Rating",
                                        tint = StarColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = String.format("%.1f", book.rating),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // Informasi Buku Section
                            Text(
                                text = "Informasi Buku",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Info Rows
                            BookInfoRow(label = "Penerbit", value = book.publisher)
                            BookInfoRow(label = "Tahun Terbit", value = book.publicationYear.toString())
                            BookInfoRow(label = "Halaman", value = "${book.pages} halaman")
                            BookInfoRow(label = "Bahasa", value = book.language)
                            BookInfoRow(
                                label = "Ketersediaan",
                                value = "${book.availableCopies} dari ${book.totalCopies} tersedia",
                                valueColor = if (book.availableCopies > 0) Green else Color.Red
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            // Deskripsi Section
                            Text(
                                text = "Deskripsi",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = book.description,
                                fontSize = 14.sp,
                                color = GreyText,
                                lineHeight = 20.sp,
                                textAlign = TextAlign.Justify
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            // Action Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Peminjaman Button (disabled for now)
                                Button(
                                    onClick = { /* TODO: Navigate to peminjaman screen */ },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = BottomNavInactive,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    enabled = false
                                ) {
                                    Text(
                                        text = "Peminjaman",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                // Wishlist Button
                                Button(
                                    onClick = {
                                        val bookForWishlist = Book(
                                            id = book.id,
                                            title = book.title,
                                            author = book.author,
                                            rating = book.rating,
                                            availability = "${book.availableCopies}/${book.totalCopies} tersedia",
                                            category = book.category ?: "",
                                            categoryColor = when (book.category?.lowercase()) {
                                                "novel" -> NovelColor
                                                "bisnis" -> BisnisColor
                                                "pendidikan" -> PendidikanColor
                                                "sejarah" -> SejarahColor
                                                else -> NovelColor
                                            },
                                            coverImage = R.drawable.book_cover,
                                            coverImageUrl = book.coverImageUrl
                                        )
                                        wishlistViewModel.toggleWishlist(bookForWishlist)
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isWishlisted) Color(0xFFD91F11) else Green,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isWishlisted) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                        contentDescription = if (isWishlisted) "Remove from Wishlist" else "Add to Wishlist",
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isWishlisted) "Hapus Wishlist" else "Tambah Wishlist",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailTopAppBar(navController: NavController) {
    Column(modifier = Modifier.background(Color.White)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.navigateUp() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.Black)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Detail Buku",
                color = Green,
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
fun BookInfoRow(
    label: String,
    value: String,
    valueColor: Color = Color.Black
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = GreyText,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            color = valueColor,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun BookDetailScreenPreview() {
    BookDetailScreen(
        navController = rememberNavController(),
        bookId = "sample-id"
    )
}

@Preview(showBackground = true)
@Composable
fun BookDetailTopAppBarPreview() {
    BookDetailTopAppBar(navController = rememberNavController())
}

@Preview(showBackground = true)
@Composable
fun BookInfoRowPreview() {
    Column(modifier = Modifier.padding(16.dp)) {
        BookInfoRow(label = "Penerbit", value = "Bentang Pustaka")
        BookInfoRow(label = "Tahun Terbit", value = "2005")
        BookInfoRow(label = "Halaman", value = "529 halaman")
        BookInfoRow(label = "Bahasa", value = "Indonesia")
        BookInfoRow(label = "Ketersediaan", value = "5 dari 8 tersedia", valueColor = Green)
    }
}

