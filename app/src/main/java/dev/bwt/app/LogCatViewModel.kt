package dev.bwt.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers

var CMD =
    "logcat -b main -v tag {OPT} '*:V' bwt-daemon:D bwt-main:D | grep --line-buffered -E '^[A-Z]/(bwt|bitcoin)'"

class LogCatViewModel : ViewModel() {
    fun logCatOutput() = liveData(viewModelScope.coroutineContext + Dispatchers.IO) {
        // Read the last 100 messages and pass them in one go, then stream new messages one at a time.
        // This is done to avoid a lag when appending messages to the TextView one-by-one.

        sh(CMD.replace("{OPT}", "-T 500 -d"))
            .inputStream
            .bufferedReader()
            .useLines { lines ->
                // Grab the last 500 messages, then grep for relevant ones, then take the last 100 messages
                // Needed because there's a high percentage of non-relevant messages
                emit(lines.toList().takeLast(100).joinToString("\n"))
            }

        sh(CMD.replace("{OPT}", "-T 1"))
            .inputStream
            .bufferedReader()
            .useLines { lines ->
                lines.forEach { line -> emit(line) }
            }
    }
}

fun sh(cmd: String): Process = Runtime.getRuntime().exec(arrayOf("sh", "-c", cmd))