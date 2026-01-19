package com.mehchow.letyoucook.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mehchow.letyoucook.ui.components.RecipeCardItem
import com.mehchow.letyoucook.ui.theme.color_primary
import com.mehchow.letyoucook.ui.viewmodel.HomeUiState
import com.mehchow.letyoucook.ui.viewmodel.HomeViewModel

/**
 * Home tab content - displayed inside the MainScreen's bottom navigation.
 * Shows the recipe feed with transparent top bar and hamburger menu.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTabContent(
    onRecipeClick: (Long) -> Unit,
    onMenuClick: () -> Unit = {}, // For future drawer expansion
    shouldRefresh: Boolean = false,
    onRefreshConsumed: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // Trigger refresh when shouldRefresh becomes true (e.g., after creating a recipe)
    LaunchedEffect(shouldRefresh) {
        if (shouldRefresh) {
            viewModel.refreshRecipes()
            onRefreshConsumed()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Let You Cook",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menu"
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(color = color_primary)
        ) {
            when (val state = uiState) {
                is HomeUiState.Loading -> {
                    LoadingContent()
                }

                is HomeUiState.Success -> {
                    SuccessContent(
                        state = state,
                        onRecipeClick = onRecipeClick,
                        onRefresh = { viewModel.refreshRecipes() },
                        onLoadMore = { viewModel.loadMoreRecipes() }
                    )
                }

                is HomeUiState.Error -> {
                    ErrorContent(
                        message = state.message,
                        onRetry = { viewModel.loadRecipes() }
                    )
                }

                is HomeUiState.Empty -> {
                    EmptyContent(
                        onRefresh = { viewModel.refreshRecipes() }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SuccessContent(
    state: HomeUiState.Success,
    onRecipeClick: (Long) -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit
) {
    // Grid state for tracking scroll position
    val gridState = rememberLazyStaggeredGridState()

    // Detect when user scrolls near the bottom (for infinite scroll)
    // This is a derived state that automatically updates when gridState changes
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisibleItem = gridState.layoutInfo.visibleItemsInfo.lastOrNull()
            val totalItems = gridState.layoutInfo.totalItemsCount

            // Load more when last visible item is within 5 items of the end
            lastVisibleItem != null &&
                    lastVisibleItem.index >= totalItems - 5 &&
                    !state.isLoadingMore &&
                    state.hasMorePages
        }
    }

    // Trigger load more when shouldLoadMore becomes true
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            onLoadMore()
        }
    }

    // PullToRefreshBox wraps content and adds pull-to-refresh gesture
    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
    ) {
        // LazyVerticalStaggeredGrid - like CSS Grid with masonry layout
        // Items can have different heights
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(2), // 2 columns
            state = gridState,
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalItemSpacing = 12.dp,
            modifier = Modifier.fillMaxSize()
        ) {
            // items() is like map() in React - renders each recipe
            items(
                items = state.recipes,
                key = { recipe -> recipe.id } // Unique key for efficient updates
            ) { recipe ->
                RecipeCardItem(
                    recipe = recipe,
                    onClick = { onRecipeClick(recipe.id) }
                )
            }

            // Loading more indicator at the bottom
            if (state.isLoadingMore) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Error state - shows error message with retry button.
 */
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
            text = "Oops! Something went wrong",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
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
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text("Try Again")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EmptyContent(
    onRefresh: () -> Unit
) {
    PullToRefreshBox(
        isRefreshing = false,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "\uD83C\uDF73",
                fontSize = 64.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "No recipes yet",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Be the first to share a delicious recipe!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}