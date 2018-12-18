package com.strikelines.app.domain

import android.content.Context
import com.google.gson.GsonBuilder
import com.strikelines.app.PlatformUtil
import com.strikelines.app.SingletonHolder
import com.strikelines.app.domain.models.Chart
import com.strikelines.app.domain.models.Charts

class Repository private constructor(val context: Context) {

    companion object : SingletonHolder<Repository, Context>(::Repository) {

        private const val url = "https://strikelines.com/api/charts/?key=A3dgmiOM1ul@IG1N=*@q"
    }

    init {}

    private val LOG = PlatformUtil.getLog(Repository::class.java)

    private val gson by lazy { GsonBuilder().setLenient().create() }

    private val chartsList = mutableListOf<Chart>()

    var isLoading = false

    fun getGPXChart(): List<Chart> = chartsList //apply filter when available

    fun get3dcharts(): List<Chart> = chartsList //apply filter when available

    fun getBaseMaps(): List<Chart> = chartsList //apply filter when available



    private fun onRequestResult(result:String?){
        LOG.info(result)
        chartsList.addAll(parseJson(result))
    }

    private fun parseJson(response: String?): List<Chart> {
        val charts: Charts = gson.fromJson(response, Charts::class.java)
        return charts.charts
    }
}