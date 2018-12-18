package com.strikelines.app

import android.os.AsyncTask
import android.util.Log
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL



class GetRequestAsync(private val url: String, private val listener: OnRequestResultListener) : AsyncTask<Void, Void, String>() {

    override fun doInBackground(vararg p0: Void?): String? {
        try {
            return getRequest(url)
        } catch (e: NullPointerException) {
            e.printStackTrace()

        } catch (e: MalformedURLException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return "Request Failed!"
    }

    override fun onProgressUpdate(vararg values: Void?) {
        super.onProgressUpdate(*values)
        listener.onRequest(true)
    }

    override fun onPostExecute(result: String) {
        Log.d("Async results", result)
        listener.onRequest(false)
        listener.onResult(result)
    }

    fun getRequest(url: String): String {
        val obj = URL(url)
        val response = StringBuilder()
        with(obj.openConnection() as HttpURLConnection) {
            requestMethod = "GET"

            println("URL : $url")
            println("Response Code : $responseCode")

            BufferedReader(InputStreamReader(inputStream), 1024).use {
                var inputLine = it.readLine()
                while (inputLine != null) {

                    response.append(inputLine)
                    inputLine = it.readLine()
                }

                it.close()
            }

        }
        return if (response.toString().isNotEmpty()) response.toString() else ""
    }
}

interface OnRequestResultListener {
    fun onRequest(status: Boolean)
    fun onResult(result: String)
}

