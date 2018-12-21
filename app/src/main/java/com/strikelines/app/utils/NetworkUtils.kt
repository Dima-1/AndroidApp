package com.strikelines.app.utils

import android.os.AsyncTask
import android.util.Log
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.lang.Exception
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL



class GetRequestAsync(private val url: String, private val listener: OnRequestResultListener) : AsyncTask<Void, Void, String>() {

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

