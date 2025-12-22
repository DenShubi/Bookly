package com.example.bookly.ui.denda

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.bookly.viewmodel.PaymentViewModel
import java.io.InputStream

@Composable
fun DetailDendaScreen(
    navController: NavController,
    dendaId: String
) {
    val paymentViewModel: PaymentViewModel = viewModel()

    DetailDendaContent(
        dendaId = dendaId,
        viewModel = paymentViewModel,
        onBack = { navController.popBackStack() }
    )
}

@Composable
fun DetailDendaContent(
    dendaId: String,
    viewModel: PaymentViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        imageUri = uri
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F7F7))
    ) {

        // HEADER
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = "Detail Pembayaran Denda",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // STATUS
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
        ) {
            Text(
                text = "⏳ MENUNGGU VERIFIKASI",
                modifier = Modifier.padding(12.dp),
                color = Color(0xFFFF9800),
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(Modifier.height(16.dp))

        // INFO BUKU
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Laskar Pelangi", fontWeight = FontWeight.Bold)
                Text("Andrea Hirata", fontSize = 12.sp, color = Color.Gray)
                Spacer(Modifier.height(8.dp))
                Text("ID Denda: #$dendaId", fontSize = 11.sp, color = Color.Gray)
            }
        }

        Spacer(Modifier.height(16.dp))

        // RINCIAN DENDA
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Rincian Denda", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                InfoRow("Keterangan", "Keterlambatan 1 hari")
                InfoRow("Tanggal Jatuh Tempo", "16 Des 2025")
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Rp 1.000",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Red
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // INFO PEMBAYARAN
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Informasi Pembayaran", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text("Bank BCA")
                Text("No. Rekening: 1234567890")
                Text("a.n. Perpustakaan Bookly")
            }
        }

        Spacer(Modifier.height(20.dp))

        // PREVIEW IMAGE
        imageUri?.let {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Image(
                    painter = rememberAsyncImagePainter(it),
                    contentDescription = "Bukti Pembayaran",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentScale = ContentScale.Crop
                )
            }
            Spacer(Modifier.height(12.dp))
        }

        // PICK IMAGE
        Button(
            onClick = { launcher.launch("image/*") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(50.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(if (imageUri == null) "Pilih Bukti Pembayaran" else "Ganti Bukti Pembayaran")
        }

        Spacer(Modifier.height(12.dp))

        // UPLOAD BUTTON
        Button(
            onClick = {
                imageUri?.let {
                    isLoading = true
                    val bytes = uriToByteArray(context, it)

                    viewModel.uploadBuktiPembayaran(
                        dendaId = dendaId,
                        imageBytes = bytes,
                        onSuccess = {
                            isLoading = false
                            message = "Upload berhasil"
                        },
                        onError = {
                            isLoading = false
                            message = it
                        }
                    )
                }
            },
            enabled = imageUri != null && !isLoading,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(50.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Upload Bukti", color = Color.White)
        }

        if (isLoading) {
            Spacer(Modifier.height(12.dp))
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }

        message?.let {
            Spacer(Modifier.height(12.dp))
            Text(
                text = it,
                color = Color.Green,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 12.sp, color = Color.Gray)
        Text(value, fontSize = 12.sp)
    }
}

fun uriToByteArray(context: Context, uri: Uri): ByteArray {
    val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
    return inputStream?.readBytes() ?: ByteArray(0)
}

@Preview(showBackground = true)
@Composable
fun DetailDendaPreview() {
    MaterialTheme {
        // PREVIEW TANPA VIEWMODEL (AMAN)
        Text("Preview Detail Denda")
    }
}
