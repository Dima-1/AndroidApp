package com.strikelines.app.utils

import android.widget.Toast
import android.content.Intent
import android.content.BroadcastReceiver
import android.app.DownloadManager
import android.support.v4.content.ContextCompat.startActivity
import android.content.Context.DOWNLOAD_SERVICE
import android.support.v4.content.ContextCompat.getSystemService
import android.content.Context
import android.content.IntentFilter
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.view.View


object DownloadHelper{

    private var url:String = ""
    private var manager:DownloadManager? = null
    private var lastDownload = -1L
    internal var onComplete:BroadcastReceiver? = null
    internal var onNotificationClick:BroadcastReceiver? = null

    fun prepareDownload(url:String, context: Context) {
        this.url = url
        manager = context.getSystemService(DOWNLOAD_SERVICE) as DownloadManager
        onComplete = object : BroadcastReceiver() {
            override fun onReceive(ctxt: Context, intent: Intent) {

            }
        }

        onNotificationClick = object : BroadcastReceiver() {
            override fun onReceive(ctxt: Context, intent: Intent) {

            }
        }

        context.registerReceiver(
            onComplete,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        )
        context.registerReceiver(
            onNotificationClick,
            IntentFilter(DownloadManager.ACTION_NOTIFICATION_CLICKED)
        )
    }


    fun onFinishDownload(context:Context) {
        context.unregisterReceiver(onComplete)
        context.unregisterReceiver(onNotificationClick)
    }

    fun startDownload(url: String) {
        val uri = Uri.parse(url)

        Environment
            .getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            .mkdirs()

        lastDownload = manager!!.enqueue(
            DownloadManager.Request(uri)
                .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
                .setAllowedOverRoaming(false)
                .setTitle("Demo")
                .setDescription("Something useful. No, really.")
                .setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS,
                    "test.mp4"
                )
        )

    }

    fun queryStatus():String {
        val c = manager!!.query(DownloadManager.Query().setFilterById(lastDownload))

        if (c == null) {
            return "Download not found!"
        } else {
            c.moveToFirst()


            return statusMessage(c)
        }
    }

    fun viewLog(context: Context) {
        context.startActivity(Intent(DownloadManager.ACTION_VIEW_DOWNLOADS))
    }

    private fun statusMessage(c: Cursor): String =
        when (c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS))) {
            DownloadManager.STATUS_FAILED -> "Download failed!"
            DownloadManager.STATUS_PAUSED -> "Download paused!"
            DownloadManager.STATUS_PENDING -> "Download pending!"
            DownloadManager.STATUS_RUNNING -> "Download in progress!"
            DownloadManager.STATUS_SUCCESSFUL -> "Download complete!"
            else -> "Download is nowhere in sight"}

}