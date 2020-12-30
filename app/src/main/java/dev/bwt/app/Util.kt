package dev.bwt.app

import android.text.format.DateUtils
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.round

fun Date.ymd(): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        .format(this)

fun Int.fmtDur(precise: Boolean): String =
    if (precise) DateUtils.formatElapsedTime(toLong())
    else "${round(toFloat() / 60).toInt()} min"
