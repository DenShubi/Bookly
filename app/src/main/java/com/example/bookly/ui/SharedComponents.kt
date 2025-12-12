
package com.example.bookly.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController

val BottomNavGreen = Color(0xFF2E8B57)
val BottomNavInactive = Color(0xFF9E9E9E)
val GreyText = Color(0xFF666666)

@Composable
fun BottomNavigationBar(navController: NavController, selected: String) {
    NavigationBar(
        containerColor = Color.White,
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp),
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            selected = selected == "beranda",
            onClick = { if (selected != "beranda") navController.navigate("home") },
            icon = { Icon(Icons.Default.Home, contentDescription = "Beranda") },
            label = {
                Text(
                    text = "Beranda",
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = BottomNavGreen,
                unselectedIconColor = BottomNavInactive,
                selectedTextColor = BottomNavGreen,
                unselectedTextColor = BottomNavInactive,
                indicatorColor = Color.Transparent
            ),
            modifier = Modifier.weight(1f)
        )
        NavigationBarItem(
            selected = selected == "buku",
            onClick = { if (selected != "buku") navController.navigate("katalog_buku") },
            icon = { Icon(Icons.Default.Book, contentDescription = "Buku") },
            label = {
                Text(
                    text = "Buku",
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = BottomNavGreen,
                unselectedIconColor = BottomNavInactive,
                selectedTextColor = BottomNavGreen,
                unselectedTextColor = BottomNavInactive,
                indicatorColor = Color.Transparent
            ),
            modifier = Modifier.weight(1f)
        )
        NavigationBarItem(
            selected = selected == "peminjaman",
            onClick = {
                if (selected != "peminjaman") {
                    navController.navigate("peminjaman") {
                        launchSingleTop = true
                    }
                }
            },
            icon = { Icon(Icons.Default.SwapHoriz, contentDescription = "Peminjaman") },
            label = {
                Text(
                    text = "Peminjaman",
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = BottomNavGreen,
                unselectedIconColor = BottomNavInactive,
                selectedTextColor = BottomNavGreen,
                unselectedTextColor = BottomNavInactive,
                indicatorColor = Color.Transparent
            ),
            modifier = Modifier.weight(1f)
        )
        NavigationBarItem(
            selected = selected == "wishlist",
            onClick = {
                if (selected != "wishlist") {
                    navController.navigate("wishlist") {
                        launchSingleTop = true
                    }
                }
            },
            icon = {
                if (selected == "wishlist") Icon(Icons.Filled.Favorite, contentDescription = "Wishlist")
                else Icon(Icons.Outlined.FavoriteBorder, contentDescription = "Wishlist")
            },
            label = {
                Text(
                    text = "Wishlist",
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = BottomNavGreen,
                unselectedIconColor = BottomNavInactive,
                selectedTextColor = BottomNavGreen,
                unselectedTextColor = BottomNavInactive,
                indicatorColor = Color.Transparent
            ),
            modifier = Modifier.weight(1f)
        )
        NavigationBarItem(
            selected = selected == "profile",
            onClick = {
                if (selected != "profile") {
                    navController.navigate("profile") {
                        launchSingleTop = true
                    }
                }
            },
            icon = { Icon(Icons.Default.Person, contentDescription = "Profil") },
            label = {
                Text(
                    text = "Profil",
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = BottomNavGreen,
                unselectedIconColor = BottomNavInactive,
                selectedTextColor = BottomNavGreen,
                unselectedTextColor = BottomNavInactive,
                indicatorColor = Color.Transparent
            ),
            modifier = Modifier.weight(1f)
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
fun BottomNavigationBarPreview() {
    BottomNavigationBar(navController = rememberNavController(), selected = "wishlist")
}
