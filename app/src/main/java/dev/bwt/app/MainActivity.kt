package dev.bwt.app

import android.content.Intent
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.preference.PreferenceManager
import androidx.work.*
import com.google.gson.Gson
import dev.bwt.daemon.BwtConfig
import java.util.*

class MainActivity : AppCompatActivity() {
    private val logCatViewModel by viewModels<LogCatViewModel>()
    private lateinit var logView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))

        logView = findViewById(R.id.logview)

        observeWorker()
        observeLogs()

        if (intent.action == "dev.bwt.app.START_BWT") {
            startBwt()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun startBwt() {
        val config = getConfig()

        Log.i("bwt-main", "Starting with config ${scrubConfig(config)}")

        findViewById<TextView>(R.id.text_status).text = "Starting bwt..."
        clearLog()

        val bwtWorkRequest = OneTimeWorkRequestBuilder<BwtWorker>()
            .setInputData(workDataOf("JSON_CONFIG" to Gson().toJson(config)))
            .build()
        WorkManager.getInstance(applicationContext).enqueueUniqueWork(
            WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            bwtWorkRequest
        )
    }

    private fun getConfig(): BwtConfig {
        val pref = PreferenceManager.getDefaultSharedPreferences(this)
        val getStr = { name: String -> if (pref.contains(name)) pref.getString(name, "") else null }
        val getInt = { name: String -> if (pref.contains(name)) pref.getInt(name, 0) else null }

        val getStrIf = { bool_name: String, val_name: String ->
            if (pref.getBoolean(bool_name, false)) getStr(val_name) else null }
        val getIntIf = { bool_name: String, val_name: String ->
            if (pref.getBoolean(bool_name, false)) getInt(val_name) else null }

        return BwtConfig(
            network = getStr("network"),
            bitcoindUrl = getStr("bitcoind_url"),
            bitcoindAuth = getStr("bitcoind_auth"),
            bitcoindWallet = getStr("bitcoind_wallet"),
            descriptors = getStr("descriptors")?.let { getLines(it) },
            xpubs = getStr("xpubs")?.let { getLines(it) },
            rescanSince = getIntIf("rescan", "rescan_since"),
            gapLimit = getInt("gap_limit"),
            initialImportSize = getInt("initial_import_size"),
            pollInterval = getInt("poll_interval")?.let { arrayOf(it, 0) },
            electrumAddr = getStrIf("electrum", "electrum_addr"),
            httpAddr = getStrIf("http", "http_addr"),
            verbose = getInt("verbose"),
        )
    }

    private fun observeWorker() {
        val btnStart = findViewById<Button>(R.id.button_start)!!
        val btnStop = findViewById<Button>(R.id.button_stop)
        val textStatus = findViewById<TextView>(R.id.text_status)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)

        WorkManager.getInstance(applicationContext)
            .getWorkInfosForUniqueWorkLiveData(WORK_NAME)
            .observe(this, Observer { workInfos: List<WorkInfo> ->
                Log.v("bwt-main", "observed worker: ${workInfos.elementAtOrNull(0)}")
                val pendingSettingsUpdate = AppState.checkSettingsChanged()

                if (workInfos.isNotEmpty() && !workInfos[0].state.isFinished) {
                    // If the daemon is running and the settings were just updated, re-start it
                    if (pendingSettingsUpdate) {
                        Toast.makeText(applicationContext, "Setting updated, restarting daemon...", Toast.LENGTH_LONG).show()
                        startBwt()
                        return@Observer
                    }

                    val progress = workInfos[0].progress
                    var type = progress.getString("TYPE")
                    val nProgress = progress.getFloat("PROGRESS", 0.0f)
                    val nProgressStr = "%.1f".format(nProgress * 100)

                    Log.v("bwt-main", "worker progress $progress")

                    // switch back to a generic "Booting up" message and an indeterminate
                    //  progress bar when syncing/scanning is completed
                    if ((type == "scan" || type == "sync") && nProgress == 1.0f) {
                        type = "booting"
                    }

                    when (type) {
                        "booting" -> {
                            AppState.setIsRunning()
                            textStatus.text = "Booting up..."
                            progressBar.visibility = View.VISIBLE
                            progressBar.isIndeterminate = true
                        }
                        "scan" -> {
                            val eta = progress.getInt("ETA", 0)
                            val etaStr = eta.fmtDur(nProgress > 0.4)
                            progressBar.isIndeterminate = false
                            progressBar.progress = (nProgress * 100).toInt()
                            textStatus.text = "Wallet scanning in progress... $nProgressStr% done, $etaStr remaining"
                        }
                        "sync" -> {
                            val tip = Date(progress.getInt("TIP", 0).toLong())
                            progressBar.isIndeterminate = false
                            progressBar.progress = (nProgress * 100).toInt()
                            textStatus.text = "Block syncing in progress... $nProgressStr% done, tip at ${tip.ymd()}"
                        }
                        "ready" -> {
                            val services = progress.getStringArray("SERVICES")
                            textStatus.text = "Daemon running (${services?.joinToString(" + ")})"
                            progressBar.visibility = View.INVISIBLE
                        }
                        null -> {}
                        else -> throw RuntimeException("Unknown worker message $type")
                    }

                    btnStart.visibility = View.INVISIBLE
                    btnStop.visibility = View.VISIBLE
                } else {
                    AppState.setNotRunning()

                    btnStart.visibility = View.VISIBLE
                    btnStop.visibility = View.INVISIBLE
                    progressBar.visibility = View.INVISIBLE

                    val errorMessage = workInfos.elementAtOrNull(0)?.outputData?.getString("ERROR")
                    if (errorMessage != null) {
                        textStatus.text = "Error: $errorMessage"
                    } else {
                        textStatus.text = "Not running"
                    }
                }
            })
    }

    private fun observeLogs() {
        logView.movementMethod = ScrollingMovementMethod() // auto scroll

        logCatViewModel.logCatOutput().observe(this, Observer { logMessage ->
            logView.append("$logMessage\n")
        })
    }
    private fun clearLog() {
        logCatViewModel.clearLog()
        logView.text = ""
    }

    private fun stopBwt() {
        /*with (PreferenceManager.getDefaultSharedPreferences(this).edit()) {
            putBoolean("run", false)
            commit()
        }*/

        findViewById<TextView>(R.id.text_status).text = "Stopping bwt..."
        WorkManager.getInstance(applicationContext).cancelUniqueWork(WORK_NAME)
    }

    @Suppress("UNUSED_PARAMETER")
    fun onClickSettings(view: View) {
        startActivity(Intent(this, SettingsActivity::class.java))
    }

    @Suppress("UNUSED_PARAMETER")
    fun onClickStart(view: View) {
        startBwt()
    }

    @Suppress("UNUSED_PARAMETER")
    fun onClickStop(view: View) {
        stopBwt()
    }

    companion object {
        const val WORK_NAME = "bwt-daemon"
    }
}

private fun getLines(str: String): Array<String> =
    str.lines().filter { it.isNotBlank() }.toTypedArray()

private fun scrubConfig(config: BwtConfig): String {
    var str = config.toString()
    config.bitcoindAuth?.let {
        str = str.replace(it, "**SCRUBBED**")
    }
    return str
}
