package app.focus.feature.profiles.icon

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import coil3.ImageLoader
import coil3.asImage
import coil3.compose.AsyncImage
import coil3.decode.DataSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.ImageFetchResult
import coil3.request.ImageRequest
import coil3.request.Options

data class PackageIconKey(val packageName: String)

class PackageIconFetcher(
    private val context: Context,
    private val packageName: String,
) : Fetcher {
    override suspend fun fetch(): FetchResult? = runCatching {
        val drawable = context.packageManager.getApplicationIcon(packageName)
        ImageFetchResult(
            image = drawable.asImage(),
            isSampled = false,
            dataSource = DataSource.MEMORY,
        )
    }.getOrNull()
}

private class PackageIconFetcherFactory(
    private val context: Context,
) : Fetcher.Factory<PackageIconKey> {
    override fun create(data: PackageIconKey, options: Options, imageLoader: ImageLoader): Fetcher? =
        PackageIconFetcher(context, data.packageName)
}

object PackageIconImageLoader {
    fun get(context: Context): ImageLoader =
        ImageLoader.Builder(context)
            .components {
                add(PackageIconFetcherFactory(context))
            }
            .build()
}

@Composable
fun AppPackageIcon(
    packageName: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    val context = LocalContext.current
    val imageLoader = remember(context) { PackageIconImageLoader.get(context) }
    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(PackageIconKey(packageName))
            .build(),
        imageLoader = imageLoader,
        contentDescription = contentDescription,
        modifier = modifier,
    )
}
