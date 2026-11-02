package de.formerdehne.wposter.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import de.formerdehne.wposter.R
import de.formerdehne.wposter.data.Term
import de.formerdehne.wposter.ui.TermsUi

/** Auswahl von Kategorien (aus Liste + neu) und Schlagwörtern (Vorschläge + neu). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TermsSection(
    state: TermsUi,
    onToggleCategory: (Term) -> Unit,
    onAddCategory: (String) -> Unit,
    onTagQuery: (String) -> Unit,
    onAddTagFromQuery: () -> Unit,
    onAddTag: (Term) -> Unit,
    onRemoveTag: (Term) -> Unit,
    onRetry: () -> Unit,
) {
    var showAllCategories by rememberSaveable { mutableStateOf(false) }
    var newCategoryDialog by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxWidth()) {
        // ---------- Kategorien ----------
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel(stringResource(R.string.terms_categories), Modifier.weight(1f))
            if (state.loading) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
        }
        if (state.error != null) {
            Text(
                stringResource(R.string.terms_load_failed, state.error),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
            TextButton(onClick = onRetry) { Text(stringResource(R.string.common_retry)) }
        }
        // Gewählte zuerst, dann die übrigen; bei vielen Kategorien zunächst nur ein Teil.
        val selected = state.selectedCategories
        val others = state.availableCategories.filter { c -> selected.none { same(it, c) } }
        val visibleOthers = if (showAllCategories) others else others.take(MAX_COLLAPSED)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (selected + visibleOthers).forEach { term ->
                val isSelected = selected.any { same(it, term) }
                FilterChip(
                    selected = isSelected,
                    onClick = { onToggleCategory(term) },
                    label = { Text(label(term)) },
                    leadingIcon = if (isSelected) {
                        { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                    } else null,
                )
            }
            AssistChip(
                onClick = { newCategoryDialog = true },
                label = { Text(stringResource(R.string.terms_new_category)) },
                leadingIcon = { Icon(Icons.Default.Add, null, Modifier.size(16.dp)) },
            )
        }
        if (others.size > MAX_COLLAPSED) {
            TextButton(onClick = { showAllCategories = !showAllCategories }) {
                Text(
                    if (showAllCategories) stringResource(R.string.terms_show_less)
                    else stringResource(R.string.terms_show_all, others.size + selected.size)
                )
            }
        }

        // ---------- Schlagwörter ----------
        SectionLabel(stringResource(R.string.terms_tags), Modifier.padding(top = 8.dp))
        if (state.selectedTags.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                state.selectedTags.forEach { term ->
                    InputChip(
                        selected = true,
                        onClick = { onRemoveTag(term) },
                        label = { Text(label(term)) },
                        trailingIcon = {
                            Icon(Icons.Default.Close, stringResource(R.string.terms_remove_tag), Modifier.size(16.dp))
                        },
                    )
                }
            }
        }
        OutlinedTextField(
            value = state.tagQuery,
            onValueChange = onTagQuery,
            placeholder = { Text(stringResource(R.string.terms_tag_placeholder)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done, capitalization = KeyboardCapitalization.None),
            keyboardActions = KeyboardActions(onDone = { onAddTagFromQuery() }),
            trailingIcon = {
                if (state.tagQuery.isNotBlank()) {
                    IconButton(onClick = onAddTagFromQuery) { Icon(Icons.Default.Add, stringResource(R.string.common_add)) }
                }
            },
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        )
        val suggestions = state.tagSuggestions
        if (suggestions.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                suggestions.forEach { term ->
                    SuggestionChip(onClick = { onAddTag(term) }, label = { Text(term.name) })
                }
            }
        }
    }

    if (newCategoryDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { newCategoryDialog = false },
            title = { Text(stringResource(R.string.terms_new_category_title)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(R.string.terms_category_name)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    )
                    Text(
                        stringResource(R.string.terms_new_category_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { onAddCategory(name); newCategoryDialog = false },
                    enabled = name.isNotBlank(),
                ) { Text(stringResource(R.string.common_add)) }
            },
            dismissButton = { TextButton(onClick = { newCategoryDialog = false }) { Text(stringResource(R.string.common_cancel)) } },
        )
    }
}

@Composable
private fun label(term: Term): String =
    if (term.id == null) stringResource(R.string.terms_new_suffix, term.name) else term.name

private fun same(a: Term, b: Term) =
    if (a.id != null && b.id != null) a.id == b.id else a.name.equals(b.name, ignoreCase = true)

private const val MAX_COLLAPSED = 12
