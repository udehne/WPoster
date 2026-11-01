package de.formerdehne.wordprest

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.content.IntentCompat
import de.formerdehne.wordprest.ui.AppRoot
import de.formerdehne.wordprest.ui.CalendarViewModel
import de.formerdehne.wordprest.ui.ComposeViewModel
import de.formerdehne.wordprest.ui.SearchViewModel
import de.formerdehne.wordprest.ui.EditViewModel
import de.formerdehne.wordprest.ui.HomeViewModel
import de.formerdehne.wordprest.ui.PostViewModel
import de.formerdehne.wordprest.ui.NavViewModel
import de.formerdehne.wordprest.ui.theme.WordPrestTheme

class MainActivity : ComponentActivity() {

    private val nav: NavViewModel by viewModels()
    private val draft: ComposeViewModel by viewModels()
    private val home: HomeViewModel by viewModels()
    private val post: PostViewModel by viewModels()
    private val edit: EditViewModel by viewModels()
    private val search: SearchViewModel by viewModels()
    private val calendar: CalendarViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) handleShare(intent)
        val repository = (application as WordPrestApp).settings
        setContent {
            WordPrestTheme {
                AppRoot(
                    nav = nav,
                    draftVm = draft,
                    homeVm = home,
                    postVm = post,
                    editVm = edit,
                    searchVm = search,
                    calendarVm = calendar,
                    repository = repository,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleShare(intent)
    }

    /** Fotos/Videos, die aus einer anderen App per "Teilen" kommen. */
    private fun handleShare(intent: Intent?) {
        intent ?: return
        val uris: List<Uri> = when (intent.action) {
            Intent.ACTION_SEND ->
                listOfNotNull(IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java))
            Intent.ACTION_SEND_MULTIPLE ->
                IntentCompat.getParcelableArrayListExtra(intent, Intent.EXTRA_STREAM, Uri::class.java).orEmpty()
            else -> emptyList()
        }
        if (uris.isEmpty()) return
        // Beim nächsten Neuaufbau (z. B. Drehen) nicht erneut verarbeiten.
        intent.action = Intent.ACTION_MAIN
        val needsSetup = (application as WordPrestApp).settings.state.value.credentials == null
        draft.addMedia(uris)
        nav.openCompose(needsSetup)
    }
}
