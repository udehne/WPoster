package de.formerdehne.wordprest.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import de.formerdehne.wordprest.ui.SearchState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    state: SearchState,
    onBack: () -> Unit,
    onQuery: (String) -> Unit,
    onSubmit: () -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    onOpenPost: (Long) -> Unit,
) {
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    // Beim ersten Öffnen direkt ins Suchfeld springen.
    LaunchedEffect(Unit) { if (state.query.isEmpty()) focus.requestFocus() }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück") }
                },
                title = {
                    TextField(
                        value = state.query,
                        onValueChange = onQuery,
                        placeholder = { Text("In Überschriften und Texten suchen") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        trailingIcon = {
                            if (state.query.isNotEmpty()) {
                                IconButton(onClick = { onQuery(""); focus.requestFocus() }) {
                                    Icon(Icons.Default.Clear, "Leeren")
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { keyboard?.hide(); onSubmit() }),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.fillMaxWidth().padding(end = 12.dp).focusRequester(focus),
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when {
                state.loading -> item(key = "loading") {
                    Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                state.error != null && state.results.isEmpty() -> item(key = "error") {
                    MessageBlock("Suche fehlgeschlagen", state.error, "Erneut versuchen", onRetry)
                }
                state.searchedFor == null -> item(key = "hint") {
                    MessageBlock(
                        null,
                        "Gib mindestens zwei Buchstaben ein. Gesucht wird in Überschriften und Texten aller Beiträge.",
                        null, null,
                    )
                }
                state.results.isEmpty() -> item(key = "none") {
                    MessageBlock("Nichts gefunden", "Keine Beiträge zu „${state.searchedFor}“.", null, null)
                }
                else -> {
                    item(key = "count") {
                        Text(
                            if (state.endReached && state.results.size == 1) "1 Treffer"
                            else if (state.endReached) "${state.results.size} Treffer"
                            else "Mehr als ${state.results.size} Treffer",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    items(state.results, key = { it.id }) { post -> PostCard(post) { onOpenPost(post.id) } }
                    item(key = "footer") {
                        when {
                            state.error != null -> MessageBlock(null, state.error, "Erneut versuchen", onRetry)
                            !state.endReached -> {
                                LaunchedEffect(state.results.size) { onLoadMore() }
                                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 2.dp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
