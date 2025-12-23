package com.example.bookly.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.bookly.supabase.LoansRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminFinesScreen() {
    var isLoading by remember { mutableStateOf(true) }
    var loansWithFines by remember { mutableStateOf<List<LoansRepository.LoanRow>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun loadFines() {
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                val result = LoansRepository.getAllOverdueLoans()
                if (result.isSuccess) {
                    loansWithFines = result.getOrNull()?.filter { (it.lateFee ?: 0.0) > 0.0 } ?: emptyList()
                } else {
                    errorMessage = result.exceptionOrNull()?.message ?: "Gagal memuat data denda"
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Terjadi kesalahan"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadFines()
    }

    PullToRefreshBox(
        isRefreshing = isLoading,
        onRefresh = { loadFines() },
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Receipt,
                            contentDescription = "Fines",
                            tint = Color(0xFF329A71),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Daftar Denda",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                    Text(
                        text = "${loansWithFines.size} peminjaman dengan denda",
                        fontSize = 14.sp,
                        color = Color(0xFF828282)
                    )
                    if (loansWithFines.isNotEmpty()) {
                        val totalFines = loansWithFines.sumOf { it.lateFee ?: 0.0 }
                        Text(
                            text = "Total: Rp ${String.format(Locale.US, "%.2f", totalFines)}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Red
                        )
                    }
                }
            }

            // Error message
            if (errorMessage != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            color = Color.Red,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }

            // Loading state
            if (isLoading && loansWithFines.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 60.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFF329A71))
                    }
                }
            }

            // Empty state
            if (!isLoading && loansWithFines.isEmpty() && errorMessage == null) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 60.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Receipt,
                            contentDescription = "No fines",
                            modifier = Modifier.size(64.dp),
                            tint = Color(0xFF828282)
                        )
                        Text(
                            text = "Tidak ada denda",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF828282)
                        )
                        Text(
                            text = "Semua peminjaman sudah dikembalikan tepat waktu",
                            fontSize = 14.sp,
                            color = Color(0xFFBDBDBD)
                        )
                    }
                }
            }

            // Fines list
            items(loansWithFines) { loan ->
                FineItemCard(loan = loan)
            }
        }
    }
}

@Composable
fun FineItemCard(loan: LoansRepository.LoanRow) {
    // Use WIB timezone for displaying dates
    val wibTimeZone = TimeZone.getTimeZone("Asia/Jakarta")
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale("id")).apply {
        timeZone = wibTimeZone
    }
    val today = Date()
    val returnDeadline = loan.returnDeadline ?: today
    val daysOverdue = ((today.time - returnDeadline.time) / (1000 * 60 * 60 * 24)).toInt()
    val hoursOverdue = ((today.time - returnDeadline.time) / (1000 * 60 * 60)).toInt()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Book Cover
                AsyncImage(
                    model = loan.coverImageUrl,
                    contentDescription = loan.bookTitle,
                    modifier = Modifier
                        .width(70.dp)
                        .height(100.dp)
                        .clip(RoundedCornerShape(8.dp))
                )

                // Book Info
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = loan.bookTitle ?: "Unknown",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Text(
                        text = loan.bookAuthor ?: "Unknown",
                        fontSize = 12.sp,
                        color = Color(0xFF828282)
                    )

                    // Status badge
                    Surface(
                        color = Color(0xFFFFEBEE),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = loan.status?.uppercase() ?: "OVERDUE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Red,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Text(
                        text = "ID: #${loan.id.take(8)}",
                        fontSize = 11.sp,
                        color = Color(0xFF828282)
                    )
                }
            }

            Divider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = Color.LightGray.copy(alpha = 0.3f)
            )

            // Fine details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Batas Kembali",
                        fontSize = 11.sp,
                        color = Color(0xFF828282)
                    )
                    Text(
                        text = sdf.format(returnDeadline),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "Keterlambatan",
                        fontSize = 11.sp,
                        color = Color(0xFF828282)
                    )
                    Text(
                        text = if (daysOverdue > 0) "$daysOverdue hari" else "$hoursOverdue jam",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Red
                    )
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "Denda",
                        fontSize = 11.sp,
                        color = Color(0xFF828282)
                    )
                    Text(
                        text = "Rp ${String.format(Locale.US, "%.2f", loan.lateFee ?: 0.0)}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Red
                    )
                }
            }
        }
    }
}

