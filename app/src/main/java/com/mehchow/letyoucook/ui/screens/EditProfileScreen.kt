package com.mehchow.letyoucook.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.mehchow.letyoucook.util.ImageCropper
import com.mehchow.letyoucook.util.rememberImageCropperState
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.mehchow.letyoucook.ui.viewmodel.EditProfileUiState
import com.mehchow.letyoucook.ui.viewmodel.EditProfileViewModel
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    onBackClick: () -> Unit,
    onSaveSuccess: () -> Unit,
    viewModel: EditProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Handle save success
    LaunchedEffect(uiState) {
        if (uiState is EditProfileUiState.Ready && (uiState as EditProfileUiState.Ready).saveSuccess) {
            onSaveSuccess()
        }
    }
    
    // Show error in snackbar
    LaunchedEffect(uiState) {
        val state = uiState as? EditProfileUiState.Ready
        state?.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Edit Profile",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is EditProfileUiState.Loading -> {
                    LoadingContent()
                }
                is EditProfileUiState.Ready -> {
                    EditProfileContent(
                        state = state,
                        onUsernameChange = viewModel::updateUsername,
                        onAvatarSelected = viewModel::setNewAvatar,
                        onRemoveAvatar = viewModel::removeAvatar,
                        onSave = viewModel::saveProfile,
                        snackbarHostState = snackbarHostState
                    )
                }
                is EditProfileUiState.Error -> {
                    ErrorContent(
                        message = state.message,
                        onBackClick = onBackClick
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EditProfileContent(
    state: EditProfileUiState.Ready,
    onUsernameChange: (String) -> Unit,
    onAvatarSelected: (Uri) -> String?,  // Returns error message or null
    onRemoveAvatar: () -> Unit,
    onSave: () -> Unit,
    snackbarHostState: SnackbarHostState
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // State for cropping mode
    var isCropping by remember { mutableStateOf(false) }
    var imageUriToCrop by remember { mutableStateOf<Uri?>(null) }

    // Create cropper state that will be used in the ImageCropper composable
    val cropperState = rememberImageCropperState(imageUriToCrop?.toString())

    // Simple image picker launcher - select from gallery then crop
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { pickedUri ->
            // Store the URI and show cropper
            imageUriToCrop = pickedUri
            isCropping = true
        }
    }
    
    // Handle cropping completion
    fun onCropDone() {
        scope.launch {
            try {
                // Check if we have an image to crop
                if (imageUriToCrop == null) {
                    return@launch
                }

                // Get the cropped bitmap directly from the cropper
                val croppedBitmap = cropperState.cropper?.getCroppedImage(512, 512)

                if (croppedBitmap != null) {
                    try {
                        // Create a temporary file for the cropped image
                        val croppedFile = File(
                            context.cacheDir,
                            "cropped_avatar_${System.currentTimeMillis()}.jpg"
                        )

                        // Save the bitmap to file
                        FileOutputStream(croppedFile).use { out ->
                            croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                        }

                        // Check if file was created successfully
                        if (croppedFile.exists() && croppedFile.length() > 0) {
                            // Use the cropped image
                            val croppedUri = Uri.fromFile(croppedFile)

                            // Call the callback to update the avatar
                            val error = onAvatarSelected(croppedUri)
                            if (error != null) {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Failed to apply cropped image")
                                }
                            } else {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Image cropped successfully!")
                                }
                            }
                        } else {
                            scope.launch {
                                snackbarHostState.showSnackbar("Failed to save cropped image")
                            }
                        }

                        // Exit cropping mode
                        isCropping = false
                        imageUriToCrop = null
                    } catch (e: Exception) {
                        scope.launch {
                            snackbarHostState.showSnackbar("Failed to save cropped image")
                        }
                    } finally {
                        // Clean up bitmap
                        croppedBitmap.recycle()
                    }
                } else {
                    scope.launch {
                        snackbarHostState.showSnackbar("Failed to crop image")
                    }
                }
            } catch (e: Exception) {
                // Handle crop error
                scope.launch {
                    snackbarHostState.showSnackbar("Failed to crop image")
                }
            }
        }
    }

    fun onCropCancel() {
        isCropping = false
        imageUriToCrop = null
    }

    // Show error from image selection
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
        }
    }
    
    if (isCropping) {
        // Show cropping interface
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Cropping view
            Box(modifier = Modifier.weight(1f)) {
                if (imageUriToCrop != null) {
                    ImageCropper(
                        state = cropperState,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Show loading or error if no image to crop
                    Text("Loading image...")
                }
            }

            // Crop controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = { onCropCancel() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = { onCropDone() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Crop & Use")
                }
            }
        }
    } else {
        // Show normal profile editing interface
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
        // Avatar section
        Text(
            text = "Profile Photo",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.align(Alignment.Start)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Avatar with edit overlay
        Box(
            contentAlignment = Alignment.Center
        ) {
                // Avatar image (automatically cropped to fit circular shape)
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                        .clickable {
                            imagePickerLauncher.launch("image/*")
                        },
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        // Show new avatar if selected (automatically cropped to fit circle)
                        state.newAvatarUri != null -> {
                            AsyncImage(
                                model = state.newAvatarUri,
                                contentDescription = "New profile picture",
                                contentScale = ContentScale.Crop, // Automatically crops to fit circular shape
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        // Show current avatar if not marked for removal (automatically cropped to fit circle)
                        state.currentAvatarUrl != null && !state.removeCurrentAvatar -> {
                            AsyncImage(
                                model = state.currentAvatarUrl,
                                contentDescription = "Current profile picture",
                                contentScale = ContentScale.Crop, // Automatically crops to fit circular shape
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        // Show placeholder (when no avatar or marked for removal)
                        else -> {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }
                
                // Upload overlay
                if (state.isUploading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            color = Color.White
                        )
                    }
                }
            }
            
            // Camera icon overlay
            if (!state.isUploading) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable {
                            imagePickerLauncher.launch("image/*")
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Change photo",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            // Remove avatar button (for both new and current avatars)
            if ((state.newAvatarUri != null || state.currentAvatarUrl != null) && !state.isUploading) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error)
                        .clickable { onRemoveAvatar() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove new photo",
                        tint = MaterialTheme.colorScheme.onError,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Tap to change photo",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = "Tap to select & crop profile image • Max 10MB • JPG, PNG, WebP",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Username field
        Text(
            text = "Username",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.align(Alignment.Start)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        OutlinedTextField(
            value = state.username,
            onValueChange = onUsernameChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Enter your username") },
            singleLine = true,
            isError = !state.isUsernameValid && state.username.isNotEmpty(),
            supportingText = {
                if (!state.isUsernameValid && state.username.isEmpty()) {
                    Text(
                        text = "Username cannot be empty",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            enabled = !state.isSaving && !state.isUploading
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Save button
        Button(
            onClick = onSave,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            enabled = state.canSave
        ) {
            if (state.isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "Save Changes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onBackClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Oops!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(onClick = onBackClick) {
            Text("Go Back")
        }
    }
}
