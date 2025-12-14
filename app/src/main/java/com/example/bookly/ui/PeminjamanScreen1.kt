package com.example.bookly.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.bookly.supabase.LoansRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

import androidx.compose.ui.text.style.TextOverflow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeminjamanScreen1(navController: NavController, loanId: String?) {
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var loan by remember { mutableStateOf<LoansRepository.LoanRow?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(loanId) {
        isLoading = true
        error = null
        try {
            val res = if (!loanId.isNullOrBlank()) LoansRepository.getLoanById(loanId) else LoansRepository.getActiveLoan()
            if (res.isSuccess) loan = res.getOrNull()
            else error = res.exceptionOrNull()?.localizedMessage ?: "Gagal memuat data peminjaman"
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
                    Text(text = "Status Peminjaman", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = BottomNavGreen)
                }
                Divider(color = Color.LightGray.copy(alpha = 0.5f), thickness = 1.dp)
            }
        },
        bottomBar = { BottomNavigationBar(navController = navController, selected = "peminjaman") },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.White
    ) { paddingValues ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)) {

            when {
                isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = BottomNavGreen)
                error != null -> Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = error ?: "Terjadi Kesalahan", color = Color.Red)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { /* reload */ }) { Text("Muat Ulang") }
                }
                loan != null -> {
                    val l = loan!!
                    val sdf = SimpleDateFormat("dd MMM yyyy", Locale("id"))
                    val sdfTime = SimpleDateFormat("HH:mm", Locale("id"))
                    val today = Date()
                    val borrowedAtDate = l.borrowedAt ?: today
                    val returnDeadlineDate = l.returnDeadline ?: today
                    val daysPassed = ((today.time - borrowedAtDate.time) / (1000 * 60 * 60 * 24)).toInt()
                    val daysRemaining = ((returnDeadlineDate.time - today.time) / (1000 * 60 * 60 * 24)).toInt()

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {

                        // Status pill
                        Box(modifier = Modifier
                            .fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Box(modifier = Modifier
                                .height(40.dp)
                                .widthIn(min = 140.dp)
                                .background(color = Color(0xFFDFF4E9), shape = RoundedCornerShape(20.dp)), contentAlignment = Alignment.Center) {
                                Text(text = l.status?.uppercase() ?: "AKTIF", color = Color(0xFF2E8B57), fontWeight = FontWeight.Bold)
                            }
                        }

                        // Book card
                        Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
                            Row(modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                // image placeholder
                                Box(modifier = Modifier
                                    .size(96.dp)
                                    .background(color = Color(0xFFF0F0F0), shape = RoundedCornerShape(8.dp)))

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = l.bookTitle.orEmpty(), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = l.bookAuthor.orEmpty(), color = GreyText)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    // category pill
                                    Box(modifier = Modifier
                                        .background(color = Color(0xFFE8F4EE), shape = RoundedCornerShape(12.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)) {
                                        Text(text = "Novel", color = Color(0xFF2E8B57), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(text = "ID Peminjaman", fontSize = 12.sp, color = GreyText)
                                    Text(text = "#${l.id}", fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Return date card
                        Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF6F6F6))) {
                            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Outlined.CalendarToday, contentDescription = null, tint = Color(0xFF2E8B57))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = "Tanggal Pengembalian", fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Box(modifier = Modifier
                                    .fillMaxWidth()
                                    .background(color = Color(0xFFFFFFFF), shape = RoundedCornerShape(8.dp))
                                    .padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        // decide whether to show time component depending on DB value
                                        val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Jakarta"))
                                        cal.time = returnDeadlineDate
                                        val hour = cal.get(Calendar.HOUR_OF_DAY)
                                        val minute = cal.get(Calendar.MINUTE)
                                        val timePresent = hour != 0 || minute != 0

                                        Text(text = sdf.format(returnDeadlineDate), fontWeight = FontWeight.Bold, fontSize = 22.sp, color = Color(0xFF2E8B57))
                                        Spacer(modifier = Modifier.height(6.dp))
                                        if (timePresent) Text(text = "${sdfTime.format(returnDeadlineDate)} WIB", color = GreyText)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(text = if (daysRemaining >= 0) "$daysRemaining hari lagi" else "${-daysRemaining} hari terlambat", color = Color(0xFF2E8B57))
                                    }
                                }
                            }
                        }

                        // Detail card
                        Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF6F6F6))) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Detail Peminjaman", fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column { Text("Tanggal Pinjam", color = GreyText); Spacer(modifier = Modifier.height(6.dp)); Text("${sdf.format(borrowedAtDate)} • ${sdfTime.format(borrowedAtDate)}") }
                                    Column(horizontalAlignment = Alignment.End) { Text("Durasi Peminjaman", color = GreyText); Spacer(modifier = Modifier.height(6.dp)); Text("${l.durationDays} Hari") }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column { Text("Hari Berlalu", color = GreyText); Spacer(modifier = Modifier.height(6.dp)); Text("$daysPassed Hari") }
                                    Column(horizontalAlignment = Alignment.End) { /* empty for symmetry */ }
                                }
                            }
                        }

                        // Actions
                        Button(onClick = {
                            scope.launch {
                                val res = LoansRepository.extendLoan(l.id)
                                if (res.isSuccess) snackbarHostState.showSnackbar("Berhasil diperpanjang")
                                else snackbarHostState.showSnackbar("Gagal: ${res.exceptionOrNull()?.localizedMessage}")
                            }
                        }, modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFE6B8))) {
                            Text("Perpanjang Peminjaman", color = Color(0xFF7A5A00), fontWeight = FontWeight.Bold)
                        }

                        Button(onClick = {
                            scope.launch {
                                val res = LoansRepository.returnBook(l.id, l.bookId)
                                if (res.isSuccess) {
                                    snackbarHostState.showSnackbar("Buku dikembalikan")
                                    // navigate to status screen showing returned timeline
                                    navController.navigate("status_peminjaman/${l.id}")
                                } else {
                                    snackbarHostState.showSnackbar("Gagal: ${res.exceptionOrNull()?.localizedMessage}")
                                }
                            }
                        }, modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E8B57))) {
                            Text("Kembalikan Buku", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}