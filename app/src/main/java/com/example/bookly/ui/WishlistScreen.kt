
package com.example.bookly.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import coil.compose.AsyncImage
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.bookly.R
import com.example.bookly.viewmodel.WishlistViewModel

// Wishlist-specific Colors
private val WishlistGreen = Color(0xFF2E8B57)
private val EmptyStateGray = Color(0xFFBDBDBD)
private val EmptyStateTextGray = Color(0xFF9E9E9E)

@Composable
fun WishlistScreen(
    navController: NavController,
    wishlistViewModel: WishlistViewModel = viewModel()
) {
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

    Scaffold(
        topBar = { WishlistTopAppBar(navController) },
        bottomBar = { BottomNavigationBar(navController = navController, selected = "wishlist") },
        containerColor = Color.White,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        if (wishlistBooks.isEmpty()) {
            // Empty State UI matching the screenshot
            WishlistEmptyState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        } else {
            // Populated Wishlist
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(Color.White)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
            ) {
                items(wishlistBooks) { book ->
                    WishlistBookCard(
                        book = book,
                        onRemoveClick = { wishlistViewModel.removeFromWishlist(book.title) },
                        onBookClick = { navController.navigate("book_detail/${book.id}") }
                    )
                }
            }
        }
    }
}

@Composable
private fun WishlistEmptyState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            // Large heart icon
            Icon(
                imageVector = Icons.Outlined.FavoriteBorder,
                contentDescription = "Empty Wishlist",
                tint = EmptyStateGray,
                modifier = Modifier.size(120.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Main title
            Text(
                text = "Wishlist Kosong",
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = EmptyStateTextGray,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Subtitle
            Text(
                text = "Belum ada buku di wishlist kamu.\nMulai tambahkan buku favorit kamu!",
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                color = EmptyStateTextGray,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WishlistTopAppBar(navController: NavController) {
    Column(modifier = Modifier.background(Color.White)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Wishlist",
                color = WishlistGreen,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Divider(color = Color.LightGray.copy(alpha = 0.5f), thickness = 1.dp, modifier = Modifier.shadow(1.dp))
    }
}

@Composable
private fun WishlistBookCard(
    book: Book, 
    onRemoveClick: () -> Unit,
    onBookClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onBookClick() },
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
                    color = GreyText,
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
                    Text(
                        text = "${book.rating ?: 0f} • ${book.availability}",
                        color = GreyText,
                        fontSize = 12.sp
                    )
                }
            }
            IconButton(onClick = onRemoveClick) {
                Icon(
                    Icons.Filled.Favorite,
                    contentDescription = "Remove from Wishlist",
                    tint = Color(0xFFD91F11)
                )
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Empty Wishlist")
@Composable
fun WishlistScreenEmptyPreview() {
    WishlistScreen(navController = rememberNavController())
}

@Preview(showBackground = true, name = "Empty State Only")
@Composable
fun WishlistEmptyStatePreview() {
    WishlistEmptyState(modifier = Modifier.fillMaxSize())
}

@Preview(showBackground = true, name = "Wishlist Book Card")
@Composable
fun WishlistBookCardPreview() {
    Column(modifier = Modifier.padding(16.dp)) {
        WishlistBookCard(
            book = Book(
                id = "1",
                title = "The Great Gatsby",
                author = "F. Scott Fitzgerald",
                rating = 4.5f,
                availability = "Tersedia",
                category = "Fiction",
                categoryColor = Color(0xFFE0F2F1),
                coverImage = R.drawable.book_cover
            ),
            onRemoveClick = {}
        )
    }
}

