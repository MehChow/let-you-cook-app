package com.mehchow.letyoucook.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.mehchow.letyoucook.data.model.Ingredient
import com.mehchow.letyoucook.data.model.RecipeDetail
import com.mehchow.letyoucook.data.model.RecipeImage
import com.mehchow.letyoucook.data.model.RecipeStep
import com.mehchow.letyoucook.ui.viewmodel.RecipeDetailUiState
import com.mehchow.letyoucook.ui.viewmodel.RecipeDetailViewModel

/**
 * Recipe Detail Screen displaying full recipe information.
 *
 * @param onBackClick Called when back button is pressed
 * @param onCreatorClick Called when creator info is tapped
 * @param onEditClick Called when edit option is selected
 * @param onDeleteSuccess Called after successful deletion
 * @param viewModel Injected by Hilt
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeDetailScreen(
    onBackClick: () -> Unit,
    onCreatorClick: (Long) -> Unit,
    onEditClick: (Long) -> Unit = {},
    onDeleteSuccess: () -> Unit = {},
    onRecipeEdited: () -> Unit = {},  // Callback to clear the flag
    recipeEdited: Boolean = false,     // Flag from savedStateHandle
    viewModel: RecipeDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val deleteSuccess by viewModel.deleteSuccess.collectAsState()
    
    // Track dropdown menu state
    var showMenu by remember { mutableStateOf(false) }
    
    // Reload recipe data when returning from edit screen
    LaunchedEffect(recipeEdited) {
        if (recipeEdited) {
            viewModel.loadRecipeDetail()
            onRecipeEdited()  // Clear the flag
        }
    }
    
    // Navigate back after successful delete
    LaunchedEffect(deleteSuccess) {
        if (deleteSuccess) {
            viewModel.resetDeleteSuccess()
            onDeleteSuccess()
        }
    }
    
    // Get recipe ID from success state for edit navigation
    val recipeId = (uiState as? RecipeDetailUiState.Success)?.recipe?.id

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    // Show menu only for recipe owner
                    val successState = uiState as? RecipeDetailUiState.Success
                    if (successState?.isOwner == true) {
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "More options"
                                )
                            }
                            
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Edit recipe") },
                                    onClick = {
                                        showMenu = false
                                        recipeId?.let { onEditClick(it) }
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = null
                                        )
                                    }
                                )
                                DropdownMenuItem(
                                    text = { 
                                        Text(
                                            "Delete recipe",
                                            color = MaterialTheme.colorScheme.error
                                        ) 
                                    },
                                    onClick = {
                                        showMenu = false
                                        viewModel.showDeleteDialog()
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is RecipeDetailUiState.Loading -> {
                    LoadingContent()
                }

                is RecipeDetailUiState.Success -> {
                    RecipeDetailContent(
                        recipe = state.recipe,
                        isLikeLoading = state.isLikeLoading,
                        onLikeClick = { viewModel.toggleLike() },
                        onCreatorClick = onCreatorClick
                    )
                    
                    // Delete confirmation dialog
                    if (state.showDeleteDialog) {
                        DeleteConfirmationDialog(
                            recipeName = state.recipe.title,
                            onConfirm = { viewModel.deleteRecipe() },
                            onDismiss = { viewModel.hideDeleteDialog() }
                        )
                    }
                    
                    // Loading overlay while deleting
                    if (state.isDeleting) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color.White)
                        }
                    }
                }

                is RecipeDetailUiState.Error -> {
                    ErrorContent(
                        message = state.message,
                        onRetry = { viewModel.loadRecipeDetail() }
                    )
                }
            }
        }
    }
}

@Composable
private fun DeleteConfirmationDialog(
    recipeName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text("Delete Recipe")
        },
        text = {
            Text("Are you sure you want to delete \"$recipeName\"? This action cannot be undone.")
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
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
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "😕",
            fontSize = 64.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Failed to load recipe",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRetry) {
            Text("Try Again")
        }
    }
}

@Composable
private fun RecipeDetailContent(
    recipe: RecipeDetail,
    isLikeLoading: Boolean,
    onLikeClick: () -> Unit,
    onCreatorClick: (Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // Image Carousel
        item {
            ImageCarousel(
                images = recipe.images,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Title and Like Section
        item {
            TitleSection(
                title = recipe.title,
                likeCount = recipe.likeCount,
                isLiked = recipe.isLikedByCurrentUser,
                isLikeLoading = isLikeLoading,
                onLikeClick = onLikeClick,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
            )
        }

        // Creator Info
        item {
            CreatorSection(
                creatorId = recipe.creator.id,
                creatorName = recipe.creator.username,
                creatorAvatarUrl = recipe.creator.avatarUrl,
                onClick = { onCreatorClick(recipe.creator.id) },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // Description (if present)
        if (!recipe.description.isNullOrBlank()) {
            item {
                DescriptionSection(
                    description = recipe.description,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
        }

        // Ingredient Section
        item {
            IngredientSection(
                ingredients = recipe.ingredients,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        // Steps Section Header
        item {
            Text(
                text = "Steps",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }

        // Steps
        itemsIndexed(recipe.steps.sortedBy { it.stepNumber }) { index, step ->
            StepCard(
                step = step,
                stepIndex = index + 1,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }

        // Reminder Section (if present)
        if (!recipe.reminder.isNullOrBlank()) {
            item {
                ReminderSection(
                    reminder = recipe.reminder,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
        }
    }
}

// ==================== IMAGE CAROUSEL ====================

@Composable
private fun ImageCarousel(
    images: List<RecipeImage>,
    modifier: Modifier = Modifier
) {
    val sortedImages = images.sortedBy { it.displayOrder }

    if (sortedImages.isEmpty()) {
        // Placeholder when no images
        Box(
            modifier = modifier
                .aspectRatio(16f / 9f)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "🍳",
                fontSize = 64.sp
            )
        }
        return
    }

    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { sortedImages.size }
    )

    Column(modifier = modifier) {
        // Horizontal Pager for images
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
        ) { page ->
            AsyncImage(
                model = sortedImages[page].imageUrl,
                contentDescription = "Recipe image ${page + 1}",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Page indicators
        if (sortedImages.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(sortedImages.size) { index ->
                    val isSelected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (isSelected) 10.dp else 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                }
                            )
                    )
                }
            }
        }
    }
}

// ==================== TITLE SECTION ====================

@Composable
private fun TitleSection(
    title: String,
    likeCount: Int,
    isLiked: Boolean,
    isLikeLoading: Boolean,
    onLikeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )

        // Like button with count
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isLikeLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            } else {
                IconButton(onClick = onLikeClick) {
                    Icon(
                        imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (isLiked) "Unlike" else "Like",
                        tint = if (isLiked) Color.Red else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            Text(
                text = formatCount(likeCount),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ==================== CREATOR SECTION ====================

@Composable
private fun CreatorSection(
    creatorId: Long,
    creatorName: String,
    creatorAvatarUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        if (creatorAvatarUrl != null) {
            AsyncImage(
                model = creatorAvatarUrl,
                contentDescription = "$creatorName's avatar",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = "by $creatorName",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ==================== DESCRIPTION SECTION ====================

@Composable
private fun DescriptionSection(
    description: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = description,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier
    )
}

// ==================== INGREDIENT SECTION ====================

@Composable
private fun IngredientSection(
    ingredients: List<Ingredient>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Ingredients",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            ingredients.forEach { ingredient ->
                IngredientItem(ingredient = ingredient)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun IngredientItem(ingredient: Ingredient) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = ingredient.name,
            style = MaterialTheme.typography.bodyLarge
        )

        Text(
            text = buildString {
                append(ingredient.quantity)
                ingredient.unit?.let { append(" $it") }
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium
        )
    }
}

// ==================== STEP CARD ====================

@Composable
private fun StepCard(
    step: RecipeStep,
    stepIndex: Int,
    modifier: Modifier = Modifier
) {
    // Orange border color from the design
    val borderColor = Color(0xFFFF9800)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(2.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Step number
            Text(
                text = "Step $stepIndex",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = borderColor
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Step description
            Text(
                text = step.description,
                style = MaterialTheme.typography.bodyMedium
            )

            // Step image (if present)
            if (step.imageUrl != null) {
                Spacer(modifier = Modifier.height(12.dp))
                AsyncImage(
                    model = step.imageUrl,
                    contentDescription = "Step $stepIndex image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(8.dp))
                )
            }
        }
    }
}

// ==================== REMINDER SECTION ====================

@Composable
private fun ReminderSection(
    reminder: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "💡 Reminder",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = reminder,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}

// ==================== UTILITY ====================

private fun formatCount(count: Int): String {
    return when {
        count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000.0)
        count >= 1_000 -> String.format("%.1fK", count / 1_000.0)
        else -> count.toString()
    }
}
