package io.legado.app.ui.compose.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.request.ImageRequest
import io.legado.app.R

@Composable
fun LegadoImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    placeholder: Int = R.drawable.image_cover_default,
    error: Int = R.drawable.image_cover_default,
    contentScale: ContentScale = ContentScale.Crop
) {
    val context = LocalContext.current
    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(model)
            .placeholder(placeholder)
            .error(error)
            .build(),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale
    )
}

@Composable
fun LegadoImageWithLoading(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    placeholder: Int = R.drawable.image_cover_default,
    error: Int = R.drawable.image_cover_default,
    contentScale: ContentScale = ContentScale.Crop
) {
    val context = LocalContext.current
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        var isLoading by remember { mutableStateOf(true) }

        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(model)
                .placeholder(placeholder)
                .error(error)
                .build(),
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(),
            contentScale = contentScale,
            onState = { state ->
                isLoading = state is AsyncImagePainter.State.Loading
            }
        )

        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(36.dp),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
