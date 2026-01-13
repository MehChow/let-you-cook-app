package com.mehchow.letyoucook.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mehchow.letyoucook.ui.viewmodel.HomeViewModel

@Composable
fun HomeScreen(
    username: String,
    viewModel: HomeViewModel = hiltViewModel()
) {
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Welcome, $username!", fontSize = 24.sp)

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = { viewModel.onLogoutClick() },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) {
                Text("Logout", color = Color.White)
            }
        }
    }
}