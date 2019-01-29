package com.strikelines.app.utils

import android.content.Context
import android.os.AsyncTask
import android.os.Environment
import android.util.Log
import java.io.*
import java.lang.Exception
import java.net.HttpURLConnection
import java.net.URL


class GetRequestAsync(private val url: String, private val listener: OnRequestResultListener) :
    AsyncTask<Void, Void, String>() {

    override fun doInBackground(vararg p0: Void?): String? {
        try {
            return getRequest(url)
        } catch (e: Exception) {
            Log.w(e.message, e)
        }
        return "Request Failed!"
    }

    override fun onPostExecute(result: String) {
        //Log.d("Async results", result)
        listener.onResult(result)
    }

    private fun getRequest(url: String): String {
        val obj = URL(url)
        val response = StringBuilder()
        with(obj.openConnection() as HttpURLConnection) {
            requestMethod = "GET"

            Log.d("Response Code", responseCode.toString())

            BufferedReader(InputStreamReader(inputStream), 1024).use {
                var inputLine = it.readLine()
                while (inputLine != null) {
                    response.append(inputLine)
                    inputLine = it.readLine()
                }
                it.close()
            }

        }
        return if (response.toString().isNotEmpty()) response.toString() else "Request Failed!"
    }
}

interface OnRequestResultListener {
    fun onResult(result: String)
}

class DownloadFileAsync(private val downloadUrl: String, private val downloadCallback: DownloadCallback) : AsyncTask<String, String, String>() {

    private val path = "${Environment.getExternalStorageDirectory().absolutePath}/strikelines/"
    private val fileName = downloadUrl.substringBeforeLast('/').substringAfterLast('/') +
            ".${downloadUrl.substringAfterLast('.')}"

    override fun doInBackground(vararg params: String?): String {
        try {
            val dir = File(path)
            if (!dir.exists()) dir.mkdir()
            val file = File(dir, fileName)

            val urlcon = URL(downloadUrl).openConnection()
            urlcon.readTimeout = 60000
            urlcon.connectTimeout = 60000
            urlcon.connect()
            val inputStream = BufferedInputStream(urlcon.getInputStream(), 4096)

            val outputStream = FileOutputStream(file)
            val data = ByteArray(1024)
            var total: Long = 0
            var count = inputStream.read(data)

            while (count != -1) {
                total += count.toLong()
                outputStream.write(data, 0, count)
                count = inputStream.read(data)
            }

            outputStream.flush()
            outputStream.close()
            inputStream.close()
            downloadCallback.onDownloadComplete(fileName, path, true)
        } catch (e: Exception) {
            downloadCallback.onDownloadComplete(fileName, path, false)
            Log.w(e.message, e)
        }
        return ""
    }
}

interface DownloadCallback{
    fun onDownloadComplete(fileName:String, filePath:String, isSuccess:Boolean)
}