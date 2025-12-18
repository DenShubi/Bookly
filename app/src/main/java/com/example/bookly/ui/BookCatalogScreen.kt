package com.example.bookly.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.example.bookly.viewmodel.BookCatalogViewModel
import com.example.bookly.viewmodel.WishlistViewModel

// Colors
val Green = Color(0xFF2E8B57)
val StarColor = Color(0xFFFFB800)

// Category Colors
val NovelColor = Color(0xFFE0F2F1)
val BisnisColor = Color(0xFFFCE4EC)
val PendidikanColor = Color(0xFFE3F2FD)
val SejarahColor = Color(0xFFFFF3E0)

// Data Class
data class Book(
    val id: String = "",
    val title: String,
    val author: String,
    val rating: Float?,
    val availability: String,
    val category: String,
    val categoryColor: Color,
    val coverImage: Int,
    val coverImageUrl: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookCatalogScreen(
    navController: NavController,
    catalogViewModel: BookCatalogViewModel = viewModel(),
    wishlistViewModel: WishlistViewModel = viewModel()
) {
    var searchQuery by remember { mutableStateOf("") }
    // Collect state from ViewModels
    val bookRows by catalogViewModel.books.collectAsState()
    val isLoading by catalogViewModel.isLoading.collectAsState()
    val wishlistBooks by wishlistViewModel.wishlist.collectAsState()
    val toastMessage by wishlistViewModel.toastMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Show toast message using Snackbar
    LaunchedEffect(toastMessage) {
        toastMessage?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
            wishlistViewModel.clearToastMessage()
        }
    }

    // Map BookRow -> Book (UI model)
    val allBooks = remember(bookRows) {
        bookRows.map { r ->
            val color = when (r.category?.lowercase()) {
                "novel" -> NovelColor
                "bisnis" -> BisnisColor
                "pendidikan" -> PendidikanColor
                "sejarah" -> SejarahColor
                else -> NovelColor
            }
            Book(
                id = r.id,
                title = r.title.ifBlank { "-" },
                author = r.author.ifBlank { "-" },
                rating = r.rating,
                availability = "${r.availableCopies}/${r.totalCopies} tersedia",
                category = r.category ?: "",
                categoryColor = color,
                coverImage = R.drawable.book_cover,
                coverImageUrl = r.coverImageUrl
            )
        }
    }

    // Load wishlist when books are loaded
    LaunchedEffect(allBooks) {
        if (allBooks.isNotEmpty()) {
            wishlistViewModel.loadWishlist(allBooks)
        }
    }

    // Filter books based on search query
    val filteredBooks = remember(allBooks, searchQuery) {
        if (searchQuery.isBlank()) {
            allBooks
        } else {
            allBooks.filter { book ->
                book.title.contains(searchQuery, ignoreCase = true) ||
                        book.author.contains(searchQuery, ignoreCase = true)
            }
        }
    }
    val pullToRefreshState = rememberPullToRefreshState()

    Scaffold(
        topBar = { BookCatalogTopAppBar(navController) },
        bottomBar = { BottomNavigationBar(navController = navController, selected = "buku") },
        // --- BAGIAN BARU: Floating Action Button untuk Request Buku ---
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("request_book") },
                containerColor = Green,
                contentColor = Color.White
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Request Buku Baru"
                )
            }
        },
        // -------------------------------------------------------------
        containerColor = Color.White,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        PullToRefreshBox(
            state = pullToRefreshState,
            isRefreshing = isLoading,
            onRefresh = { catalogViewModel.refreshBooks() },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp) // Tambah padding bottom agar tidak tertutup FAB
            ) {
                item {
                    SearchBar(
                        searchQuery = searchQuery,
                        onSearchQueryChange = { query ->
                            searchQuery = query
                        }
                    )
                }
                item {
                    FilterSortRow()
                }
                if (filteredBooks.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Tidak ada buku ditemukan",
                                color = Color.Gray,
                                fontSize = 16.sp
                            )
                        }
                    }
                } else {
                    items(filteredBooks) { book ->
                        val isWishlisted = wishlistBooks.any { it.id == book.id }
                        BookCard(
                            book = book,
                            isWishlisted = isWishlisted,
                            onWishlistClick = { wishlistViewModel.toggleWishlist(book) },
                            onClick = { navController.navigate("book_detail/${book.id}") }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookCatalogTopAppBar(navController: NavController) {
    Column(modifier = Modifier.background(Color.White)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Katalog Buku",
                color = Green,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Divider(color = Color.LightGray.copy(alpha = 0.5f), thickness = 1.dp, modifier = Modifier.shadow(1.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBar(
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {}
) {
    TextField(
        value = searchQuery,
        onValueChange = onSearchQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(3.dp, RoundedCornerShape(16.dp)),
        placeholder = { Text("Cari judul buku, penulis...", color = Color.Gray) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Icon", tint = Color.Gray) },
        shape = RoundedCornerShape(16.dp),
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            unfocusedContainerColor = Color.White,
            focusedContainerColor = Color.White
        )
    )
}

@Composable
fun FilterSortRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.Start)
    ) {
        FilterSortItem(icon = Icons.Default.FilterList, text = "Filter")
        FilterSortItem(icon = Icons.Default.SwapVert, text = "Urutkan")
    }
}

@Composable
fun FilterSortItem(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { }) {
        Icon(icon, contentDescription = text, tint = Green)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, color = Green, fontSize = 16.sp)
    }
}

@Composable
fun BookCard(
    book: Book,
    isWishlisted: Boolean = false,
    onWishlistClick: () -> Unit = {},
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = book.coverImageUrl,
                contentDescription = book.title,
                modifier = Modifier
                    .size(80.dp, 100.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(id = R.drawable.book_cover),
                error = painterResource(id = R.drawable.book_cover)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = book.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = book.author,
                    color = Color.Gray, // Fixed missing GreyText variable
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
                Chip(text = book.category, color = book.categoryColor)
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = "Rating",
                        tint = StarColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "${book.rating ?: 0f} • ${book.availability}", color = Color.Gray, fontSize = 12.sp) // Fixed missing GreyText
                }
            }
            IconButton(onClick = onWishlistClick) {
                Icon(
                    imageVector = if (isWishlisted) Icons.Filled.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (isWishlisted) "Remove from Wishlist" else "Add to Wishlist",
                    tint = if (isWishlisted) Color(0xFFD91F11) else Color.Gray
                )
            }
        }
    }
}

@Composable
fun Chip(text: String, color: Color) {
    Box(
        modifier = Modifier
            .background(color, CircleShape)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            color = Green.copy(alpha=0.8f),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun BookCatalogScreenPreview() {
    BookCatalogScreen(navController = rememberNavController())
}

@Preview(showBackground = true)
@Composable
fun BookCatalogTopAppBarPreview() {
    BookCatalogTopAppBar(navController = rememberNavController())
}

@Preview(showBackground = true, widthDp = 360)
@Composable
fun SearchBarPreview() {
    Column(modifier = Modifier.padding(16.dp)) {
        SearchBar()
    }
}

@Preview(showBackground = true)
@Composable
fun FilterSortRowPreview() {
    FilterSortRow()
}

@Preview(showBackground = true)
@Composable
fun FilterSortItemPreview() {
    FilterSortItem(icon = Icons.Default.FilterList, text = "Filter")
}

@Preview(showBackground = true, widthDp = 360)
@Composable
fun BookCardPreview() {
    Column(modifier = Modifier.padding(16.dp)) {
        BookCard(book = Book(id = "1", title = "Laskar Pelangi", author = "Andrea Hirata", rating = 4.9f, availability = "5/8 tersedia", category = "Novel", categoryColor = NovelColor, coverImage = R.drawable.book_cover), onClick = {})
    }
}

@Preview(showBackground = true)
@Composable
fun ChipPreview() {
    Chip(text = "Novel", color = NovelColor)
}