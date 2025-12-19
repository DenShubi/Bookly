package com.example.bookly.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.bookly.viewmodel.RequestBookViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestBookScreen(
    navController: NavController,
    viewModel: RequestBookViewModel
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Pesan Sukses
    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) {
            Toast.makeText(context, "Permintaan berhasil dikirim!", Toast.LENGTH_LONG).show()
            navController.popBackStack()
        }
    }

    // Launcher Gambar & File
    val coverPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { viewModel.onCoverSelected(it) }
    )
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { viewModel.onFileSelected(it) }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Request Buku Baru") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Info Box
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))) {
                Text(
                    "Ajukan buku yang belum tersedia. Admin akan meninjaunya.",
                    modifier = Modifier.padding(16.dp),
                    color = Color(0xFF2E7D32)
                )
            }

            // Form Input
            OutlinedTextField(
                value = state.title,
                onValueChange = { viewModel.onTitleChange(it) },
                label = { Text("Judul Buku") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.author,
                onValueChange = { viewModel.onAuthorChange(it) },
                label = { Text("Penulis") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.year,
                onValueChange = { if (it.all { c -> c.isDigit() }) viewModel.onYearChange(it) },
                label = { Text("Tahun Terbit") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.reason,
                onValueChange = { viewModel.onReasonChange(it) },
                label = { Text("Alasan Request") },
                modifier = Modifier.fillMaxWidth().height(100.dp),
                maxLines = 4
            )

            // Tombol Upload
            Button(
                onClick = { coverPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = if(state.coverUri!=null) Color.Gray else MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Image, null); Spacer(Modifier.width(8.dp))
                Text(if (state.coverUri != null) "Cover Terpilih" else "Upload Cover (Wajib)")
            }

            OutlinedButton(
                onClick = { filePicker.launch(arrayOf("application/pdf")) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.AttachFile, null); Spacer(Modifier.width(8.dp))
                Text(if (state.supportingFileUri != null) "File Terpilih" else "Upload PDF (Opsional)")
            }

            // Pesan Error
            if (state.errorMessage != null) {
                Text(state.errorMessage!!, color = Color.Red)
            }

            // Tombol Kirim
            Button(
                onClick = { viewModel.submitRequest(context) },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = !state.isLoading
            ) {
                if (state.isLoading) CircularProgressIndicator(color = Color.White) else Text("Kirim Request")
            }
        }
    }
}