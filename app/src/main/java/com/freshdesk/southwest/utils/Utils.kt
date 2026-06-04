package com.freshdesk.southwest.utils

import android.content.Context
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.freshdesk.southwest.TAG
import com.freshdesk.southwest.data.DataStore.getToastEventsState

// private val validMapPattern =
//    Regex("^\\s*\\S+\\s*:\\s*[^\\s][^,]*\\s*(,\\s*\\S+\\s*:\\s*[^\\s][^,]*\\s*)*\$\n")

fun String.toMap(): Map<String, Any> {
    return try {
        if (this.isNotEmpty()) {
            split(", ").map { it.split(": ") }.associate { (key, value) -> key to value }
        } else {
            emptyMap()
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error converting string to map: $e")
        emptyMap()
    }
}

fun Context.toast(message: String) {
    if (Looper.getMainLooper() == Looper.myLooper()) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    } else {
        Log.d("Toast (Non main thread)", message)
    }
}

fun Context.logEvent(event: String, data: String, toastData: Boolean = false) {
    if (getToastEventsState()) {
        val toastMessage = if (toastData && data.isNotEmpty()) "$event : $data" else event
        toast(toastMessage)
    }
    Log.d(TAG, "$event: $data")
}
