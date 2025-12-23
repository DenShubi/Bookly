
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
import androidx.compose.runtime.rememberCoroutineScope
import com.example.bookly.supabase.LoansRepository
import kotlinx.coroutines.launch
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
    val scope = rememberCoroutineScope()
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
        val now = Date()

        // Separate loans into active and overdue
        val activeLoans = loans.filter { loan ->
            val dueDate = loan.returnDeadline ?: now
            loan.status == "active" && dueDate.time >= now.time
        }
        val overdueLoans = loans.filter { loan ->
            val dueDate = loan.returnDeadline ?: now
            loan.status == "overdue" || (loan.status == "active" && dueDate.time < now.time)
        }

        // Calculate total fines
        val totalFines = overdueLoans.sumOf { it.lateFee ?: 0.0 }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                SummaryCardsRow(
                    aktifCount = activeLoans.size.toString(),
                    terlambatCount = overdueLoans.size.toString(),
                    denda = String.format(Locale.US, "%.0f", totalFines)
                )
                Spacer(modifier = Modifier.height(16.dp))

                if (loading.value) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = if (totalFines > 0) 80.dp else 0.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                    // Active Loans Section
                    if (activeLoans.isNotEmpty()) {
                        item {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(start = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.MenuBook,
                                    contentDescription = null,
                                    tint = PeminjamanGreen
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Sedang Dipinjam (${activeLoans.size})",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                        items(activeLoans) { loan ->
                            LoanItem(
                                loan = loan,
                                onClick = { navController.navigate("peminjamanScreen1/${loan.id}") }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(8.dp)) }
                    }

                    // Overdue Loans Section
                    if (overdueLoans.isNotEmpty()) {
                        item {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.MenuBook,
                                    contentDescription = null,
                                    tint = TerlambatText
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Terlambat (${overdueLoans.size})",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = TerlambatText
                                )
                            }
                        }
                        items(overdueLoans) { loan ->
                            OverdueLoanItem(
                                loan = loan,
                                navController = navController
                            )
                        }
                    }

                    // Empty state
                    if (activeLoans.isEmpty() && overdueLoans.isEmpty()) {
                        item {
                            EmptyPeminjamanState()
                        }
                    }
                }
            }
        }

        // Floating "Bayar Denda" button at the bottom
        if (totalFines > 0 && !loading.value) {
            Button(
                onClick = {
                    scope.launch {
                        // Create or get a consolidated fine for all overdue loans
                        val firstOverdueLoan = overdueLoans.firstOrNull()
                        if (firstOverdueLoan != null) {
                            // Check if a consolidated fine already exists for the first loan
                            val fineResult = com.example.bookly.supabase.FinesRepository.getFineByBorrowingRecordId(firstOverdueLoan.id)
                            val existingFine = fineResult.getOrNull()

                            if (existingFine != null) {
                                // Navigate to existing fine
                                navController.navigate("fine_payment_detail/${existingFine.id}")
                            } else {
                                // Create a consolidated fine for all overdue books
                                val bookTitles = overdueLoans.mapNotNull { it.bookTitle }.take(3).joinToString(", ")
                                val moreBooks = if (overdueLoans.size > 3) " dan ${overdueLoans.size - 3} buku lainnya" else ""
                                val description = "Denda keterlambatan untuk: $bookTitles$moreBooks"

                                val createResult = com.example.bookly.supabase.FinesRepository.createOrUpdateFineForLoan(
                                    borrowingRecordId = firstOverdueLoan.id,
                                    bookId = firstOverdueLoan.bookId,
                                    amount = totalFines,
                                    description = description
                                )
                                val createdFine = createResult.getOrNull()
                                if (createdFine != null) {
                                    navController.navigate("fine_payment_detail/${createdFine.id}")
                                }
                            }
                        }
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TerlambatText
                ),
                shape = RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 8.dp,
                    pressedElevation = 12.dp
                )
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Bayar Denda: Rp ${String.format(Locale.US, "%,.0f", totalFines)}",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
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
private fun SummaryCardsRow(aktifCount: String = "0", terlambatCount: String = "0", denda: String = "0") {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SummaryCard(title = "Buku Aktif", value = aktifCount, backgroundColor = AktifBg, contentColor = AktifText, modifier = Modifier.weight(1f))
        SummaryCard(title = "Terlambat", value = terlambatCount, backgroundColor = TerlambatBg, contentColor = TerlambatText, modifier = Modifier.weight(1f))
        SummaryCard(title = "Denda (Rp)", value = denda, backgroundColor = DendaBg, contentColor = DendaText, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun LoanItem(loan: LoansRepository.LoanRow, onClick: () -> Unit) {
    // Use WIB timezone for displaying dates
    val wibTimeZone = TimeZone.getTimeZone("Asia/Jakarta")
    val fmtDate = SimpleDateFormat("dd MMM yyyy", Locale("id")).apply {
        timeZone = wibTimeZone
    }
    val fmtTime = SimpleDateFormat("HH:mm", Locale("id")).apply {
        timeZone = wibTimeZone
    }
    val today = Date()
    val due = loan.returnDeadline ?: today

    // Calculate remaining time in milliseconds
    val remainingMillis = due.time - today.time
    val daysRemaining = (remainingMillis / (1000 * 60 * 60 * 24)).toInt()
    val hoursRemaining = (remainingMillis / (1000 * 60 * 60)).toInt()
    val minutesRemaining = (remainingMillis / (1000 * 60)).toInt()

    // Determine time display - show hours/minutes if less than 1 day
    val timeRemainingText = when {
        daysRemaining >= 1 -> "$daysRemaining hari lagi"
        hoursRemaining > 0 -> "$hoursRemaining jam lagi"
        minutesRemaining > 0 -> "$minutesRemaining menit lagi"
        else -> "Jatuh tempo"
    }

    // determine if time-of-day is meaningful (non-midnight) in Asia/Jakarta
    val cal = Calendar.getInstance(wibTimeZone)
    cal.time = due
    val hour = cal.get(Calendar.HOUR_OF_DAY)
    val minute = cal.get(Calendar.MINUTE)
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
                Text(text = "${fmtDate.format(due)}${timeStr?.let { " • $it" } ?: ""} • $timeRemainingText", color = PeminjamanGreen)
            }
        }
    }
}

@Composable
private fun OverdueLoanItem(loan: LoansRepository.LoanRow, navController: NavController) {
    val scope = rememberCoroutineScope()
    // Use WIB timezone for displaying dates
    val wibTimeZone = TimeZone.getTimeZone("Asia/Jakarta")
    val fmtDate = SimpleDateFormat("dd MMM yyyy", Locale("id")).apply {
        timeZone = wibTimeZone
    }
    val fmtTime = SimpleDateFormat("HH:mm", Locale("id")).apply {
        timeZone = wibTimeZone
    }
    val today = Date()
    val due = loan.returnDeadline ?: today

    // Calculate overdue time
    val overdueMillis = today.time - due.time
    val daysOverdue = (overdueMillis / (1000 * 60 * 60 * 24)).toInt()
    val hoursOverdue = (overdueMillis / (1000 * 60 * 60)).toInt()
    val minutesOverdue = (overdueMillis / (1000 * 60)).toInt()

    // Determine time display - show hours/minutes if less than 1 day
    val overdueText = when {
        daysOverdue >= 1 -> "$daysOverdue hari terlambat"
        hoursOverdue > 0 -> "$hoursOverdue jam terlambat"
        else -> "$minutesOverdue menit terlambat"
    }

    // determine if time-of-day is meaningful
    val cal = Calendar.getInstance(wibTimeZone)
    cal.time = due
    val hour = cal.get(Calendar.HOUR_OF_DAY)
    val minute = cal.get(Calendar.MINUTE)
    val timeStr = if (hour != 0 || minute != 0) fmtTime.format(due) + " WIB" else null

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // thumbnail
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(color = Color(0xFFF0F0F0), shape = RoundedCornerShape(8.dp))
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = loan.bookTitle.orEmpty(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = loan.bookAuthor.orEmpty(), fontSize = 13.sp, color = GreyText)
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .background(color = TerlambatBg, shape = RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Terlambat",
                            color = TerlambatText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Due date info
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.CalendarToday,
                    contentDescription = null,
                    tint = TerlambatText
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Jatuh tempo:", color = GreyText, fontSize = 13.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${fmtDate.format(due)}${timeStr?.let { " • $it" } ?: ""} • $overdueText",
                    color = TerlambatText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Late fee info - display only, no button
            if (loan.lateFee != null && loan.lateFee!! > 0.0) {
                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = Color.LightGray.copy(alpha = 0.3f), thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Denda:",
                        fontSize = 12.sp,
                        color = GreyText
                    )
                    Text(
                        text = "Rp ${String.format(Locale.US, "%.0f", loan.lateFee)}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TerlambatText
                    )
                }
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
