package de.formerdehne.wposter

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import de.formerdehne.wposter.data.MediaPreparer
import de.formerdehne.wposter.data.SettingsRepository
import de.formerdehne.wposter.ui.ComposeViewModel
import de.formerdehne.wposter.ui.EditViewModel
import de.formerdehne.wposter.ui.ItemState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

/** Zusammenfassung aller laufenden Uploads (Erstellen + Bearbeiten). */
data class UploadStatus(
    val total: Int,
    val done: Int,
    val failed: Int,
    val pending: Int,
    val percent: Int,
    /** Mindestens ein Upload wartet auf eine Internetverbindung. */
    val waitingForNetwork: Boolean = false,
)

class WPosterApp : Application(), ImageLoaderFactory, ViewModelStoreOwner {

    lateinit var settings: SettingsRepository
        private set

    val network by lazy { de.formerdehne.wposter.data.NetworkMonitor(this) }
    val terms by lazy { de.formerdehne.wposter.data.TermsCache(settings) }

    /**
     * ViewModels mit Daten (Entwurf, Editor, Listen) leben so lange wie der Prozess –
     * nicht nur so lange wie die Activity. So laufen Uploads weiter, auch wenn die
     * App geschlossen wird, solange der Upload-Dienst den Prozess am Leben hält.
     */
    override val viewModelStore = ViewModelStore()

    inline fun <reified T : ViewModel> appViewModel(): T =
        ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(this))[T::class.java]

    val uploadStatus: Flow<UploadStatus> by lazy {
        combine(appViewModel<ComposeViewModel>().state, appViewModel<EditViewModel>().state) { draft, edit ->
            val items = draft.items.map { it.state to it.progress } +
                edit.blocks.filterIsInstance<de.formerdehne.wposter.ui.EditBlock.Media>()
                    .filter { it.localUri != null }
                    .map { it.state to it.progress }
            val done = items.count { it.first == ItemState.DONE }
            val failed = items.count { it.first == ItemState.FAILED }
            val current = items.firstOrNull { it.first == ItemState.UPLOADING }?.second ?: 0f
            val percent = if (items.isEmpty()) 0 else (((done + current) / items.size) * 100).toInt()
            val offline = items.any { it.first == ItemState.OFFLINE }
            UploadStatus(items.size, done, failed, items.size - done - failed, percent, offline)
        }.distinctUntilChanged()
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        settings = SettingsRepository(this)
        // Reste abgebrochener Uploads aus früheren Sitzungen entfernen.
        MediaPreparer.clearWorkDir(this)
    }

    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .components { add(VideoFrameDecoder.Factory()) }
            .crossfade(true)
            .build()

    companion object {
        /** Für Texte aus Ressourcen außerhalb von Composables (siehe i18n/Strings.kt). */
        lateinit var instance: WPosterApp
            private set
    }
}
