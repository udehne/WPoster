package de.formerdehne.wordprest

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import de.formerdehne.wordprest.data.MediaPreparer
import de.formerdehne.wordprest.data.SettingsRepository

class WordPrestApp : Application(), ImageLoaderFactory {

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
