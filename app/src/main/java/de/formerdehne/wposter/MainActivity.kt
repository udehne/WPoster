package de.formerdehne.wposter

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import de.formerdehne.wposter.ui.AppRoot
import de.formerdehne.wposter.ui.CalendarViewModel
import de.formerdehne.wposter.ui.ComposeViewModel
import de.formerdehne.wposter.ui.EditViewModel
import de.formerdehne.wposter.ui.HomeViewModel
import de.formerdehne.wposter.ui.NavViewModel
import de.formerdehne.wposter.ui.PostViewModel
import de.formerdehne.wposter.ui.SearchViewModel
import de.formerdehne.wposter.ui.theme.WPosterTheme
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val app get() = application as WPosterApp

    // Navigation gehört zur Activity; Daten-ViewModels leben in der App (siehe WPosterApp).
    private val nav: NavViewModel by viewModels()
    private val draft by lazy { app.appViewModel<ComposeViewModel>() }
    private val home by lazy { app.appViewModel<HomeViewModel>() }
    private val post by lazy { app.appViewModel<PostViewModel>() }
    private val edit by lazy { app.appViewModel<EditViewModel>() }
    private val search by lazy { app.appViewModel<SearchViewModel>() }
    private val calendar by lazy { app.appViewModel<CalendarViewModel>() }

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* optional */ }
    private var askedForNotifications = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) handleIntent(intent)
        setContent {
            WPosterTheme {
                AppRoot(
                    nav = nav,
                    draftVm = draft,
                    homeVm = home,
                    postVm = post,
                    editVm = edit,
                    searchVm = search,
                    calendarVm = calendar,
                    repository = app.settings,
                )
            }
        }
        // Sobald Uploads laufen: Upload-Dienst starten (Fortschritt + Weiterlaufen im Hintergrund).
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                app.uploadStatus.map { it.pending > 0 }.distinctUntilChanged().collect { active ->
                    if (active) {
                        askForNotificationPermission()
                        UploadService.start(this@MainActivity)
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun askForNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || askedForNotifications) return
        askedForNotifications = true
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun handleIntent(intent: Intent?) {
        intent ?: return
        when (intent.action) {
            UploadService.ACTION_OPEN_UPLOADS -> {
                // Tipp auf die Upload-Benachrichtigung: zum laufenden Entwurf.
                intent.action = Intent.ACTION_MAIN
                if (draft.state.value.items.isNotEmpty()) nav.openCompose(needsSetup = false)
            }
            Intent.ACTION_SEND, Intent.ACTION_SEND_MULTIPLE -> handleShare(intent)
        }
    }

    /** Fotos/Videos, die aus einer anderen App per "Teilen" kommen. */
    private fun handleShare(intent: Intent) {
        val uris: List<Uri> = when (intent.action) {
            Intent.ACTION_SEND ->
                listOfNotNull(IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java))
            else ->
                IntentCompat.getParcelableArrayListExtra(intent, Intent.EXTRA_STREAM, Uri::class.java).orEmpty()
        }
        if (uris.isEmpty()) return
        // Beim nächsten Neuaufbau (z. B. Drehen) nicht erneut verarbeiten.
        intent.action = Intent.ACTION_MAIN
        val needsSetup = app.settings.state.value.credentials == null
        draft.addMedia(uris, stage = true)
        nav.openCompose(needsSetup)
    }
}
