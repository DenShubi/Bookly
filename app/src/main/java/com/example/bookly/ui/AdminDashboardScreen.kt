package com.example.bookly.ui
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.bookly.supabase.BooksRepository
import com.example.bookly.viewmodel.AdminDashboardViewModel
import java.util.Locale
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    navController: NavController,
    viewModel: AdminDashboardViewModel = viewModel()
) {
    var activeTab by remember { mutableStateOf("buku") }
    var showLogoutDialog by remember { mutableStateOf(false) }
    // Logout confirmation dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Konfirmasi Logout") },
            text = { Text("Apakah Anda yakin ingin keluar?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        navController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color(0xFFCC0707)
                    )
                ) {
                    Text("Logout")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
    Scaffold(
        topBar = {
            AdminTopBar(
                title = "Admin Dashboard",
                onBack = { navController.navigateUp() }
            )
        },
        bottomBar = {
            AdminBottomNavigation(
                activeTab = activeTab,
                onNavigate = { tab -> activeTab = tab },
                onLogout = { showLogoutDialog = true }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(paddingValues)
        ) {
            when (activeTab) {
                "buku" -> AdminBooksTab(navController, viewModel)
                "denda" -> ComingSoonScreen("Denda")
                "verifikasi" -> ComingSoonScreen("Verifikasi")
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminTopBar(
    title: String,
    onBack: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF329A71)
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.White
        ),
        modifier = Modifier.shadow(elevation = 2.dp)
    )
}
@Composable
fun AdminBottomNavigation(
    activeTab: String,
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(78.dp),
        color = Color.White,
        shadowElevation = 8.dp,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 40.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AdminNavItem(
                icon = Icons.Default.Book,
                label = "Buku",
                isActive = activeTab == "buku",
                onClick = { onNavigate("buku") }
            )
            AdminNavItem(
                icon = Icons.Default.Receipt,
                label = "Denda",
                isActive = activeTab == "denda",
                onClick = { onNavigate("denda") }
            )
            AdminNavItem(
                icon = Icons.Default.VerifiedUser,
                label = "Verifikasi",
                isActive = activeTab == "verifikasi",
                onClick = { onNavigate("verifikasi") }
            )
            AdminNavItem(
                icon = Icons.AutoMirrored.Filled.ExitToApp,
                label = "Logout",
                isActive = false,
                onClick = onLogout
            )
        }
    }
}
@Composable
fun AdminNavItem(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(60.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive) Color(0xFF329A71) else Color(0xFF828282),
                modifier = Modifier.size(24.dp)
            )
        }
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isActive) Color(0xFF329A71) else Color(0xFF828282),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
@Composable
fun ComingSoonScreen(feature: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Construction,
                contentDescription = "Coming Soon",
                modifier = Modifier.size(64.dp),
                tint = Color(0xFF828282)
            )
            Text(
                text = "Coming Soon",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF383131)
            )
            Text(
                text = "Fitur $feature sedang dalam pengembangan",
                fontSize = 14.sp,
                color = Color(0xFF828282)
            )
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminBooksTab(
    navController: NavController,
    viewModel: AdminDashboardViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    var bookToDelete by remember { mutableStateOf<BooksRepository.BookRow?>(null) }
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current

    LaunchedEffect(Unit) {
        navController.currentBackStackEntry
            ?.savedStateHandle
            ?.getLiveData<Boolean>("BOOKS_UPDATED")
            ?.observe(lifecycleOwner) { updated ->
                if (updated == true) {
                    viewModel.loadBooks()
                    navController.currentBackStackEntry
                        ?.savedStateHandle
                        ?.remove<Boolean>("BOOKS_UPDATED")
                }
            }
    }
    // Delete confirmation dialog
    if (bookToDelete != null) {
        AlertDialog(
            onDismissRequest = { bookToDelete = null },
            title = { Text("Konfirmasi Hapus") },
            text = { Text("Apakah Anda yakin ingin menghapus buku \"${bookToDelete?.title}\"?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        bookToDelete?.let { book ->
                            viewModel.deleteBook(book.id)
                        }
                        bookToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color(0xFFCC0707)
                    )
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { bookToDelete = null }) {
                    Text("Batal")
                }
            }
        )
    }
    // Error dialog
    if (uiState.errorMessage != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text("Error") },
            text = { Text(uiState.errorMessage ?: "") },
            confirmButton = {
                TextButton(onClick = { viewModel.clearError() }) {
                    Text("OK")
                }
            }
        )
    }

    PullToRefreshBox(
        isRefreshing = uiState.isLoading,
        onRefresh = { viewModel.loadBooks() },
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Header with Add Button
            item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Kelola Buku",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Text(
                        text = "${uiState.books.size} buku tersedia",
                        fontSize = 14.sp,
                        color = Color(0xFF828282)
                    )
                }
                Button(
                    onClick = { navController.navigate("admin_add_book") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF329A71)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                    modifier = Modifier.shadow(
                        elevation = 4.dp,
                        shape = RoundedCornerShape(12.dp)
                    )
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add",
                        modifier = Modifier.size(20.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Tambah",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        }
        // Loading state
        if (uiState.isLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFF329A71)
                    )
                }
            }
        }
        // Empty state
        if (!uiState.isLoading && uiState.books.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 60.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Belum ada buku",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF828282)
                    )
                    Text(
                        text = "Klik tombol \"Tambah\" untuk menambahkan buku baru",
                        fontSize = 14.sp,
                        color = Color(0xFFBDBDBD),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        // Books list
        items(uiState.books, key = { it.id }) { book ->
            BookItemCard(
                book = book,
                onEdit = { navController.navigate("admin_edit_book/${book.id}") },
                onDelete = { bookToDelete = book }
            )
        }
    }
    }
}

@Composable
fun BookItemCard(
    book: BooksRepository.BookRow,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Book Cover
            AsyncImage(
                model = book.coverImageUrl,
                contentDescription = book.title,
                modifier = Modifier
                    .width(85.dp)
                    .height(120.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .shadow(
                        elevation = 4.dp,
                        shape = RoundedCornerShape(10.dp)
                    ),
                contentScale = ContentScale.Crop
            )
            // Book Info
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Title and Author
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = book.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = book.author,
                        fontSize = 12.sp,
                        color = Color(0xFF828282)
                    )
                }
                // Category and Rating
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Category badge
                    Surface(
                        color = Color(book.categoryColor?.let {
                            android.graphics.Color.parseColor(it)
                        } ?: 0xFFE0E0E0.toInt()),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = book.category ?: "",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Black,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    // Rating
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = "Rating",
                            modifier = Modifier.size(12.dp),
                            tint = Color(0xFFCC9600)
                        )
                        Text(
                            text = String.format(Locale.US, "%.1f", book.rating),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF383131)
                        )
                    }
                }
                // Stock Info
                Text(
                    text = "Stok: ${book.availableCopies}/${book.totalCopies}",
                    fontSize = 12.sp,
                    color = Color(0xFF828282)
                )
                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Edit Button
                    Button(
                        onClick = onEdit,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE7A93B).copy(alpha = 0.34f),
                            contentColor = Color(0xFFE7A93B)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Edit",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    // Delete Button
                    Button(
                        onClick = onDelete,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFCC0707).copy(alpha = 0.1f),
                            contentColor = Color(0xFFCC0707)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Hapus",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
