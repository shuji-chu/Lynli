package com.github.kr328.clash.utity

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.github.kr328.clash.design.R

object LoadingDialog {
    private const val TAG = "LoadingDialog"
    private const val AUTO_HIDE_MS = 30_000L

    private var dialog: AlertDialog? = null
    private val handler = Handler(Looper.getMainLooper())
    private var autoHideTask: Runnable? = null

    fun show(context: Context, message: String = "请稍等...") {
        handler.post {
            try {
                if (dialog == null) {
                    val inflater = LayoutInflater.from(context)
                    val view: View = inflater.inflate(R.layout.loading_dialog, null)
                    val messageTextView: TextView = view.findViewById(R.id.loadingMessage)
                    messageTextView.text = message
                    dialog = AlertDialog.Builder(context)
                        .setCancelable(false)
                        .setView(view)
                        .create()
                    dialog?.show()
                }
                scheduleAutoHide()
            } catch (e: Throwable) {
                Log.e(TAG, "show failed", e)
                dialog = null
            }
        }
    }

    private fun scheduleAutoHide() {
        autoHideTask?.let { handler.removeCallbacks(it) }
        val task = Runnable {
            Log.w(TAG, "auto hide after ${AUTO_HIDE_MS}ms")
            hide()
        }
        autoHideTask = task
        handler.postDelayed(task, AUTO_HIDE_MS)
    }

    fun hide() {
        handler.post {
            try {
                autoHideTask?.let { handler.removeCallbacks(it) }
                autoHideTask = null
                dialog?.dismiss()
            } catch (e: Throwable) {
                Log.e(TAG, "hide failed", e)
            }
            dialog = null
        }
    }
}
