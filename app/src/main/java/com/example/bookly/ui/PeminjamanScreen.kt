
package com.example.bookly.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            SummaryCardsRow()
            Spacer(modifier = Modifier.height(16.dp))
            EmptyPeminjamanState()
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
private fun SummaryCardsRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SummaryCard(title = "Buku Aktif", value = "0", backgroundColor = AktifBg, contentColor = AktifText, modifier = Modifier.weight(1f))
        SummaryCard(title = "Terlambat", value = "0", backgroundColor = TerlambatBg, contentColor = TerlambatText, modifier = Modifier.weight(1f))
        SummaryCard(title = "Denda (Rp)", value = "0", backgroundColor = DendaBg, contentColor = DendaText, modifier = Modifier.weight(1f))
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
