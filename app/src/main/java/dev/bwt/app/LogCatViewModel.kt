package dev.bwt.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers

var CMD =
    "logcat -b main -v tag {OPT} '*:V' bwt-daemon:D bwt-main:D| grep --line-buffered -E '^[A-Z]/(bwt|bitcoin)'"

class LogCatViewModel : ViewModel() {
    fun logCatOutput() = liveData(viewModelScope.coroutineContext + Dispatchers.IO) {
        // Read the last 100 messages and pass them in one go, then stream new messages one at a time.
        // This is done to avoid a lag when appending the messages to the TextView.

        sh(CMD.replace("{OPT}", "-T 100 -d"))
            .inputStream
            .bufferedReader()
            .useLines { lines ->
                emit(lines.joinToString("\n"))
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