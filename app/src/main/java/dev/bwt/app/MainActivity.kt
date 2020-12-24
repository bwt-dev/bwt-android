package dev.bwt.app

import android.content.Intent
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.preference.PreferenceManager
import androidx.work.*
import com.google.gson.Gson
import dev.bwt.daemon.BwtConfig

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))

        observeWorker()
        observeLogs(findViewById(R.id.logview))

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

        Log.d("bwt-main", "starting with config $config")

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

        return BwtConfig(
            network = getStr("network"),
            bitcoindUrl = getStr("bitcoind_url"),
            bitcoindAuth = getStr("bitcoind_auth"),
            bitcoindWallet = getStr("bitcoind_wallet"),
            descriptors = getStr("descriptors")?.let { it.lines().toTypedArray() },
            xpubs = getStr("xpubs")?.let { it.lines().toTypedArray() },
            rescanSince = if (pref.getBoolean("rescan", false)) getInt("rescan_since") else null,
            gapLimit = getInt("gap_limit"),
            initialImportSize = getInt("initial_import_size"),
            pollInterval = getInt("poll_interval")?.let { arrayOf(it, 0) },
            electrumAddr = getStr("electrum_addr"),
            httpAddr = "0.0.0.0:3060", // getStr("http_addr"),
            verbose = getInt("verbose"),
        )
        // FIXME check for missing config options
    }

    private fun observeWorker() {
        WorkManager.getInstance(applicationContext)
            .getWorkInfosForUniqueWorkLiveData(WORK_NAME)
            .observe(this, Observer { workInfos: List<WorkInfo> ->
                Log.d("bwt-main", "observed ${workInfos.size} workers")
                if (workInfos.isNotEmpty() && !workInfos[0].state.isFinished) {
                    val progress = workInfos[0].progress
                    val fProgress = progress.getFloat("PROGRESS", 0.0f)

                    when (progress.getString("TYPE")) {
                        "booting" -> Log.d("bwt-main", "worker progress booting")
                        "scan" -> Log.d("bwt-main", "worker progress scan")
                        "sync" -> Log.d("bwt-main", "worker progress sync")
                        "ready" -> Log.d("bwt-main", "worker progress ready")
                    }
                } else {
                    Log.d("bwt-main", "worker inactive")
                }
                // TODO check workInfo.outputData for errors
            })
    }

    private fun observeLogs(logView: TextView) {
        val logCatViewModel by viewModels<LogCatViewModel>()
        logView.movementMethod = ScrollingMovementMethod() // auto scroll
        logCatViewModel.logCatOutput().observe(this, Observer { logMessage ->
            logView.append("$logMessage\n")
        })
    }

    private fun stopBwt() {
        /*with (PreferenceManager.getDefaultSharedPreferences(this).edit()) {
            putBoolean("run", false)
            commit()
        }*/

        WorkManager.getInstance(applicationContext).cancelUniqueWork(WORK_NAME)
    }

    fun onClickSettings(view: View) {
        Log.d("bwt-main", "mainActivity onClickSettings")
        startActivity(Intent(this, SettingsActivity::class.java))
    }

    fun onClickStart(view: View) {
        Log.d("bwt-main", "mainActivity onClickStart")
        startBwt()
    }

    companion object {
        const val WORK_NAME = "bwt-daemon"
    }
}