package com.example.bookly.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.bookly.supabase.BooksRepository
import com.example.bookly.supabase.FinesRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinePaymentDetailScreen(navController: NavController, fineId: String?) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var isLoading by remember { mutableStateOf(true) }
    var fine by remember { mutableStateOf<FinesRepository.FineRow?>(null) }
    var book by remember { mutableStateOf<BooksRepository.BookRow?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    var uploadedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uploadedImageUri = uri
    }

    LaunchedEffect(fineId) {
        if (fineId.isNullOrBlank()) {
            error = "Invalid fine ID"
            isLoading = false
            return@LaunchedEffect
        }

        isLoading = true
        try {
            val fineResult = FinesRepository.getFineById(fineId)
            if (fineResult.isSuccess) {
                fine = fineResult.getOrNull()
                fine?.bookId?.let { bookId ->
                    val bookResult = BooksRepository.getBookById(bookId)
                    if (bookResult.isSuccess) {
                        book = bookResult.getOrNull()
                    }
                }
            } else {
                error = fineResult.exceptionOrNull()?.message ?: "Failed to load fine"
            }
        } catch (t: Throwable) {
            error = t.message
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
                    Text(
                        text = "Detail Pembayaran Denda",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF329A71)
                    )
                }
                Divider(color = Color.LightGray.copy(alpha = 0.5f), thickness = 1.dp)
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.White
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = Color(0xFF329A71)
                    )
                }
                error != null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = error ?: "Error", color = Color.Red)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { navController.navigateUp() }) {
                            Text("Kembali")
                        }
                    }
                }
                fine != null -> {
                    FinePaymentDetailContent(
                        fine = fine!!,
                        book = book,
                        uploadedImageUri = uploadedImageUri,
                        onImagePick = { imagePickerLauncher.launch("image/*") },
                        onSubmit = {
                            scope.launch {
                                isSubmitting = true
                                try {
                                    val uri = uploadedImageUri
                                    if (uri == null) {
                                        snackbarHostState.showSnackbar("Harap upload bukti pembayaran")
                                        isSubmitting = false
                                        return@launch
                                    }

                                    // Read image bytes from URI
                                    val inputStream = context.contentResolver.openInputStream(uri)
                                    val imageBytes = inputStream?.readBytes()
                                    inputStream?.close()

                                    if (imageBytes == null) {
                                        snackbarHostState.showSnackbar("Gagal membaca gambar")
                                        isSubmitting = false
                                        return@launch
                                    }

                                    val result = FinesRepository.submitPaymentProof(
                                        fineId = fine!!.id,
                                        imageBytes = imageBytes,
                                        amountPaid = fine!!.amount
                                    )

                                    if (result.isSuccess) {
                                        snackbarHostState.showSnackbar("Bukti pembayaran berhasil dikirim")
                                        // Refresh fine data
                                        val updatedFine = FinesRepository.getFineById(fine!!.id)
                                        if (updatedFine.isSuccess) {
                                            fine = updatedFine.getOrNull()
                                        }
                                        uploadedImageUri = null
                                    } else {
                                        snackbarHostState.showSnackbar(
                                            "Gagal: ${result.exceptionOrNull()?.message}"
                                        )
                                    }
                                } catch (t: Throwable) {
                                    snackbarHostState.showSnackbar("Error: ${t.message}")
                                }
                                isSubmitting = false
                            }
                        },
                        isSubmitting = isSubmitting
                    )
                }
            }
        }
    }
}

@Composable
private fun FinePaymentDetailContent(
    fine: FinesRepository.FineRow,
    book: BooksRepository.BookRow?,
    uploadedImageUri: Uri?,
    onImagePick: () -> Unit,
    onSubmit: () -> Unit,
    isSubmitting: Boolean
) {
    val wibTimeZone = TimeZone.getTimeZone("Asia/Jakarta")
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale("id")).apply {
        timeZone = wibTimeZone
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Status Badge
        FinePaymentStatusBadge(status = fine.status)

        // Book Card
        BookInfoCard(book = book, fineId = fine.id)

        // Fine Details
        FineDetailsCard(fine = fine, dateFormat = dateFormat)

        // Payment Information
        PaymentInfoCard()

        // Upload Section - Only show if unpaid or rejected
        if (fine.status == "unpaid" || fine.status == "rejected") {
            UploadProofCard(
                uploadedImageUri = uploadedImageUri,
                paymentProofUrl = fine.paymentProofUrl,
                onImagePick = onImagePick,
                onSubmit = onSubmit,
                isSubmitting = isSubmitting
            )
        }

        // Status Messages
        when (fine.status) {
            "pending" -> PendingVerificationCard(paymentProofUrl = fine.paymentProofUrl)
            "rejected" -> RejectedPaymentCard(note = fine.verificationNote)
            "paid" -> PaidConfirmationCard()
        }
    }
}

@Composable
private fun FinePaymentStatusBadge(status: String) {
    val (bgColor, textColor, icon, label) = when (status) {
        "unpaid" -> Quad(
            Color(0xFFCC0707).copy(alpha = 0.1f),
            Color(0xFFCC0707),
            Icons.Default.Warning,
            "BELUM DIBAYAR"
        )
        "pending" -> Quad(
            Color(0xFFE7A93B).copy(alpha = 0.1f),
            Color(0xFFE7A93B),
            Icons.Default.Schedule,
            "MENUNGGU VERIFIKASI"
        )
        "paid" -> Quad(
            Color(0xFF329A71).copy(alpha = 0.1f),
            Color(0xFF329A71),
            Icons.Default.CheckCircle,
            "LUNAS"
        )
        "rejected" -> Quad(
            Color(0xFFCC0707).copy(alpha = 0.1f),
            Color(0xFFCC0707),
            Icons.Default.Cancel,
            "DITOLAK"
        )
        else -> Quad(
            Color.Gray.copy(alpha = 0.1f),
            Color.Gray,
            Icons.Default.Help,
            "STATUS TIDAK DIKETAHUI"
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = null, tint = textColor, modifier = Modifier.size(24.dp))
            Text(
                text = label,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = textColor
            )
        }
    }
}

@Composable
private fun BookInfoCard(book: BooksRepository.BookRow?, fineId: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Book Cover
            Box(
                modifier = Modifier
                    .width(85.dp)
                    .height(120.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFF0F0F0))
            ) {
                book?.coverImageUrl?.let { url ->
                    Image(
                        painter = rememberAsyncImagePainter(url),
                        contentDescription = "Book Cover",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            // Book Info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = book?.title ?: "Unknown Book",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 2
                    )
                    Text(
                        text = book?.author ?: "Unknown Author",
                        fontSize = 11.sp,
                        color = Color(0xFF828282)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "ID Denda",
                        fontSize = 8.sp,
                        color = Color(0xFF383131)
                    )
                    Text(
                        text = "#FIN${fineId.takeLast(6).uppercase()}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = Color(0xFF383131)
                    )
                }
            }
        }
    }
}

@Composable
private fun FineDetailsCard(fine: FinesRepository.FineRow, dateFormat: SimpleDateFormat) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.AttachMoney,
                    contentDescription = null,
                    tint = Color(0xFF329A71),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Rincian Denda",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Keterangan", fontSize = 12.sp, color = Color(0xFF828282))
                    Text(
                        text = fine.description,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.End
                    )
                }

                fine.dueDate?.let { dueDate ->
                    val parsedDate = remember(dueDate) {
                        try {
                            SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dueDate)
                        } catch (_: Exception) {
                            null
                        }
                    }

                    parsedDate?.let { date ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Tanggal Jatuh Tempo", fontSize = 12.sp, color = Color(0xFF828282))
                            Text(
                                text = dateFormat.format(date),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Divider(color = Color(0xFFE0E0E0), thickness = 1.dp)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Total Denda",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Rp ${String.format(Locale("id", "ID"), "%,.0f", fine.amount)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color(0xFFCC0707)
                    )
                }
            }
        }
    }
}

@Composable
private fun PaymentInfoCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF329A71).copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Informasi Pembayaran",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Bank BCA",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = Color(0xFF383131)
                )
                Text(
                    text = "No. Rekening: 1234567890",
                    fontSize = 11.sp,
                    color = Color(0xFF828282)
                )
                Text(
                    text = "a.n. Perpustakaan Bookly",
                    fontSize = 11.sp,
                    color = Color(0xFF828282)
                )
            }
        }
    }
}

@Composable
private fun UploadProofCard(
    uploadedImageUri: Uri?,
    paymentProofUrl: String?,
    onImagePick: () -> Unit,
    onSubmit: () -> Unit,
    isSubmitting: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Upload,
                    contentDescription = null,
                    tint = Color(0xFF329A71),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Upload Bukti Pembayaran",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            // Upload Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (uploadedImageUri != null) 250.dp else 180.dp)
                    .border(
                        width = 2.dp,
                        color = Color(0xFFD9D9D9),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF5F5F5))
                    .clickable { onImagePick() },
                contentAlignment = Alignment.Center
            ) {
                if (uploadedImageUri != null) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(uploadedImageUri),
                            contentDescription = "Payment Proof",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Fit
                        )
                        Text(
                            text = "Klik untuk mengganti gambar",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF329A71),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Upload,
                            contentDescription = null,
                            tint = Color(0xFF828282),
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "Klik atau tarik gambar ke sini",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF383131),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Format: JPG, PNG (Max 5MB)",
                            fontSize = 11.sp,
                            color = Color(0xFF828282),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Submit Button
            Button(
                onClick = onSubmit,
                enabled = uploadedImageUri != null && !isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF329A71),
                    disabledContainerColor = Color(0xFFD9D9D9)
                )
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Upload,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Kirim Bukti Pembayaran",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PendingVerificationCard(paymentProofUrl: String?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE7A93B).copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Status Verifikasi",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = Color(0xFFE7A93B)
            )
            Text(
                text = "Bukti pembayaran Anda sedang dalam proses verifikasi oleh admin. Mohon tunggu 1x24 jam.",
                fontSize = 11.sp,
                color = Color(0xFF383131)
            )

            paymentProofUrl?.let { url ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Bukti yang telah dikirim:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF383131)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Image(
                    painter = rememberAsyncImagePainter(url),
                    contentDescription = "Payment Proof",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFFD9D9D9), RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}

@Composable
private fun RejectedPaymentCard(note: String?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFCC0707).copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Pembayaran Ditolak",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = Color(0xFFCC0707)
            )
            Text(
                text = note ?: "Bukti pembayaran Anda ditolak oleh admin.",
                fontSize = 11.sp,
                color = Color(0xFF383131)
            )
            Text(
                text = "Silakan upload ulang bukti pembayaran yang benar.",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFCC0707)
            )
        }
    }
}

@Composable
private fun PaidConfirmationCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF329A71).copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF329A71),
                modifier = Modifier.size(64.dp)
            )
            Text(
                text = "Pembayaran Berhasil",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color(0xFF329A71)
            )
            Text(
                text = "Denda Anda telah lunas dan terverifikasi oleh admin.\nTerima kasih!",
                fontSize = 12.sp,
                color = Color(0xFF383131),
                textAlign = TextAlign.Center
            )
        }
    }
}

// Helper data class for status badge
private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

