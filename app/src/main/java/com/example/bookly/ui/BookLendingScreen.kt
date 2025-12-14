package com.example.bookly.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable // Import ini
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import com.example.bookly.R
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.bookly.supabase.BooksRepository
import com.example.bookly.supabase.LoansRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LendingScreen(navController: NavController, bookId: String) {
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var book by remember { mutableStateOf<BooksRepository.BookRow?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val borrowDays = 14

    LaunchedEffect(bookId) {
        isLoading = true
        error = null
        try {
            val res = BooksRepository.getBookById(bookId)
            if (res.isSuccess) book = res.getOrNull()
            else error = res.exceptionOrNull()?.localizedMessage ?: "Gagal memuat buku"
        } catch (t: Throwable) {
            error = t.localizedMessage
        }
        isLoading = false
    }

    var showPostConfirmDialog by remember { mutableStateOf(false) }
    var createdLoanId by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(Color.White)) {
                Row(modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { navController.navigateUp() }) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.Black) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Pinjam Buku", color = Green, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                }
                Divider(color = Color.LightGray.copy(alpha = 0.5f), thickness = 1.dp, modifier = Modifier.shadow(1.dp))
            }
        },
        bottomBar = { BottomNavigationBar(navController = navController, selected = "peminjaman") },
        containerColor = Color.White,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when {
                isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Green)
                error != null -> Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = error ?: "Terjadi Kesalahan", color = Color.Red)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { /* reload */ }) { Text("Muat Ulang") }
                }
                book != null -> {
                    val b = book!!
                    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                AsyncImage(model = b.coverImageUrl, contentDescription = b.title, modifier = Modifier.size(120.dp).clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop, placeholder = painterResource(id = R.drawable.book_cover), error = painterResource(id = R.drawable.book_cover))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(text = b.title, fontWeight = FontWeight.Bold, fontSize = 18.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = b.author ?: "", color = GreyText, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                    Box(modifier = Modifier.background(NovelColor, RoundedCornerShape(12.dp)).padding(horizontal = 10.dp, vertical = 6.dp)) {
                                        Text(text = b.category ?: "Novel", color = Green.copy(alpha = 0.8f), fontWeight = FontWeight.SemiBold)
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))

                                    // >>> DISINI PERUBAHANNYA <<<
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { navController.navigate("review/${b.id}") } // Klik untuk lihat review
                                            .padding(4.dp)
                                    ) {
                                        Icon(Icons.Default.Info, contentDescription = null, tint = Color.Transparent, modifier = Modifier.size(0.dp))
                                        repeat(5) { idx ->
                                            Icon(Icons.Default.Star, contentDescription = "star", tint = if (idx < b.rating.toInt()) Color(0xFFFFC107) else Color(0xFFDDDDDD), modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(2.dp))
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(text = String.format("%.1f", b.rating), fontWeight = FontWeight.Bold)
                                    }
                                    // ----------------------------
                                }
                            }

                            // Availability card
                            Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFF6F6F6))) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Info, contentDescription = "info", tint = Green)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(text = "Ketersediaan", fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Column {
                                            Text(text = "Status Buku", color = GreyText)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(text = "${b.availableCopies} dari ${b.totalCopies} tersedia", fontWeight = FontWeight.Bold)
                                        }
                                        Box(modifier = Modifier.background(Green.copy(alpha = 0.15f), RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 8.dp)) {
                                            Text(text = if (b.availableCopies > 0) "Tersedia" else "Kosong", color = Green)
                                        }
                                    }
                                }
                            }

                            // Estimate dates card
                            Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFF6F6F6))) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Outlined.CalendarToday, contentDescription = "calendar", tint = Green)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(text = "Estimasi Tanggal Kembali", fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    val today = Date()
                                    val sdf = SimpleDateFormat("dd MMM yyyy", Locale("id"))
                                    val cal = Calendar.getInstance().apply { time = today; add(Calendar.DAY_OF_YEAR, borrowDays) }
                                    val returnDate = cal.time

                                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Text(text = "Durasi Peminjaman", color = GreyText)
                                            Text(text = "${borrowDays} Hari", fontWeight = FontWeight.Bold)
                                        }
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Text(text = "Tanggal Pinjam", color = GreyText)
                                            Text(text = sdf.format(today), fontWeight = FontWeight.Bold)
                                        }
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Text(text = "Tanggal Kembali", color = GreyText)
                                            Text(text = sdf.format(returnDate), fontWeight = FontWeight.Bold, color = Green)
                                        }
                                    }
                                }
                            }

                            // Notes
                            Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(text = "Catatan Penting", fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(text = "• Buku harus dikembalikan sebelum tanggal jatuh tempo")
                                        Text(text = "• Denda keterlambatan Rp 1.000/hari")
                                        Text(text = "• Jaga kondisi buku dengan baik")
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Button(onClick = {
                                scope.launch {
                                    if (b.availableCopies <= 0) {
                                        snackbarHostState.showSnackbar("Buku tidak tersedia")
                                        return@launch
                                    }
                                    val res = LoansRepository.borrowBook(b.id)
                                    if (res.isSuccess) {
                                        val loan = res.getOrNull()
                                        createdLoanId = loan?.id
                                        snackbarHostState.showSnackbar("Peminjaman berhasil")
                                        showPostConfirmDialog = true
                                    } else {
                                        snackbarHostState.showSnackbar("Gagal: ${res.exceptionOrNull()?.localizedMessage ?: "error"}")
                                    }
                                }
                            }, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Green)) {
                                Icon(imageVector = Icons.Outlined.MenuBook, contentDescription = "konfirmasi", tint = Color.White, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "Konfirmasi Peminjaman", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }

                            if (showPostConfirmDialog) {
                                AlertDialog(
                                    onDismissRequest = { showPostConfirmDialog = false },
                                    title = { Text(text = "Peminjaman Berhasil") },
                                    text = { Text(text = "Peminjaman berhasil. Apa yang ingin Anda lakukan selanjutnya?") },
                                    confirmButton = {
                                        TextButton(onClick = {
                                            showPostConfirmDialog = false
                                            createdLoanId?.let { navController.navigate("peminjamanScreen1/$it") }
                                        }) { Text(text = "Lihat Peminjaman") }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = {
                                            showPostConfirmDialog = false
                                            navController.navigate("katalog_buku")
                                        }) { Text(text = "Kembali ke Katalog") }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}