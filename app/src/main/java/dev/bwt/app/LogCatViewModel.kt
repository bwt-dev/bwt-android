package dev.bwt.app

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers

// https://stackoverflow.com/questions/12692103/read-logcat-programmatically-within-application

class LogCatViewModel : ViewModel() {
    fun logCatOutput() = liveData(viewModelScope.coroutineContext + Dispatchers.IO) {
        // Runtime.getRuntime().exec("logcat -c")
        Runtime.getRuntime().exec(arrayOf("sh", "-c", "logcat -b main -v tag -T 100 | grep --line-buffered -E '^[A-Z]/(bwt|bitcoin)'"))
            .inputStream
            .bufferedReader()
            .useLines { lines ->
                lines.forEach { line -> emit(line) }
            }
    }
}

