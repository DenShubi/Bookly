
package com.example.bookly.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import com.example.bookly.supabase.LoansRepository
import java.text.SimpleDateFormat
import java.util.*
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController

// Colors
private val PeminjamanGreen = Color(0xFF2E8B57)
private val AktifBg = Color(0xFFE8F4EE)
private val AktifText = Color(0xFF2E8B57)
private val TerlambatBg = Color(0xFFFDECEC)
private val TerlambatText = Color(0xFFC62828)
private val DendaBg = Color(0xFFFFF6E5)
private val DendaText = Color(0xFFD7A300)
private val EmptyStateIconTint = Color(0xFFC7C7C7)
private val EmptyStateSubtitleText = Color(0xFF8A8A8A)

@Composable
fun PeminjamanScreen(navController: NavController) {
    Scaffold(
        topBar = { PeminjamanTopAppBar(navController) },
        bottomBar = { BottomNavigationBar(navController = navController, selected = "peminjaman") },
        containerColor = Color.White
    ) { paddingValues ->
        val loansState = remember { mutableStateOf<List<LoansRepository.LoanRow>>(emptyList()) }
        val loading = remember { mutableStateOf(true) }

        LaunchedEffect(Unit) {
            val res = LoansRepository.getUserLoans()
            if (res.isSuccess) loansState.value = res.getOrNull() ?: emptyList()
            loading.value = false
        }

        val loans = loansState.value
        val aktifCount = loans.count { it.status == "active" }
        val overdueCount = loans.count { it.status == "overdue" }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            SummaryCardsRow(aktifCount.toString(), overdueCount.toString(), "0")
            Spacer(modifier = Modifier.height(16.dp))

            // Section header
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp)) {
                Icon(imageVector = Icons.Outlined.MenuBook, contentDescription = null, tint = PeminjamanGreen)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Sedang Dipinjam (${aktifCount})", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.height(12.dp))

            if (loading.value) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                val activeLoans = loans.filter { it.status == "active" }
                if (activeLoans.isEmpty()) {
                    EmptyPeminjamanState()
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(activeLoans) { loan ->
                            LoanItem(loan = loan, onClick = { navController.navigate("peminjamanScreen1/${loan.id}") })
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PeminjamanTopAppBar(navController: NavController) {
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
                text = "Peminjaman",
                color = PeminjamanGreen,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Divider(color = Color.LightGray.copy(alpha = 0.5f), thickness = 1.dp, modifier = Modifier.shadow(1.dp))
    }
}

@Composable
private fun SummaryCardsRow(bukuAktif: String = "0", terlambat: String = "0", denda: String = "0") {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SummaryCard(title = "Buku Aktif", value = bukuAktif, backgroundColor = AktifBg, contentColor = AktifText, modifier = Modifier.weight(1f))
        SummaryCard(title = "Terlambat", value = terlambat, backgroundColor = TerlambatBg, contentColor = TerlambatText, modifier = Modifier.weight(1f))
        SummaryCard(title = "Denda (Rp)", value = denda, backgroundColor = DendaBg, contentColor = DendaText, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun LoanItem(loan: LoansRepository.LoanRow, onClick: () -> Unit) {
    val fmtDate = SimpleDateFormat("dd MMM yyyy", Locale("id"))
    val fmtTime = SimpleDateFormat("HH:mm", Locale("id"))
    val today = Date()
    val due = loan.returnDeadline ?: today
    val daysRemaining = ((due.time - today.time) / (1000 * 60 * 60 * 24)).toInt()
    // determine if time-of-day is meaningful (non-midnight) in Asia/Jakarta
    val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("Asia/Jakarta"))
    cal.time = due
    val hour = cal.get(java.util.Calendar.HOUR_OF_DAY)
    val minute = cal.get(java.util.Calendar.MINUTE)
    val timeStr = if (hour != 0 || minute != 0) fmtTime.format(due) + " WIB" else null

    Card(modifier = Modifier
        .fillMaxWidth()
        .clickable { onClick() }, shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // thumbnail
                Box(modifier = Modifier
                    .size(64.dp)
                    .background(color = Color(0xFFF0F0F0), shape = RoundedCornerShape(8.dp)))

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(text = loan.bookTitle.orEmpty(), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = loan.bookAuthor.orEmpty(), fontSize = 13.sp, color = GreyText)
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(modifier = Modifier.background(color = Color(0xFFE8F4EE), shape = RoundedCornerShape(12.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                        Text(text = "Novel", color = PeminjamanGreen, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Outlined.CalendarToday, contentDescription = null, tint = PeminjamanGreen)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Jatuh tempo:", color = GreyText)
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "${fmtDate.format(due)}${timeStr?.let { " • $it" } ?: ""} • ${if (daysRemaining >= 0) "$daysRemaining hari lagi" else "${-daysRemaining} hari terlambat"}", color = PeminjamanGreen)
            }
        }
    }
}

@Composable
private fun SummaryCard(title: String, value: String, backgroundColor: Color, contentColor: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(80.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = value, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = contentColor)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = title, fontSize = 14.sp, color = GreyText)
        }
    }
}

@Composable
private fun EmptyPeminjamanState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.MenuBook,
                contentDescription = "No Books",
                modifier = Modifier.size(88.dp),
                tint = EmptyStateIconTint
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Belum Ada Peminjaman",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Anda belum meminjam buku apapun.\nMulai pinjam buku dari katalog!",
                fontSize = 14.sp,
                color = EmptyStateSubtitleText,
                textAlign = TextAlign.Center
            )
        }
    }
}


@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PeminjamanScreenPreview() {
    PeminjamanScreen(navController = rememberNavController())
}
