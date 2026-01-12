import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.mehchow.letyoucook.data.model.AuthResponse
import com.mehchow.letyoucook.ui.viewmodel.AuthUiState
import com.mehchow.letyoucook.ui.viewmodel.AuthViewModel

@Composable
fun AuthScreen(onLoginSuccess: (AuthResponse) -> Unit) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity

    // hiltViewModel() automatically gets the Hilt-managed AuthViewModel
    val viewModel: AuthViewModel = hiltViewModel()
    // collect the current UI state from viewmodel, read-only
    val uiState by viewModel.uiState.collectAsState(initial = AuthUiState.Idle)

    // when we enter success state, call onLoginSuccess exactly once
    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Success) {
            val authResponse = (uiState as AuthUiState.Success).authResponse
            Log.d("AuthScreen", "Login success, navigating with user: ${authResponse.username}")
            onLoginSuccess(authResponse)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (uiState) {
            is AuthUiState.Idle, is AuthUiState.Error -> {
                if (uiState is AuthUiState.Error) {
                    Text(text = (uiState as AuthUiState.Error).message)
                }

                Button(
                    enabled = activity != null,
                    onClick = {
                        if (activity == null) return@Button
                        viewModel.onGoogleSignIn(activity)
                    }
                ) {
                    Text("Sign in with Google")
                }
            }

            is AuthUiState.Loading -> {
                CircularProgressIndicator()
            }

            is AuthUiState.Success -> {
                Text("Logging you in...")
            }
        }
    }
}