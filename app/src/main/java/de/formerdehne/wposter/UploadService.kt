package de.formerdehne.wposter

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Vordergrunddienst, solange Fotos/Videos hochgeladen werden: zeigt den Fortschritt
 * in der Benachrichtigungsleiste und verhindert, dass Android die App im Hintergrund beendet.
 */
class UploadService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var collecting = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createChannels(this)
        startInForeground(progressNotification(UploadStatus(0, 0, 0, 1, 0)))
        if (!collecting) {
            collecting = true
            scope.launch {
                (application as WPosterApp).uploadStatus.collect { status ->
                    if (status.pending > 0) {
                        NotificationManagerCompat.from(this@UploadService).notifySafe(ID_PROGRESS, progressNotification(status))
                    } else {
                        finish(status)
                    }
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun startInForeground(notification: android.app.Notification) {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC else 0
        ServiceCompat.startForeground(this, ID_PROGRESS, notification, type)
    }

    private fun progressNotification(status: UploadStatus) =
        NotificationCompat.Builder(this, CHANNEL_PROGRESS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.upload_notification_title))
            .setContentText(
                if (status.waitingForNetwork) getString(R.string.upload_notification_offline)
                else getString(R.string.upload_notification_text, (status.done + 1).coerceAtMost(status.total.coerceAtLeast(1)), status.total.coerceAtLeast(1))
            )
            .setProgress(100, status.percent, status.total == 0)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setContentIntent(openAppIntent(this))
            .build()

    private fun finish(status: UploadStatus) {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        val inForeground = ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
        val manager = NotificationManagerCompat.from(this)
        when {
            status.failed > 0 -> manager.notifySafe(
                ID_RESULT,
                resultNotification(getString(R.string.upload_failed_title), getString(R.string.upload_failed_text, status.failed))
            )
            !inForeground && status.done > 0 -> manager.notifySafe(
                ID_RESULT,
                resultNotification(getString(R.string.upload_done_title), getString(R.string.upload_done_text))
            )
        }
        stopSelf()
    }

    private fun resultNotification(title: String, text: String) =
        NotificationCompat.Builder(this, CHANNEL_RESULT)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(this))
            .build()

    /** Android 15+: Zeitlimit für Datensynchronisation erreicht – sauber beenden. */
    override fun onTimeout(startId: Int, fgsType: Int) {
        stopSelf()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_PROGRESS = "uploads"
        private const val CHANNEL_RESULT = "upload_results"
        private const val ID_PROGRESS = 1
        private const val ID_RESULT = 2
        const val ACTION_OPEN_UPLOADS = "de.formerdehne.wposter.OPEN_UPLOADS"

        fun start(context: Context) {
            NotificationManagerCompat.from(context).cancel(ID_RESULT)
            ContextCompat.startForegroundService(context, Intent(context, UploadService::class.java))
        }

        private fun openAppIntent(context: Context): PendingIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java)
                .setAction(ACTION_OPEN_UPLOADS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        private fun createChannels(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_PROGRESS, context.getString(R.string.channel_uploads), NotificationManager.IMPORTANCE_LOW)
            )
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_RESULT, context.getString(R.string.channel_upload_results), NotificationManager.IMPORTANCE_DEFAULT)
            )
        }

        private fun NotificationManagerCompat.notifySafe(id: Int, notification: android.app.Notification) {
            try {
                notify(id, notification)
            } catch (_: SecurityException) {
                // Keine Benachrichtigungs-Berechtigung – Upload läuft trotzdem weiter.
            }
        }
    }
}
