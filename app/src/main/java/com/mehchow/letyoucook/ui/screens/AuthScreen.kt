package com.mehchow.letyoucook.ui.screens

import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.mehchow.letyoucook.BuildConfig
import com.mehchow.letyoucook.R
import com.mehchow.letyoucook.data.model.AuthResponse
import com.mehchow.letyoucook.ui.viewmodel.AuthUiState
import com.mehchow.letyoucook.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(onLoginSuccess: (AuthResponse) -> Unit) {
    // This is the "Stateful" version that keeps your Hilt logic
    val viewModel: AuthViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState(initial = AuthUiState.Idle)
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val coroutineScope = rememberCoroutineScope()

    // Keep your LaunchedEffect here
    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Success) {
            onLoginSuccess((uiState as AuthUiState.Success).authResponse)
        }

        if (uiState is AuthUiState.Error) {
            Toast.makeText(
                context,
                (uiState as AuthUiState.Error).message,
                Toast.LENGTH_SHORT
            ).show()

            viewModel.resetState()
        }
    }

    // Call the "Stateless" version below
    AuthScreenContent(
        uiState = uiState,
        onSignInClick = {
            activity?.let { act ->
                coroutineScope.launch {
                    try {
                        val credentialManager = CredentialManager.create(act)
                        val googleIdOption = GetGoogleIdOption.Builder()
                            .setFilterByAuthorizedAccounts(false)
                            .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
                            .setAutoSelectEnabled(true)
                            .build()

                        val request = GetCredentialRequest.Builder()
                            .addCredentialOption(googleIdOption)
                            .build()

                        Log.d("AuthScreen", "Requesting Google credentials...")
                        val result = credentialManager.getCredential(act, request)
                        val credential = result.credential

                        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                        val idToken = googleIdTokenCredential.idToken
                        Log.d("AuthScreen", "Google token received: ${idToken.take(10)}")

                        // Pass only the token to ViewModel
                        viewModel.authenticateWithBackend(idToken)
                    } catch(e: GetCredentialCancellationException) {
                        Log.d("AuthScreen", "User cancelled")
                    } catch (e: Exception) {
                        Log.e("AuthScreen", "Google Sign-in failed", e)
                        Toast.makeText(
                            context,
                            "Google Sign-in failed: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    )
}

@Composable
fun AuthScreenContent(
    uiState: AuthUiState,
    onSignInClick: () -> Unit
) {
    Scaffold(
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFEFE6B5))
                .padding(innerPadding),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                AppIcon()

                AppTitle()

                GoogleLoginButton(
                    uiState = uiState,
                    action = onSignInClick
                )
            }
        }
    }
}

@Composable
fun AppIcon() {
    Box(
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.myicon),
            contentDescription = "Auth screen icon",
            modifier = Modifier
                .size(200.dp)
                .clip(CircleShape)
        )
    }
}

@Composable
fun AppTitle() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Let you cook",
            color = Color.Black,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            "Everyone can be a chef\uD83E\uDD0C\uD83C\uDFFB",
            color = Color.Gray,
            fontSize = 14.sp,
        )
    }
}

@Composable
fun GoogleLoginButton(
    action: () -> Unit,
    uiState: AuthUiState
) {
    val isLoading = uiState is AuthUiState.Loading

    Spacer(modifier = Modifier.height(10.dp))

    Button(
        onClick = action,
        enabled = !isLoading,
        modifier = Modifier
            .width(240.dp)
            .height(50.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White,      // Color when enabled
            contentColor = Color.Black,        // Text/Icon color when enabled
            disabledContainerColor = Color.White, // THIS FIXES IT: Keep it white while loading
            disabledContentColor = Color.Black    // Keep the spinner black
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 4.dp,   // Shadow when idle
            pressedElevation = 8.dp,  // Shadow when clicked
            disabledElevation = 4.dp,
        ),
    ) {
        // 2. Use a Box to center the content perfectly
        Box(contentAlignment = Alignment.Center) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = Color.LightGray
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painterResource(R.drawable.google_icon),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Color.Unspecified
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Continue with Google",
                        style = TextStyle(fontWeight = FontWeight.Medium)
                    )
                }
            }
        }
    }
}