package com.example.bookly.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.bookly.supabase.FinesRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPaymentVerificationScreen(navController: NavController) {
    var isLoading by remember { mutableStateOf(true) }
    var submissions by remember { mutableStateOf<List<FinesRepository.PaymentSubmissionWithDetails>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedSubmission by remember { mutableStateOf<FinesRepository.PaymentSubmissionWithDetails?>(null) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    fun loadSubmissions() {
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                val result = FinesRepository.getAllPendingSubmissions()
                if (result.isSuccess) {
                    submissions = result.getOrNull() ?: emptyList()
                } else {
                    errorMessage = result.exceptionOrNull()?.message ?: "Gagal memuat data"
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Terjadi kesalahan"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadSubmissions()
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.Black)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Verifikasi Pembayaran",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF329A71)
                    )
                }
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f), thickness = 1.dp)
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.White
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = isLoading,
            onRefresh = { loadSubmissions() },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.VerifiedUser,
                                contentDescription = "Verification",
                                tint = Color(0xFF329A71),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Pengajuan Pembayaran",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                        Text(
                            text = "${submissions.size} pengajuan menunggu verifikasi",
                            fontSize = 14.sp,
                            color = Color(0xFF828282)
                        )
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

                // Empty state
                if (!isLoading && submissions.isEmpty() && errorMessage == null) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 60.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "All verified",
                                modifier = Modifier.size(64.dp),
                                tint = Color(0xFF329A71)
                            )
                            Text(
                                text = "Semua Sudah Terverifikasi",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF828282)
                            )
                            Text(
                                text = "Tidak ada pengajuan pembayaran yang menunggu",
                                fontSize = 14.sp,
                                color = Color(0xFFBDBDBD),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Submissions list
                items(submissions) { submission ->
                    PaymentSubmissionCard(
                        submission = submission,
                        onClick = { selectedSubmission = submission }
                    )
                }
            }
        }
    }

    // Verification Dialog
    selectedSubmission?.let { submission ->
        VerificationDialog(
            submission = submission,
            onDismiss = { selectedSubmission = null },
            onVerify = { approved, note ->
                scope.launch {
                    try {
                        val result = FinesRepository.verifyPaymentSubmission(
                            submissionId = submission.submission.id,
                            approved = approved,
                            notes = note
                        )
                        if (result.isSuccess) {
                            snackbarHostState.showSnackbar(
                                if (approved) "Pembayaran diterima" else "Pembayaran ditolak"
                            )
                            selectedSubmission = null
                            loadSubmissions()
                        } else {
                            snackbarHostState.showSnackbar(
                                "Gagal: ${result.exceptionOrNull()?.message}"
                            )
                        }
                    } catch (e: Exception) {
                        snackbarHostState.showSnackbar("Error: ${e.message}")
                    }
                }
            }
        )
    }
}

@Composable
private fun PaymentSubmissionCard(
    submission: FinesRepository.PaymentSubmissionWithDetails,
    onClick: () -> Unit
) {
    val wibTimeZone = TimeZone.getTimeZone("Asia/Jakarta")
    val dateFormat = SimpleDateFormat("dd MMM yyyy HH:mm", Locale("id")).apply {
        timeZone = wibTimeZone
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // User info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = submission.userName ?: "Unknown User",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Text(
                        text = submission.userEmail ?: "",
                        fontSize = 11.sp,
                        color = Color(0xFF828282)
                    )
                }

                Surface(
                    color = Color(0xFFE7A93B).copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "PENDING",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE7A93B),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Book info
            Text(
                text = submission.bookTitle ?: "Unknown Book",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Fine info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Jumlah Denda",
                        fontSize = 11.sp,
                        color = Color(0xFF828282)
                    )
                    Text(
                        text = "Rp ${String.format(Locale("id", "ID"), "%,.0f", submission.submission.amountPaid)}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFCC0707)
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Tanggal Pengajuan",
                        fontSize = 11.sp,
                        color = Color(0xFF828282)
                    )

                    val submissionDateText = remember(submission.submission.submissionDate) {
                        try {
                            val subDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
                                timeZone = TimeZone.getTimeZone("UTC")
                            }.parse(submission.submission.submissionDate ?: "")
                            dateFormat.format(subDate ?: Date()) + " WIB"
                        } catch (e: Exception) {
                            "-"
                        }
                    }

                    Text(
                        text = submissionDateText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action hint
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tap untuk verifikasi",
                    fontSize = 11.sp,
                    color = Color(0xFF329A71),
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Color(0xFF329A71),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun VerificationDialog(
    submission: FinesRepository.PaymentSubmissionWithDetails,
    onDismiss: () -> Unit,
    onVerify: (approved: Boolean, note: String) -> Unit
) {
    var note by remember { mutableStateOf("") }
    var showRejectDialog by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Verifikasi Pembayaran",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider()

                // User & Book Info
                Text(
                    text = submission.userName ?: "Unknown User",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = submission.bookTitle ?: "Unknown Book",
                    fontSize = 13.sp,
                    color = Color(0xFF828282)
                )

                // Amount
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Jumlah Denda:", fontSize = 13.sp, color = Color(0xFF828282))
                    Text(
                        text = "Rp ${String.format(Locale("id", "ID"), "%,.0f", submission.submission.amountPaid)}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFCC0707)
                    )
                }

                // Payment Proof Image
                Text(
                    text = "Bukti Pembayaran:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )

                submission.submission.paymentProofUrl?.let { url ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 200.dp, max = 400.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF5F5F5))
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(url),
                            contentDescription = "Payment Proof",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                }

                HorizontalDivider()

                // Note field for rejection
                if (showRejectDialog) {
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Catatan Penolakan") },
                        placeholder = { Text("Masukkan alasan penolakan...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5
                    )
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Reject Button
                    OutlinedButton(
                        onClick = {
                            if (showRejectDialog) {
                                onVerify(false, note)
                            } else {
                                showRejectDialog = true
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFCC0707)
                        )
                    ) {
                        Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Tolak")
                    }

                    // Approve Button
                    Button(
                        onClick = { onVerify(true, note) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF329A71)
                        )
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Terima")
                    }
                }
            }
        }
    }
}

