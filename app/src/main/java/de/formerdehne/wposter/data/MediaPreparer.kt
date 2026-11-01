package de.formerdehne.wposter.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import android.provider.OpenableColumns
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import kotlin.coroutines.coroutineContext
import kotlin.math.max

data class PreparedMedia(val file: File, val mimeType: String, val fileName: String, val isVideo: Boolean)

/**
 * Bereitet Fotos und Videos für den Upload vor:
 *  - Fotos werden (falls nötig) verkleinert, richtig gedreht und als JPEG neu
 *    gespeichert. Dabei gehen ALLE Metadaten verloren (GPS, Kamera, …).
 *  - Videos werden ohne Neukodierung in einen neuen Container umkopiert.
 *    Metadaten wie der Aufnahmeort werden dabei nicht übernommen.
 */
class MediaPreparer(private val context: Context) {

    private val resolver = context.contentResolver

    suspend fun prepare(uri: Uri, maxImageEdge: Int): PreparedMedia = withContext(Dispatchers.IO) {
        val name = displayName(uri)
        val mime = resolver.getType(uri) ?: guessMime(name)
        val baseName = name.substringBeforeLast('.').ifBlank { "medium" }
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
            .take(60)
        when {
            mime.startsWith("video/") -> prepareVideo(uri, mime, baseName)
            mime == "image/gif" -> copyAsIs(uri, "$baseName.gif", mime)
            mime.startsWith("image/") -> prepareImage(uri, baseName, maxImageEdge)
            else -> throw WpException("Dieser Dateityp wird nicht unterstützt ($mime).")
        }
    }

    // ---------- Fotos ----------

    private suspend fun prepareImage(uri: Uri, baseName: String, maxEdge: Int): PreparedMedia {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        open(uri).use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw WpException("Das Bild konnte nicht gelesen werden.")
        }
        val orientation = try {
            open(uri).use { ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) }
        } catch (e: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }

        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxEdge) sample *= 2
        val decoded = open(uri).use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: throw WpException("Das Bild konnte nicht gelesen werden.")
        coroutineContext.ensureActive()

        val longest = max(decoded.width, decoded.height)
        val scale = if (longest > maxEdge) maxEdge.toFloat() / longest else 1f
        val matrix = orientationMatrix(orientation).apply { preScale(scale, scale) }
        var bitmap = if (scale != 1f || !matrix.isIdentity) {
            Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
                .also { if (it !== decoded) decoded.recycle() }
        } else decoded

        if (bitmap.hasAlpha()) {
            // JPEG kennt keine Transparenz – auf Weiß legen statt Schwarz.
            val opaque = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
            Canvas(opaque).apply { drawColor(Color.WHITE); drawBitmap(bitmap, 0f, 0f, null) }
            bitmap.recycle()
            bitmap = opaque
        }

        val out = newWorkFile("$baseName.jpg")
        FileOutputStream(out).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 88, it) }
        bitmap.recycle()
        return PreparedMedia(out, "image/jpeg", out.name.substringAfter('_'), isVideo = false)
    }

    private fun orientationMatrix(orientation: Int) = Matrix().apply {
        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> setScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> setRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> { setRotate(180f); postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_TRANSPOSE -> { setRotate(90f); postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_ROTATE_90 -> setRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> { setRotate(-90f); postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_ROTATE_270 -> setRotate(-90f)
        }
    }

    // ---------- Videos ----------

    private suspend fun prepareVideo(uri: Uri, mime: String, baseName: String): PreparedMedia {
        val webm = mime == "video/webm"
        val out = newWorkFile(if (webm) "$baseName.webm" else "$baseName.mp4")
        val rotation = try {
            MediaMetadataRetriever().run {
                try {
                    setDataSource(context, uri)
                    extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
                } finally {
                    release()
                }
            }
        } catch (e: Exception) {
            0
        }

        val extractor = MediaExtractor()
        var muxer: MediaMuxer? = null
        try {
            extractor.setDataSource(context, uri, null)
            val m = MediaMuxer(
                out.absolutePath,
                if (webm) MediaMuxer.OutputFormat.MUXER_OUTPUT_WEBM else MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
            )
            muxer = m
            val trackMap = HashMap<Int, Int>()
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val trackMime = format.getString(android.media.MediaFormat.KEY_MIME) ?: continue
                // Nur Bild und Ton übernehmen – Metadaten-Spuren (z. B. GPS) fallen weg.
                if (!trackMime.startsWith("video/") && !trackMime.startsWith("audio/")) continue
                extractor.selectTrack(i)
                trackMap[i] = m.addTrack(format)
            }
            if (trackMap.isEmpty()) throw WpException("Das Video enthält keine lesbaren Spuren.")
            if (!webm) m.setOrientationHint(rotation)
            m.start()

            val buffer = ByteBuffer.allocate(8 * 1024 * 1024)
            val info = MediaCodec.BufferInfo()
            while (true) {
                coroutineContext.ensureActive()
                val track = extractor.sampleTrackIndex
                if (track < 0) break
                val size = extractor.readSampleData(buffer, 0)
                if (size < 0) break
                info.offset = 0
                info.size = size
                info.presentationTimeUs = extractor.sampleTime
                info.flags = if (extractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC != 0) {
                    MediaCodec.BUFFER_FLAG_KEY_FRAME
                } else 0
                trackMap[track]?.let { m.writeSampleData(it, buffer, info) }
                extractor.advance()
            }
            m.stop()
        } catch (e: WpException) {
            out.delete(); throw e
        } catch (e: kotlinx.coroutines.CancellationException) {
            out.delete(); throw e
        } catch (e: Exception) {
            out.delete()
            throw WpException("Das Video konnte nicht verarbeitet werden (${e.javaClass.simpleName}).")
        } finally {
            try { muxer?.release() } catch (_: Exception) {}
            extractor.release()
        }
        return PreparedMedia(out, if (webm) "video/webm" else "video/mp4", out.name.substringAfter('_'), isVideo = true)
    }

    // ---------- Hilfsfunktionen ----------

    private fun copyAsIs(uri: Uri, fileName: String, mime: String): PreparedMedia {
        val out = newWorkFile(fileName)
        open(uri).use { input -> FileOutputStream(out).use { input.copyTo(it) } }
        return PreparedMedia(out, mime, out.name.substringAfter('_'), isVideo = false)
    }

    private fun open(uri: Uri) = resolver.openInputStream(uri)
        ?: throw WpException("Die Datei konnte nicht geöffnet werden.")

    private fun displayName(uri: Uri): String = try {
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
    } catch (e: Exception) {
        null
    } ?: uri.lastPathSegment ?: "medium"

    private fun guessMime(name: String): String = when (name.substringAfterLast('.', "").lowercase()) {
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "gif" -> "image/gif"
        "webp" -> "image/webp"
        "heic", "heif" -> "image/heic"
        "mp4", "m4v" -> "video/mp4"
        "mov" -> "video/quicktime"
        "webm" -> "video/webm"
        "3gp" -> "video/3gpp"
        else -> "application/octet-stream"
    }

    private fun newWorkFile(name: String): File =
        File(workDir(context), "${System.nanoTime()}_$name")

    companion object {
        private fun workDir(context: Context) = File(context.cacheDir, "uploads").apply { mkdirs() }

        fun clearWorkDir(context: Context) {
            workDir(context).listFiles()?.forEach { it.delete() }
        }
    }
}

