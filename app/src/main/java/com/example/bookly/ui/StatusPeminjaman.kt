package com.example.bookly.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.bookly.supabase.LoansRepository
import java.text.SimpleDateFormat
import java.util.*

// Use shared colors from SharedComponents.kt: `BottomNavGreen` and `GreyText`

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusPeminjaman(navController: NavController, loanId: String?) {
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var loan by remember { mutableStateOf<LoansRepository.LoanRow?>(null) }

    LaunchedEffect(loanId) {
        isLoading = true
        error = null
        try {
            val res = if (!loanId.isNullOrBlank()) LoansRepository.getLoanById(loanId) else LoansRepository.getActiveLoan()
            if (res.isSuccess) loan = res.getOrNull()
            else error = res.exceptionOrNull()?.localizedMessage ?: "Gagal memuat data"
        } catch (t: Throwable) {
            error = t.localizedMessage
        }
        isLoading = false
    }

    Scaffold(
        topBar = {
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
                    Text(text = "Riwayat Peminjaman", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = BottomNavGreen)
                }
                Divider(color = Color.LightGray.copy(alpha = 0.5f), thickness = 1.dp)
            }
        },
        bottomBar = { BottomNavigationBar(navController = navController, selected = "peminjaman") },
        containerColor = Color.White
    ) { paddingValues ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)) {

            when {
                isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = BottomNavGreen)
                error != null -> Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = error ?: "Terjadi kesalahan", color = Color.Red)
                }
                loan != null -> {
                    val l = loan!!
                    val sdf = SimpleDateFormat("dd MMM yyyy", Locale("id"))
                    val sdfTime = SimpleDateFormat("HH:mm", Locale("id"))

                    Column(modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

                        // Status pill
                        Box(modifier = Modifier
                            .fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Box(modifier = Modifier
                                .height(48.dp)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFDFF4E9)), contentAlignment = Alignment.Center) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = BottomNavGreen)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = "Buku Telah Dikembalikan", fontWeight = FontWeight.SemiBold, color = BottomNavGreen)
                                }
                            }
                        }

                        // Book card
                        Card(shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
                            Row(modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                AsyncImage(
                                    model = l.coverImageUrl,
                                    contentDescription = l.bookTitle,
                                    modifier = Modifier
                                        .size(width = 110.dp, height = 140.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    placeholder = null,
                                    error = null
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = l.bookTitle.orEmpty(), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(text = l.bookAuthor.orEmpty(), color = GreyText)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Box(modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF81C784))) {
                                        Text(text = "Novel", color = Color.White, modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp))
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(text = "ID Peminjaman", color = GreyText)
                                    Text(text = "#${l.id}", fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Timeline card
                        Card(shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.CalendarToday, tint = BottomNavGreen, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = "Timeline Peminjaman", fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(12.dp))

                                // Buku Dipinjam
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(BottomNavGreen))
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Box(modifier = Modifier.width(2.dp).height(40.dp).background(Color(0xFFDCF2DF)))
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(text = "Buku Dipinjam", fontWeight = FontWeight.SemiBold)
                                        Text(text = l.borrowedAt?.let { sdf.format(it) + " • " + sdfTime.format(it) + " WIB" } ?: "-", color = GreyText)
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Batas Pengembalian
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(Color(0xFFFFB74D)))
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Box(modifier = Modifier.width(2.dp).height(40.dp).background(Color(0xFFFFF3E0)))
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(text = "Batas Pengembalian", fontWeight = FontWeight.SemiBold)
                                        Text(text = l.returnDeadline?.let { sdf.format(it) + " • " + sdfTime.format(it) + " WIB" } ?: "-", color = GreyText)
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Buku Dikembalikan
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(BottomNavGreen))
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(text = "Buku Dikembalikan", fontWeight = FontWeight.SemiBold)
                                        Text(text = l.returnDeadline?.let { sdf.format(it) + " • " + sdfTime.format(it) + " WIB" } ?: "-", color = GreyText)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Box(modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFFB9F6CA))
                                            .padding(horizontal = 10.dp, vertical = 6.dp)) {
                                            Text(text = "Tepat Waktu", color = BottomNavGreen, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                            }
                        }

                        // Summary card (two-column label/value layout for alignment)
                        Card(shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Article, tint = BottomNavGreen, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = "Ringkasan Peminjaman", fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(12.dp))

                                Row(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Total Durasi", color = GreyText)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text("Kondisi Buku", color = GreyText)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text("Denda", color = GreyText)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text("Status Pembayaran", color = GreyText)
                                    }

                                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                                        Text(text = "${l.durationDays} Hari", fontWeight = FontWeight.SemiBold)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(text = l.status ?: "Baik", fontWeight = FontWeight.SemiBold)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(text = "Tidak Ada", color = BottomNavGreen, fontWeight = FontWeight.SemiBold)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Box(modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFFB9F6CA))
                                            .padding(horizontal = 10.dp, vertical = 6.dp)) {
                                            Text(text = "Tidak Ada Denda", color = BottomNavGreen, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                            }
                        }

                        // Notes / return transaction
                        Card(shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(text = "Catatan Pengembalian", fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(12.dp))

                                // inner note box
                                Box(modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFF1F4F6))
                                    .padding(12.dp)) {
                                    Column {
                                        Text(text = "ID Transaksi: #${l.id}", color = BottomNavGreen, fontWeight = FontWeight.SemiBold)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(text = "Buku telah dikembalikan dalam kondisi baik. Tidak ada denda.", color = GreyText)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(text = "Terima kasih telah menggunakan layanan perpustakaan dengan baik.", color = GreyText.copy(alpha = 0.8f), fontSize = 13.sp)
                                    }
                                }
                            }
                        }

                        // Completed block
                        Box(modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(color = Color(0xFFEFF7F0))
                            .padding(vertical = 28.dp, horizontal = 16.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(36.dp), tint = BottomNavGreen)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(text = "Peminjaman Selesai", fontWeight = FontWeight.SemiBold, color = BottomNavGreen)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(text = "Anda dapat meminjam buku lainnya kapan saja", color = GreyText)
                            }
                        }

                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }
}
