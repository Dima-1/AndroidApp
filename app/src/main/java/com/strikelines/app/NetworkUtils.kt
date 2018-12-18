package com.strikelines.app

import android.os.AsyncTask
import android.util.Log
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.lang.StringBuilder
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import java.nio.Buffer


interface OnRequestResultListener {
    fun onRequest(status: Boolean)
    fun onResult(result: String)
}

class GetRequestAsync(private val url: String, private val listener: OnRequestResultListener) : AsyncTask<Void, Void, String>() {

    override fun onPreExecute() {
        listener.onRequest(true)
        super.onPreExecute()
    }

    override fun doInBackground(vararg p0: Void?): String? {
        var connection: HttpURLConnection? = null
        try {
            connection = getHttpURLConnection(url)
            connection.setRequestProperty("Content-length", "0")
            connection.connectTimeout = 30000
            connection.readTimeout = 15000
            connection.requestMethod = "GET"
            connection.connect()

            when (connection.responseCode) {
                HttpURLConnection.HTTP_OK -> {

                    val inStream: InputStream? = connection.inputStream
                    val buffer = BufferedReader(InputStreamReader(inStream))
                    val responseBody = StringBuilder()

                    var line:String? = null
                    var f = true
                    while ({line = buffer.readLine(); line}()!= null) {
                        if(!f) {
                            responseBody.append("\n")
                        } else {
                            f = false
                        }
                        responseBody.append(line)
                    }

                    try {
                        inStream?.close()
                        buffer.close()
                    } catch (e: Exception) {
                        // ignore exception
                    }
                    Log.d("Response", responseBody.toString())
                    return responseBody.toString()
                }

                else -> {
                    return "Request Failed!"
                }
            }

        } catch (e: NullPointerException) {
            e.printStackTrace()

        } catch (e: MalformedURLException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            connection?.disconnect()

        }
        return "Request Failed!"
    }

    override fun onPostExecute(result: String) {
        Log.d("Async results", result)
        listener.onRequest(false)
        listener.onResult(result)
    }


}


//Copy from NetworkUtils
@Throws(MalformedURLException::class, IOException::class)
private fun getHttpURLConnection(urlString: String): HttpURLConnection {
    return getHttpURLConnection(URL(urlString))
}

//Copy from NetworkUtils
@Throws(IOException::class)
private fun getHttpURLConnection(url: URL): HttpURLConnection {
    return url.openConnection() as HttpURLConnection

}

