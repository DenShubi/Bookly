package com.example.bookly.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.bookly.R
import com.example.bookly.supabase.BooksRepository
import com.example.bookly.supabase.LoansRepository
import com.example.bookly.supabase.WishlistRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Bookdetailscreen1(navController: NavController, bookId: String) {
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var book by remember { mutableStateOf<BooksRepository.BookRow?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    // local colors for buttons to match design
    val wishlistBg = Color(0xFFF7E7CC)
    val wishlistTextColor = Color(0xFFDA9A3C)

    LaunchedEffect(bookId) {
        isLoading = true
        error = null
        try {
            val result = BooksRepository.getBookById(bookId)
            if (result.isSuccess) book = result.getOrNull()
            else error = result.exceptionOrNull()?.localizedMessage ?: "Gagal memuat buku"
        } catch (t: Throwable) {
            error = t.localizedMessage
        }
        isLoading = false
    }

    Scaffold(
        topBar = { BookDetailTopAppBar(navController = navController) },
        bottomBar = { BottomNavigationBar(navController = navController, selected = "buku") },
        containerColor = Color.White,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)) {

            when {
                isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Green)
                error != null -> Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = error ?: "Terjadi Kesalahan", color = Color.Red)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { /* reload */ }) { Text("Muat Ulang") }
                }
                book != null -> {
                    val b = book!!
                    Column(modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)) {

                        AsyncImage(
                            model = b.coverImageUrl,
                            contentDescription = b.title,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop,
                            placeholder = painterResource(id = R.drawable.book_cover),
                            error = painterResource(id = R.drawable.book_cover)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(text = b.title, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = b.author, fontSize = 14.sp, color = GreyText, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier
                                .background(NovelColor, CircleShape)
                                .padding(horizontal = 12.dp, vertical = 6.dp)) {
                                Text(text = b.category ?: "Novel", color = Green.copy(alpha = 0.8f), fontWeight = FontWeight.SemiBold)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Icon(Icons.Default.Star, contentDescription = "Rating", tint = StarColor)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = String.format("%.1f", b.rating), fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFF6F6F6))) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(text = "Informasi Buku", fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))
                                BookInfoRow(label = "Penerbit", value = b.publisher ?: "-")
                                BookInfoRow(label = "Tahun Terbit", value = b.publicationYear?.toString() ?: "-")
                                BookInfoRow(label = "Halaman", value = b.pages?.let { "$it halaman" } ?: "-")
                                BookInfoRow(label = "Bahasa", value = b.language ?: "-")
                                BookInfoRow(label = "Ketersediaan", value = "${b.availableCopies} dari ${b.totalCopies} tersedia", valueColor = if (b.availableCopies > 0) Green else Color.Red)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = "Deskripsi", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = b.description ?: "-", color = GreyText, lineHeight = 20.sp)

                        Spacer(modifier = Modifier.height(20.dp))

                        // Buttons
                        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Wishlist button (match BookDetailScreen sizes/style)
                            ElevatedButton(onClick = {
                                scope.launch {
                                    val res = WishlistRepository.addToWishlist(b.id)
                                    if (res.isSuccess) snackbarHostState.showSnackbar("Ditambahkan ke Wishlist")
                                    else snackbarHostState.showSnackbar("Gagal menambahkan wishlist: ${res.exceptionOrNull()?.localizedMessage ?: "error"}")
                                }
                            }, modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = wishlistBg, contentColor = wishlistTextColor)) {
                                Icon(imageVector = Icons.Outlined.Favorite, contentDescription = "Wishlist", tint = wishlistTextColor, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "Tambah ke Wishlist", fontWeight = FontWeight.Bold, color = wishlistTextColor, fontSize = 16.sp)
                            }

                            Button(onClick = {
                                // Navigate to confirmation screen first; actual borrowing happens there
                                navController.navigate("book_borrow_confirm/${b.id}")
                            }, modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Green), shape = RoundedCornerShape(12.dp)) {
                                Icon(imageVector = Icons.Outlined.MenuBook, contentDescription = "Pinjam", tint = Color.White, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "Pinjam Buku", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
