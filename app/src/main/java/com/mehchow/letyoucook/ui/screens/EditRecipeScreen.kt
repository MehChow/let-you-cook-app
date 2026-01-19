package com.mehchow.letyoucook.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.mehchow.letyoucook.data.model.RecipeVisibility
import com.mehchow.letyoucook.ui.viewmodel.EditRecipeFormData
import com.mehchow.letyoucook.ui.viewmodel.EditRecipeUiState
import com.mehchow.letyoucook.ui.viewmodel.EditRecipeViewModel
import com.mehchow.letyoucook.ui.viewmodel.EditableImage
import com.mehchow.letyoucook.ui.viewmodel.EditableStep
import com.mehchow.letyoucook.ui.viewmodel.IngredientForm

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditRecipeScreen(
    onBackClick: () -> Unit,
    onSuccess: () -> Unit,
    viewModel: EditRecipeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Handle update success
    LaunchedEffect(uiState) {
        if (uiState is EditRecipeUiState.Ready && (uiState as EditRecipeUiState.Ready).submitSuccess) {
            onSuccess()
        }
    }

    // Show error in snackbar
    LaunchedEffect(uiState) {
        val state = uiState as? EditRecipeUiState.Ready
        state?.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Edit Recipe",
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
        when (val state = uiState) {
            is EditRecipeUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            is EditRecipeUiState.Ready -> {
                EditRecipeContent(
                    state = state,
                    viewModel = viewModel,
                    modifier = Modifier.padding(paddingValues)
                )
            }
            
            is EditRecipeUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Failed to load recipe",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onBackClick) {
                            Text("Go Back")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditRecipeContent(
    state: EditRecipeUiState.Ready,
    viewModel: EditRecipeViewModel,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Step indicator
        EditStepIndicator(
            currentStep = state.currentStep,
            totalSteps = state.totalSteps,
            stepTitles = state.stepTitles,
            onStepClick = { viewModel.goToStep(it) }
        )

        // Loading indicator
        if (state.isUploading || state.isSubmitting) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        // Step content
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (state.currentStep) {
                0 -> EditBasicInfoStep(
                    formData = state.formData,
                    onTitleChange = viewModel::updateTitle,
                    onDescriptionChange = viewModel::updateDescription,
                    onVisibilityChange = viewModel::updateVisibility
                )
                1 -> EditImagesStep(
                    images = state.formData.images,
                    onAddImages = viewModel::addImages,
                    onRemoveImage = viewModel::removeImage
                )
                2 -> EditIngredientsStep(
                    ingredients = state.formData.ingredients,
                    onAddIngredient = viewModel::addIngredient,
                    onRemoveIngredient = viewModel::removeIngredient,
                    onUpdateIngredient = viewModel::updateIngredient
                )
                3 -> EditStepsStep(
                    steps = state.formData.steps,
                    onAddStep = viewModel::addStep,
                    onRemoveStep = viewModel::removeStep,
                    onUpdateStepDescription = viewModel::updateStepDescription,
                    onUpdateStepImage = viewModel::updateStepImage,
                    onRemoveStepImage = viewModel::removeStepImage
                )
                4 -> EditReminderStep(
                    reminder = state.formData.reminder,
                    onReminderChange = viewModel::updateReminder
                )
                5 -> EditReviewStep(
                    formData = state.formData,
                    isSubmitting = state.isSubmitting || state.isUploading
                )
            }
        }

        // Navigation buttons
        EditNavigationButtons(
            state = state,
            onPrevious = viewModel::previousStep,
            onNext = {
                if (state.currentStep == state.totalSteps - 1) {
                    viewModel.submitRecipe()
                } else {
                    viewModel.nextStep()
                }
            }
        )
    }
}

@Composable
private fun EditStepIndicator(
    currentStep: Int,
    totalSteps: Int,
    stepTitles: List<String>,
    onStepClick: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            repeat(totalSteps) { index ->
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            if (index <= currentStep) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .clickable { onStepClick(index) },
                    contentAlignment = Alignment.Center
                ) {
                    if (index < currentStep) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Text(
                            text = "${index + 1}",
                            color = if (index <= currentStep) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }

                if (index < totalSteps - 1) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(2.dp)
                            .align(Alignment.CenterVertically)
                            .padding(horizontal = 4.dp)
                            .background(
                                if (index < currentStep) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stepTitles.getOrElse(currentStep) { "" },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}

// ==================== STEP 1: BASIC INFO ====================

@Composable
private fun EditBasicInfoStep(
    formData: EditRecipeFormData,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onVisibilityChange: (RecipeVisibility) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            OutlinedTextField(
                value = formData.title,
                onValueChange = onTitleChange,
                label = { Text("Recipe Title *") },
                placeholder = { Text("e.g., Grandma's Apple Pie") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                )
            )
        }

        item {
            OutlinedTextField(
                value = formData.description,
                onValueChange = onDescriptionChange,
                label = { Text("Description (optional)") },
                placeholder = { Text("Tell us about your recipe...") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences
                )
            )
        }

        item {
            Text(
                text = "Visibility",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = formData.visibility == RecipeVisibility.PUBLIC,
                    onClick = { onVisibilityChange(RecipeVisibility.PUBLIC) },
                    label = { Text("Public") }
                )
                FilterChip(
                    selected = formData.visibility == RecipeVisibility.PRIVATE,
                    onClick = { onVisibilityChange(RecipeVisibility.PRIVATE) },
                    label = { Text("Private") }
                )
            }
        }
    }
}

// ==================== STEP 2: IMAGES ====================

@Composable
private fun EditImagesStep(
    images: List<EditableImage>,
    onAddImages: (List<Uri>) -> Unit,
    onRemoveImage: (String) -> Unit
) {
    val multiplePhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
    ) { uris ->
        if (uris.isNotEmpty()) {
            onAddImages(uris)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Add at least 1 image (up to 10) for your recipe carousel *",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            OutlinedButton(
                onClick = {
                    multiplePhotoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = images.size < 10
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Images (${images.size}/10)")
            }
        }

        if (images.isNotEmpty()) {
            item {
                Text(
                    text = "First image will be the cover",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    itemsIndexed(images, key = { _, image -> image.id }) { index, image ->
                        EditableImageThumbnail(
                            image = image,
                            isCover = index == 0,
                            onRemove = { onRemoveImage(image.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EditableImageThumbnail(
    image: EditableImage,
    isCover: Boolean,
    onRemove: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(120.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = if (isCover) 2.dp else 0.dp,
                color = if (isCover) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        // Display image based on type
        when (image) {
            is EditableImage.Existing -> {
                AsyncImage(
                    model = image.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            is EditableImage.New -> {
                AsyncImage(
                    model = image.uri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Cover badge
        if (isCover) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(4.dp)
                    .background(
                        MaterialTheme.colorScheme.primary,
                        RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "Cover",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }

        // Remove button
        IconButton(
            onClick = onRemove,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(32.dp)
                .padding(4.dp)
                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// ==================== STEP 3: INGREDIENTS ====================

@Composable
private fun EditIngredientsStep(
    ingredients: List<IngredientForm>,
    onAddIngredient: () -> Unit,
    onRemoveIngredient: (String) -> Unit,
    onUpdateIngredient: (String, String?, String?, String?) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "List all ingredients needed",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        itemsIndexed(ingredients, key = { _, item -> item.id }) { index, ingredient ->
            EditIngredientItem(
                ingredient = ingredient,
                index = index + 1,
                canRemove = ingredients.size > 1,
                onRemove = { onRemoveIngredient(ingredient.id) },
                onNameChange = { onUpdateIngredient(ingredient.id, it, null, null) },
                onQuantityChange = { onUpdateIngredient(ingredient.id, null, it, null) },
                onUnitChange = { onUpdateIngredient(ingredient.id, null, null, it) }
            )
        }

        item {
            OutlinedButton(
                onClick = onAddIngredient,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Ingredient")
            }
        }
    }
}

@Composable
private fun EditIngredientItem(
    ingredient: IngredientForm,
    index: Int,
    canRemove: Boolean,
    onRemove: () -> Unit,
    onNameChange: (String) -> Unit,
    onQuantityChange: (String) -> Unit,
    onUnitChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ingredient $index",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
                if (canRemove) {
                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Remove",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            OutlinedTextField(
                value = ingredient.name,
                onValueChange = onNameChange,
                label = { Text("Name *") },
                placeholder = { Text("e.g., Flour") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = ingredient.quantity,
                    onValueChange = onQuantityChange,
                    label = { Text("Quantity *") },
                    placeholder = { Text("e.g., 2 cups") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = ingredient.unit,
                    onValueChange = onUnitChange,
                    label = { Text("Unit") },
                    placeholder = { Text("optional") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
        }
    }
}

// ==================== STEP 4: STEPS ====================

@Composable
private fun EditStepsStep(
    steps: List<EditableStep>,
    onAddStep: () -> Unit,
    onRemoveStep: (String) -> Unit,
    onUpdateStepDescription: (String, String) -> Unit,
    onUpdateStepImage: (String, Uri?) -> Unit,
    onRemoveStepImage: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Add step-by-step instructions",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        itemsIndexed(steps, key = { _, item -> item.id }) { index, step ->
            EditStepItem(
                step = step,
                index = index + 1,
                canRemove = steps.size > 1,
                onRemove = { onRemoveStep(step.id) },
                onDescriptionChange = { onUpdateStepDescription(step.id, it) },
                onImageChange = { onUpdateStepImage(step.id, it) },
                onRemoveImage = { onRemoveStepImage(step.id) }
            )
        }

        item {
            OutlinedButton(
                onClick = onAddStep,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Step")
            }
        }
    }
}

@Composable
private fun EditStepItem(
    step: EditableStep,
    index: Int,
    canRemove: Boolean,
    onRemove: () -> Unit,
    onDescriptionChange: (String) -> Unit,
    onImageChange: (Uri?) -> Unit,
    onRemoveImage: () -> Unit
) {
    val singlePhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        onImageChange(uri)
    }

    // Determine which image to show
    val hasExistingImage = step.existingImageUrl != null && !step.removeExistingImage
    val hasNewImage = step.newImageUri != null
    val showImage = hasNewImage || hasExistingImage

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Step $index",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
                if (canRemove) {
                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Remove",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            OutlinedTextField(
                value = step.description,
                onValueChange = onDescriptionChange,
                label = { Text("Instructions *") },
                placeholder = { Text("Describe this step...") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4
            )

            // Image display
            if (showImage) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    AsyncImage(
                        model = step.newImageUri ?: step.existingImageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    IconButton(
                        onClick = onRemoveImage,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(32.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove image",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            } else {
                OutlinedButton(
                    onClick = {
                        singlePhotoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Image (optional)")
                }
            }
        }
    }
}

// ==================== STEP 5: REMINDER ====================

@Composable
private fun EditReminderStep(
    reminder: String,
    onReminderChange: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Add a reminder or tip for this recipe (optional)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            OutlinedTextField(
                value = reminder,
                onValueChange = onReminderChange,
                label = { Text("Reminder / Tips") },
                placeholder = { Text("e.g., Best served warm with vanilla ice cream") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 6
            )
        }
    }
}

// ==================== STEP 6: REVIEW ====================

@Composable
private fun EditReviewStep(
    formData: EditRecipeFormData,
    isSubmitting: Boolean
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Review your changes before saving",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Title & Description
        item {
            EditReviewSection(title = "Basic Info") {
                EditReviewItem(label = "Title", value = formData.title)
                if (formData.description.isNotBlank()) {
                    EditReviewItem(label = "Description", value = formData.description)
                }
                EditReviewItem(
                    label = "Visibility",
                    value = formData.visibility.name.lowercase().replaceFirstChar { it.uppercase() }
                )
            }
        }

        // Images
        item {
            EditReviewSection(title = "Images") {
                Text(
                    text = "${formData.images.size} image(s)",
                    style = MaterialTheme.typography.bodyMedium
                )
                if (formData.images.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(formData.images, key = { it.id }) { image ->
                            val model = when (image) {
                                is EditableImage.Existing -> image.imageUrl
                                is EditableImage.New -> image.uri
                            }
                            AsyncImage(
                                model = model,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                        }
                    }
                }
            }
        }

        // Ingredients
        item {
            val validIngredients = formData.ingredients.filter { 
                it.name.isNotBlank() && it.quantity.isNotBlank() 
            }
            EditReviewSection(title = "Ingredients (${validIngredients.size})") {
                validIngredients.forEach { ingredient ->
                    val text = buildString {
                        append("• ${ingredient.name}")
                        append(" - ${ingredient.quantity}")
                        if (ingredient.unit.isNotBlank()) {
                            append(" ${ingredient.unit}")
                        }
                    }
                    Text(text = text, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        // Steps
        item {
            val validSteps = formData.steps.filter { it.description.isNotBlank() }
            EditReviewSection(title = "Steps (${validSteps.size})") {
                validSteps.forEachIndexed { index, step ->
                    Text(
                        text = "${index + 1}. ${step.description}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    val hasImage = (step.existingImageUrl != null && !step.removeExistingImage) || 
                                   step.newImageUri != null
                    if (hasImage) {
                        Text(
                            text = "   (with image)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }

        // Reminder
        if (formData.reminder.isNotBlank()) {
            item {
                EditReviewSection(title = "Reminder") {
                    Text(
                        text = formData.reminder,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // Submitting indicator
        if (isSubmitting) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Uploading and saving changes...")
                }
            }
        }
    }
}

@Composable
private fun EditReviewSection(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            content()
        }
    }
}

@Composable
private fun EditReviewItem(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

// ==================== NAVIGATION BUTTONS ====================

@Composable
private fun EditNavigationButtons(
    state: EditRecipeUiState.Ready,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Previous button
        if (state.canGoPrevious) {
            OutlinedButton(
                onClick = onPrevious,
                modifier = Modifier.weight(1f)
            ) {
                Text("Previous")
            }
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }

        // Next / Save button
        Button(
            onClick = onNext,
            enabled = state.canGoNext,
            modifier = Modifier.weight(1f)
        ) {
            if (state.isSubmitting || state.isUploading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text(
                    if (state.currentStep == state.totalSteps - 1) "Save Changes"
                    else "Next"
                )
            }
        }
    }
}
