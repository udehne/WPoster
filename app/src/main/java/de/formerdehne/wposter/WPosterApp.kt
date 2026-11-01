package de.formerdehne.wposter

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import de.formerdehne.wposter.data.MediaPreparer
import de.formerdehne.wposter.data.SettingsRepository

class WPosterApp : Application(), ImageLoaderFactory {

    lateinit var settings: SettingsRepository
        private set

    override fun onCreate() {
        super.onCreate()
        settings = SettingsRepository(this)
        // Reste abgebrochener Uploads aus früheren Sitzungen entfernen.
        MediaPreparer.clearWorkDir(this)
    }

    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .components { add(VideoFrameDecoder.Factory()) }
            .crossfade(true)
            .build()
}
