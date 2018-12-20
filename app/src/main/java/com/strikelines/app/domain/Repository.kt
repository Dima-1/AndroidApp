package com.strikelines.app.domain

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.GsonBuilder
import com.strikelines.app.utils.PlatformUtil
import com.strikelines.app.utils.SingletonHolder
import com.strikelines.app.domain.models.Chart
import com.strikelines.app.domain.models.Charts
import com.strikelines.app.utils.GetRequestAsync
import com.strikelines.app.utils.OnRequestResultListener
import java.io.IOException

class Repository private constructor(val context: Context, val callback: RepoCallback) {

    companion object : SingletonHolder<Repository, Context, RepoCallback>(::Repository) {
        private const val spName = "StrikeLinesSP"
        private const val chartsDataKey = "storedCharts"
        private const val url = "https://strikelines.com/api/charts/?key=A3dgmiOM1ul@IG1N=*@q"
    }

    init {}

    private val LOG = PlatformUtil.getLog(Repository::class.java)

    private val gson by lazy { GsonBuilder().setLenient().create() }

    val chartsList = mutableListOf<Chart>()

    private val sp:SharedPreferences = context.getSharedPreferences(spName, 0)

    fun requestCharts(){
            if(sp.contains(chartsDataKey) && sp.getString(chartsDataKey, "{}")!="{}") {
                Log.i("Repo requestCharts", sp.getString(chartsDataKey, "{}"))

                chartsList.addAll(parseJson(sp.getString(chartsDataKey, "{}")))
                callback.isResourcesLoading(false)
                callback.onLoadingComplete("Data from SP loaded")
                updateStoredData()
            } else {
                callback.isResourcesLoading(true)
                callback.onLoadingComplete("Data is loading from API")
                updateStoredData()
            }
        Log.i("chartsList in repo", chartsList.toString())
    }

    private fun updateStoredData(){
        chartsList.addAll(parseJson(loadJSONFromAsset()))
        callback.isResourcesLoading(false)
        callback.onLoadingComplete("Data from SP loaded")
        //Since there are some quantum fluctuations on the way to StrikeLines server
        //GetRequestAsync(url, cardListener).execute()
    }

    private fun onRequestResult(result:String?){
        LOG.info(result)
        if(result != null && result!= "Request Failed!"){
            sp.edit().putString(chartsDataKey,result).apply()
            callback.onLoadingComplete("Data updated")
        } else {
            callback.onLoadingComplete("Data update failed!")
        }
        callback.isResourcesLoading(false)

        //chartsList.addAll(parseJson(result))
    }

    private fun parseJson(response: String?): List<Chart> {
        val charts: Charts = gson.fromJson(response, Charts::class.java)
        return charts.charts
    }

    private val cardListener = object : OnRequestResultListener {
        override fun onRequest(status: Boolean) {

        }

        override fun onResult(result: String) {
            onRequestResult(result = result)
        }
    }

    private fun loadJSONFromAsset(): String? {
        var json: String? = null
        try {
            val inputStream = context.assets.open("json/strike.json")
            val size = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()
            json = String(buffer)
        } catch (ex: IOException) {
            ex.printStackTrace()
            return null
        }

        return json
    }
}