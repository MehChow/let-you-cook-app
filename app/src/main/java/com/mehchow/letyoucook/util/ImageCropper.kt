package com.mehchow.letyoucook.util

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import com.canhub.cropper.CropImageView
import com.canhub.cropper.CropImageView.RequestSizeOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun rememberImageCropperState(uri: String? = null): ImageCropperState {
    val state = remember { ImageCropperState() }
    if (uri != null) {
        LaunchedEffect(uri) {
            state.load(uri.toUri())
        }

        LaunchedEffect(
            state.aspectRatio,
            state.isAutoZoomEnabled,
            state.cropShape,
            state.guidelines
        ) {
            state.applyConfig()
        }
    }
    return state
}

@Stable
@Suppress("unused")
class ImageCropperState() {
    internal var cropper by mutableStateOf<CropImageView?>(null)
        private set

    private var uri: Uri = Uri.EMPTY

    /**
     * Configs
     */
    var aspectRatio by mutableStateOf(1 to 1)
    var isAutoZoomEnabled by mutableStateOf(true)
    var isFixAspectRatio by mutableStateOf(true)
    var cropShape by mutableStateOf(CropImageView.CropShape.RECTANGLE)
    var guidelines by mutableStateOf(CropImageView.Guidelines.ON)

    fun load(imageUri: Uri) {
        uri = imageUri

        if (cropper?.imageUri != imageUri) {
            cropper?.setImageUriAsync(imageUri)
        }
    }

    suspend fun crop(
        context: Context,
        saveUri: Uri,
        reqWidth: Int = 0,
        reqHeight: Int = 0,
        format: Bitmap.CompressFormat = Bitmap.CompressFormat.PNG,
        quality: Int = 90,
        options: RequestSizeOptions = RequestSizeOptions.RESIZE_INSIDE,
    ): Unit = withContext(Dispatchers.IO) {
        val bitmap = cropper?.getCroppedImage(reqWidth, reqHeight, options)

        bitmap?.let { bmp ->
            context.contentResolver.openOutputStream(saveUri)?.use { out ->
                bmp.compress(format, quality, out)
            }
        }
    }

    internal fun applyConfig() {
        cropper?.setAspectRatio(aspectRatio.first, aspectRatio.second)
        cropper?.setFixedAspectRatio(isFixAspectRatio)
        cropper?.isAutoZoomEnabled = isAutoZoomEnabled
        cropper?.cropShape = cropShape
        cropper?.guidelines = guidelines
    }

    internal fun viewFactory(context: Context): FrameLayout {
        val layout = FrameLayout(context)
        val cropperView = CropImageView(context)
        layout.addView(
            cropperView, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            )
        )
        cropper = cropperView
        return layout
    }

    internal fun viewUpdate(view: FrameLayout) {
        load(uri)
        applyConfig()
    }

    internal fun viewRelease(view: FrameLayout) {
        cropper?.clearImage()
        cropper = null
    }
}


@Composable
fun ImageCropper(
    state: ImageCropperState,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            factory = state::viewFactory,
            update = state::viewUpdate,
            onRelease = state::viewRelease
        )
    }
}