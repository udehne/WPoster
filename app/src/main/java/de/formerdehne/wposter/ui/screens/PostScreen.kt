package de.formerdehne.wposter.ui.screens

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import de.formerdehne.wposter.data.HtmlText
import de.formerdehne.wposter.data.PostDetail
import de.formerdehne.wposter.ui.Format
import de.formerdehne.wposter.ui.PostViewState

@Composable
fun PostScreen(
    state: PostViewState,
    baseUrl: String,
    onHome: () -> Unit,
    onOlder: (Long) -> Unit,
    onNewer: (Long) -> Unit,
    onEdit: () -> Unit,
    onRetry: () -> Unit,
) {
    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                NavigationBarItem(
                    selected = false, onClick = onHome,
                    icon = { Icon(Icons.Default.Home, null) }, label = { Text("Übersicht") },
                )
                NavigationBarItem(
                    selected = false, enabled = state.olderId != null,
                    onClick = { state.olderId?.let(onOlder) },
                    icon = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null) }, label = { Text("Älterer") },
                )
                NavigationBarItem(
                    selected = false, enabled = state.newerId != null,
                    onClick = { state.newerId?.let(onNewer) },
                    icon = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) }, label = { Text("Neuerer") },
                )
                NavigationBarItem(
                    selected = false, enabled = state.post != null, onClick = onEdit,
                    icon = { Icon(Icons.Default.Edit, null) }, label = { Text("Bearbeiten") },
                )
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            when {
                state.post != null -> PostWebView(state.post, baseUrl)
                state.error != null -> Column(
                    Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("Beitrag konnte nicht geladen werden", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        state.error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(16.dp))
                    OutlinedButton(onClick = onRetry) { Text("Erneut versuchen") }
                }
                else -> CircularProgressIndicator()
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun PostWebView(post: PostDetail, baseUrl: String) {
    val dark = isSystemInDarkTheme()
    val colors = MaterialTheme.colorScheme
    val css = PageColors(
        bg = colors.background, fg = colors.onBackground, muted = colors.onSurfaceVariant,
        accent = colors.primary, line = colors.outlineVariant, quote = colors.tertiary,
    )
    val html = buildPage(post, css, dark)
    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                setBackgroundColor(css.bg.toArgb())
                settings.javaScriptEnabled = false
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                webChromeClient = WebChromeClient()
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                        // Links im Beitrag im Browser öffnen, nicht in der App.
                        openInBrowser(ctx, request.url.toString())
                        return true
                    }
                }
            }
        },
        update = { web ->
            if (web.tag != html) {
                web.tag = html
                web.setBackgroundColor(css.bg.toArgb())
                web.loadDataWithBaseURL(baseUrl.trimEnd('/') + "/", html, "text/html", "utf-8", null)
            }
        },
        onRelease = { it.destroy() },
        modifier = Modifier.fillMaxSize(),
    )
}

private data class PageColors(val bg: Color, val fg: Color, val muted: Color, val accent: Color, val line: Color, val quote: Color)

private fun Color.css(): String {
    val argb = toArgb()
    return String.format(
        "#%02x%02x%02x",
        AndroidColor.red(argb), AndroidColor.green(argb), AndroidColor.blue(argb)
    )
}

private fun buildPage(post: PostDetail, c: PageColors, dark: Boolean): String {
    val meta = listOfNotNull(post.author, Format.dateTime(post.date).ifBlank { null })
        .joinToString(" · ") { HtmlText.escape(it) }
    val scheduled = if (post.status == "future") "<span class=\"badge\">Geplant</span>" else ""
    return """
<!doctype html>
<html lang="de"><head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<meta name="color-scheme" content="${if (dark) "dark" else "light"}">
<style>
  html { -webkit-text-size-adjust: 100%; }
  body { margin: 0; padding: 28px 20px 40px; background: ${c.bg.css()}; color: ${c.fg.css()};
         font: 17px/1.65 Roboto, "Noto Sans", sans-serif; word-wrap: break-word; overflow-wrap: anywhere; }
  header { margin-bottom: 24px; padding-bottom: 18px; border-bottom: 1px solid ${c.line.css()}; }
  h1.title { font-size: 28px; line-height: 1.25; margin: 0 0 10px; font-weight: 600; letter-spacing: -0.2px; }
  .meta { color: ${c.muted.css()}; font-size: 14px; }
  .badge { display: inline-block; font-size: 12px; padding: 2px 8px; border-radius: 6px;
           background: ${c.quote.css()}; color: ${c.bg.css()}; margin-left: 6px; }
  article > *:first-child { margin-top: 0; }
  p { margin: 0 0 1.1em; }
  h2, h3, h4 { line-height: 1.3; margin: 1.6em 0 0.6em; font-weight: 600; }
  a { color: ${c.accent.css()}; }
  img, video, iframe { max-width: 100%; height: auto; border-radius: 12px; display: block; }
  video { width: 100%; background: #000; }
  figure { margin: 22px 0; }
  figcaption { font-size: 14px; color: ${c.muted.css()}; text-align: center; margin-top: 8px; }
  .wp-block-gallery, .blocks-gallery-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 8px; padding: 0; list-style: none; }
  .wp-block-gallery figure, .blocks-gallery-item { margin: 0; }
  .wp-block-gallery img { width: 100%; height: 100%; object-fit: cover; aspect-ratio: 1; }
  blockquote { border-left: 3px solid ${c.quote.css()}; margin: 18px 0; padding: 2px 0 2px 16px; color: ${c.muted.css()}; }
  hr { border: 0; border-top: 1px solid ${c.line.css()}; margin: 28px 0; }
  ul, ol { padding-left: 1.3em; }
</style></head>
<body>
<header>
  <h1 class="title">${HtmlText.escape(post.title.ifBlank { "(ohne Titel)" })}</h1>
  <div class="meta">$meta$scheduled</div>
</header>
<article>${post.contentHtml}</article>
</body></html>
""".trimIndent()
}
