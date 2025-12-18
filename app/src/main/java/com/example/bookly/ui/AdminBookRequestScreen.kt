package com.example.bookly.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
// These specific imports are required for 'by remember' delegation
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.bookly.supabase.BookRequest
import com.example.bookly.supabase.BookRequestRepository
import com.example.bookly.supabase.SupabaseClientProvider
import kotlinx.coroutines.launch

@Composable
fun AdminBookRequestScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { BookRequestRepository(SupabaseClientProvider.client) }

    var requests by remember { mutableStateOf<List<BookRequest>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Dialog States
    var showDialog by remember { mutableStateOf(false) }
    var selectedRequest by remember { mutableStateOf<BookRequest?>(null) }
    // FIXED: Removed invalid union type syntax <"approve" | "reject">
    var reviewAction by remember { mutableStateOf("approve") }
    var reviewNote by remember { mutableStateOf("") }

    val primaryColor = Color(0xFF329A71)

    fun loadRequests() {
        scope.launch {
            isLoading = true
            val result = repository.getRequestsForAdmin()
            if (result.isSuccess) {
                requests = result.getOrDefault(emptyList())
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadRequests()
    }

    fun handleStatusUpdate() {
        if (reviewAction == "reject" && reviewNote.isBlank()) {
            Toast.makeText(context, "Mohon berikan alasan penolakan", Toast.LENGTH_SHORT).show()
            return
        }

        selectedRequest?.let { req ->
            scope.launch {
                val status = if (reviewAction == "approve") "approved" else "rejected"
                val result = repository.updateStatus(req.id, status, reviewNote)
                if (result.isSuccess) {
                    Toast.makeText(context, "Status berhasil diperbarui", Toast.LENGTH_SHORT).show()
                    loadRequests()
                    showDialog = false
                    reviewNote = ""
                } else {
                    Toast.makeText(context, "Gagal: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = {
                Text(
                    if (reviewAction == "approve") "Setujui Permintaan" else "Tolak Permintaan",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text("${selectedRequest?.title} - ${selectedRequest?.author}", fontSize = 13.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = reviewNote,
                        onValueChange = { reviewNote = it },
                        label = { Text("Catatan Admin") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { handleStatusUpdate() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (reviewAction == "approve") primaryColor else Color(0xFFCC0707)
                    )
                ) {
                    Text(if (reviewAction == "approve") "Setujui" else "Tolak")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Batal", color = Color.Gray)
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = primaryColor)
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 20.dp, bottom = 20.dp)
            ) {
                // Summary Stats
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        StatCard("Menunggu", requests.count { it.status == "pending" }, Color(0xFFE7A93B))
                        StatCard("Disetujui", requests.count { it.status == "approved" }, primaryColor)
                        StatCard("Ditolak", requests.count { it.status == "rejected" }, Color(0xFFCC0707))
                    }
                }

                if (requests.isEmpty()) {
                    item {
                        Text("Belum ada permintaan", modifier = Modifier.fillMaxWidth().padding(top = 40.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = Color.Gray)
                    }
                }

                items(requests) { req ->
                    RequestCard(
                        request = req,
                        primaryColor = primaryColor,
                        onApprove = {
                            selectedRequest = req
                            reviewAction = "approve"
                            showDialog = true
                        },
                        onReject = {
                            selectedRequest = req
                            reviewAction = "reject"
                            showDialog = true
                        },
                        onAddToCatalog = {
                            // Navigate to Add Book (AdminAddBookScreen)
                            navController.navigate("admin_add_book")
                            Toast.makeText(context, "Silakan input manual data", Toast.LENGTH_LONG).show()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun RowScope.StatCard(label: String, count: Int, color: Color) {
    Column(
        modifier = Modifier
            .weight(1f)
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(count.toString(), fontWeight = FontWeight.Bold, fontSize = 24.sp, color = color)
        Text(label, fontSize = 10.sp, color = Color.Black)
    }
}

@Composable
fun RequestCard(
    request: BookRequest,
    primaryColor: Color,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onAddToCatalog: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AsyncImage(
                    model = request.coverImageUrl,
                    contentDescription = null,
                    modifier = Modifier.size(60.dp, 80.dp).clip(RoundedCornerShape(8.dp)).background(Color.Gray),
                    contentScale = ContentScale.Crop
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(request.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("${request.author} • ${request.publicationYear}", fontSize = 12.sp, color = Color.Gray)

                    Spacer(modifier = Modifier.height(4.dp))
                    StatusBadge(request.status)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFFF0F0F0))
            Spacer(modifier = Modifier.height(8.dp))

            Text("Alasan:", fontSize = 11.sp, color = Color.Gray)
            Text(request.reason, fontSize = 12.sp)

            if (request.reviewNote != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier.background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp)).padding(8.dp).fillMaxWidth()) {
                    Column {
                        Text("Catatan Admin:", fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                        Text(request.reviewNote, fontSize = 12.sp)
                    }
                }
            }

            // Actions
            if (request.status == "pending") {
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onApprove,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Setujui", fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = onReject,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFCC0707)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCC0707)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Tolak", fontSize = 12.sp)
                    }
                }
            } else if (request.status == "approved") {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onAddToCatalog,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Tambahkan ke Katalog", fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val (bgColor, textColor, text) = when (status) {
        "pending" -> Triple(Color(0xFFE7A93B).copy(alpha = 0.15f), Color(0xFFE7A93B), "Menunggu Review")
        "approved" -> Triple(Color(0xFF329A71).copy(alpha = 0.15f), Color(0xFF329A71), "Disetujui")
        else -> Triple(Color(0xFFCC0707).copy(alpha = 0.15f), Color(0xFFCC0707), "Ditolak")
    }

    Box(modifier = Modifier.background(bgColor, RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 2.dp)) {
        Text(text, color = textColor, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
}