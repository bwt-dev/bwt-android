package dev.bwt.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.TaskStackBuilder
import androidx.work.*
import com.google.gson.Gson
import dev.bwt.daemon.BwtConfig
import dev.bwt.daemon.BwtDaemon
import dev.bwt.daemon.BwtException
import dev.bwt.daemon.ProgressNotifier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.text.SimpleDateFormat
import java.util.*

class BwtWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override suspend fun doWork(): Result {
        val jsonConfig = inputData.getString("JSON_CONFIG") ?: return Result.failure()
        val config = Gson().fromJson(jsonConfig, BwtConfig::class.java)
        val bwt = BwtDaemon(config)

        val callback = object : ProgressNotifier {
            override fun onBooting() {
                setProgressAsync(
                    workDataOf(
                        "TYPE" to "booting",
                        "PROGRESS" to 0,
                    )
                )
                Log.d("bwt-worker", "bwt starting up")
            }
            override fun onScanProgress(progress: Float, eta: Int) {
                val progressStr = "%.1f".format(progress)
                val etaMinStr = "%.1f".format(eta / 60)
                setProgressAsync(
                    workDataOf(
                        "TYPE" to "scan",
                        "PROGRESS" to progress,
                        "ETA" to eta
                    )
                )
                setForegroundAsync(createForegroundInfo("History scanning in progress... ${progressStr}% done, eta ${etaMinStr} minute(s)"))
                Log.d("bwt-worker", "scan progress ${progressStr}%, eta ${etaMinStr} minute(s)")
            }

            override fun onSyncProgress(progress: Float, tip: Date) {
                val progressStr = "%.1f".format(progress)
                val tipStr = fmtDate(tip)
                setProgressAsync(
                    workDataOf(
                        "TYPE" to "sync",
                        "PROGRESS" to progress,
                        "TIP" to tip.time,
                        "TIP_STR" to tipStr,
                    )
                )
                setForegroundAsync(createForegroundInfo("Node syncing in progress... ${progressStr}% done, up to ${tipStr}"))
                Log.d("bwt-worker", "sync progress ${progressStr}% up to ${tipStr}")
            }

            override fun onReady(bwt: BwtDaemon) {
                Log.d("bwt-worker", "servers ready")
                setProgressAsync(
                    workDataOf(
                        "TYPE" to "ready",
                        "PROGRESS" to 1.0,
                        "ELECTRUM_ADDR" to bwt.electrumAddr,
                        "HTTP_ADDR" to bwt.httpAddr,
                        )
                )
                setForegroundAsync(createForegroundInfo("Bitcoin Wallet Tracker is running."))
            }
        }

        return coroutineScope {
            try {
                setForeground(createForegroundInfo("Starting Bitcoin Wallet Tracker..."))
                Log.d("bwt-worker", "starting up")
                val job = async { bwt.start(callback) }
                job.await()
                Result.success()
            } catch (e: CancellationException) {
                Log.e("bwt-worker", "worker canceled")
                bwt.shutdown()
                Log.e("bwt-worker", "shut down")
                Result.success()
            } catch (e: BwtException) {
                Log.e("bwt-worker", "bwt error: ${e.message}")
                bwt.shutdown()
                reportError(e)
                Result.retry()
            } catch (e: Exception) {
                Log.e("bwt-worker", "error: $e")
                reportError(e)
                bwt.shutdown()
                Result.failure(workDataOf("ERROR" to e))
            }
        }
    }

    private suspend fun reportError(e: Exception) {
        setProgress(workDataOf("TYPE" to "error", "MESSAGE" to e.message))
    }

    private fun createForegroundInfo(text: String): ForegroundInfo {
        val openIntent = createNotifyOpenIntent()
        val cancelIntent = WorkManager.getInstance(applicationContext)
            .createCancelPendingIntent(id)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannel()
        }

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Bitcoin Wallet Tracker")
            .setTicker("Bitcoin Wallet Tracker")
            .setContentText(text)
            //.setSmallIcon(R.drawable.ic_work_notification)
            .setSmallIcon(android.R.drawable.sym_def_app_icon)
            .setOngoing(true)
            .setContentIntent(openIntent)
            .addAction(android.R.drawable.ic_dialog_info, "Open", openIntent)
            .addAction(android.R.drawable.ic_delete, "Stop", cancelIntent)
            .build()

        return ForegroundInfo(NOTIFICATION_ID, notification)
    }

    private fun createNotifyOpenIntent(): PendingIntent? {
        val notifyIntent = Intent(applicationContext, MainActivity::class.java)
        return TaskStackBuilder.create(applicationContext).run {
            addNextIntentWithParentStack(notifyIntent)
            getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createChannel() {
        // XXX which priority level?
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Bitcoin Wallet Tracker",
            NotificationManager.IMPORTANCE_MIN
        )
        notificationManager.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "BWT"
        const val NOTIFICATION_ID = 1
    }
}

private fun fmtDate(date: Date): String {
    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return formatter.format(date)
}