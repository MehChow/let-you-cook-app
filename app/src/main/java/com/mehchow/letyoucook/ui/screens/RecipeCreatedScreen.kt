package com.mehchow.letyoucook.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.mehchow.letyoucook.R
import com.mehchow.letyoucook.ui.theme.LetYouCookTheme
import kotlinx.coroutines.delay

/**
 * Success screen shown after a recipe is created.
 * Displays a Lottie animation and auto-navigates to home after animation completes + delay.
 */
@Composable
fun RecipeCreatedScreen(
    onNavigateToHome: () -> Unit,
    postAnimationDelayMillis: Long = 1000L
) {
    // Load Lottie animation from local raw resource
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.verified)
    )

    // Animate the composition - plays once then holds at the end
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = 1,  // Play once
        isPlaying = true
    )

    // Navigate after animation completes + delay
    // progress reaches 1.0f when animation is complete
    LaunchedEffect(progress) {
        if (progress >= 1f) {
            delay(postAnimationDelayMillis)
            onNavigateToHome()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Lottie Animation
            LottieAnimation(
                composition = composition,
                progress = { progress },
                modifier = Modifier.size(200.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Success message
            Text(
                text = "Recipe Created!",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Taking you back to home...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun RecipeCreatedScreenPreview() {
    LetYouCookTheme {
        RecipeCreatedScreen(
            onNavigateToHome = {},
            postAnimationDelayMillis = Long.MAX_VALUE  // Prevent auto-navigation in preview
        )
    }
}
