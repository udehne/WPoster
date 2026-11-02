package de.formerdehne.wposter.ui.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import de.formerdehne.wposter.R
import de.formerdehne.wposter.ui.OnboardingState
import de.formerdehne.wposter.ui.OnboardingViewModel

private const val STEP_COUNT = 3

/** Geführte Einrichtung eines Blogs: Adresse → Zugriff erlauben → Zugangsdaten. */
@Composable
fun OnboardingScreen(state: OnboardingState, vm: OnboardingViewModel, onCancel: () -> Unit) {
    val context = LocalContext.current
    var help by remember { mutableStateOf<FieldHelp?>(null) }

    val goBack: () -> Unit = { if (!vm.back()) onCancel() }
    BackHandler(onBack = goBack)

    Scaffold(topBar = { AppTopBar(stringResource(R.string.onb_title), onBack = goBack) }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            StepIndicator(state.step)

            when (state.step) {
                1 -> AddressStep(state, vm, onHelp = { help = it })
                2 -> AuthorizeStep(
                    state = state,
                    onAuthorize = { openExternalBrowser(context, vm.authorizeInBrowserUrl()) },
                    onManual = {
                        openExternalBrowser(context, vm.profileUrl())
                        vm.next()
                    },
                )
                else -> CredentialsStep(state, vm, onHelp = { help = it })
            }

            state.error?.let { OnboardingErrorCard(it) }
            Spacer(Modifier.height(24.dp))
        }
    }

    help?.let { h -> FieldHelpDialog(h) { help = null } }
}

@Composable
private fun StepIndicator(step: Int) {
    val progress by animateFloatAsState(step.coerceIn(1, STEP_COUNT) / STEP_COUNT.toFloat(), label = "step")
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
        Text(
            stringResource(R.string.onb_step, step.coerceIn(1, STEP_COUNT), STEP_COUNT),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun StepHeader(title: String, text: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun AddressStep(state: OnboardingState, vm: OnboardingViewModel, onHelp: (FieldHelp) -> Unit) {
    Spacer(Modifier.height(8.dp))
    BrandMark(64.dp)
    StepHeader(stringResource(R.string.onb_address_title), stringResource(R.string.onb_address_intro))
    OutlinedTextField(
        value = state.url,
        onValueChange = vm::setUrl,
        label = { Text(stringResource(R.string.settings_address)) },
        placeholder = { Text(stringResource(R.string.settings_address_placeholder)) },
        singleLine = true,
        enabled = !state.busy,
        trailingIcon = { InfoButton { onHelp(FieldHelp.URL) } },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
        keyboardActions = KeyboardActions(onGo = { if (state.url.isNotBlank()) vm.next() }),
        modifier = Modifier.fillMaxWidth(),
    )
    PrimaryBusyButton(
        text = stringResource(R.string.onb_continue),
        busyText = stringResource(R.string.onb_checking_address),
        busy = state.busy,
        enabled = state.url.isNotBlank(),
        onClick = vm::next,
    )
}

@Composable
private fun AuthorizeStep(state: OnboardingState, onAuthorize: () -> Unit, onManual: () -> Unit) {
    StepHeader(stringResource(R.string.onb_authorize_title), stringResource(R.string.onb_authorize_text))

    val site = state.siteName ?: state.url.removePrefix("https://").removePrefix("http://")
    if (site.isNotBlank()) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                BrandMark(40.dp)
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        if (state.siteName != null) stringResource(R.string.onb_found_site) else stringResource(R.string.settings_address),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(site, style = MaterialTheme.typography.titleMedium)
                }
                if (state.siteName != null) {
                    Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }

    Button(onClick = onAuthorize, modifier = Modifier.fillMaxWidth().height(56.dp)) {
        Text(stringResource(R.string.onb_authorize_button))
    }
    Text(
        stringResource(R.string.onb_return_hint),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    OutlinedButton(onClick = onManual, modifier = Modifier.fillMaxWidth().height(52.dp)) {
        Text(stringResource(R.string.onb_manual_button))
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            stringResource(R.string.onb_requirements),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

@Composable
private fun CredentialsStep(state: OnboardingState, vm: OnboardingViewModel, onHelp: (FieldHelp) -> Unit) {
    var showPassword by rememberSaveable { mutableStateOf(false) }
    StepHeader(stringResource(R.string.onb_credentials_title), stringResource(R.string.onb_credentials_intro))
    if (state.url.isNotBlank()) {
        Text(
            state.url.removePrefix("https://").removePrefix("http://"),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
    }
    OutlinedTextField(
        value = state.user,
        onValueChange = vm::setUser,
        label = { Text(stringResource(R.string.settings_username)) },
        singleLine = true,
        enabled = !state.busy,
        trailingIcon = { InfoButton { onHelp(FieldHelp.USER) } },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = state.password,
        onValueChange = vm::setPassword,
        label = { Text(stringResource(R.string.settings_app_password)) },
        placeholder = { Text("xxxx xxxx xxxx xxxx xxxx xxxx") },
        singleLine = true,
        enabled = !state.busy,
        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { vm.connect() }),
        trailingIcon = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { showPassword = !showPassword }) {
                    Text(stringResource(if (showPassword) R.string.settings_hide else R.string.settings_show))
                }
                InfoButton { onHelp(FieldHelp.PASSWORD) }
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
    PrimaryBusyButton(
        text = stringResource(R.string.onb_connect_button),
        busyText = stringResource(R.string.settings_checking),
        busy = state.busy,
        enabled = state.user.isNotBlank() && state.password.isNotBlank(),
        onClick = vm::connect,
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            stringResource(R.string.settings_stored_encrypted),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

@Composable
private fun PrimaryBusyButton(text: String, busyText: String, busy: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled && !busy,
        modifier = Modifier.fillMaxWidth().height(56.dp),
    ) {
        if (busy) {
            CircularProgressIndicator(
                Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(12.dp))
            Text(busyText)
        } else {
            Text(text)
        }
    }
}

@Composable
private fun OnboardingErrorCard(message: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.onErrorContainer)
            Spacer(Modifier.width(12.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
    }
}

/**
 * Öffnet eine Adresse im externen Standard-Browser – bewusst kein Custom Tab, damit
 * WordPress' Rücksprung (wposter://auth) zuverlässig bei der App ankommt.
 */
fun openExternalBrowser(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        .addCategory(Intent.CATEGORY_BROWSABLE)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, context.getString(R.string.onb_no_browser), Toast.LENGTH_LONG).show()
    }
}
