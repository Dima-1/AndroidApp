package com.strikelines.app

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.widget.Toast
import com.google.gson.GsonBuilder
import com.strikelines.app.domain.Repository
import com.strikelines.app.domain.models.Chart
import com.strikelines.app.domain.models.Charts

class StrikeLinesApplication : Application() {

	private val uiHandler = Handler()
	lateinit var osmandHelper: OsmandHelper private set

    private val sp: SharedPreferences = this.getSharedPreferences(spName, 0)
    private val gson by lazy { GsonBuilder().setLenient().create() }
    private val chartsList = mutableListOf<Chart>()

	override fun onCreate() {
		super.onCreate()
		val context:Context = StrikeLinesApplication.applicationContext()
		osmandHelper = OsmandHelper(this)
        loadCharts()
	}

    private fun loadCharts() {
        if(sp.contains(chartsDataKey) && sp.getString(chartsDataKey, "")!!.isNotEmpty()) {

        } else {

        }
    }

    fun cleanupResources() {
		osmandHelper.cleanupResources()
	}

	fun runInUI(action: (() -> Unit)) {
		uiHandler.post(action)
	}

	fun runInUI(action: (() -> Unit), delay: Long) {
		uiHandler.postDelayed(action, delay)
	}

	fun showToastMessage(msg: String) {
		uiHandler.post { Toast.makeText(this@StrikeLinesApplication, msg, Toast.LENGTH_LONG).show() }
	}

    fun getChartsList():List<Chart> = chartsList


    private fun parseJson(response: String?): List<Chart> {
        val charts: Charts = gson.fromJson(response, Charts::class.java)
        return charts.charts
    }

    companion object {

        private const val spName = "StrikeLinesSP"
        private const val chartsDataKey = "storedCharts"
        private const val lastUpdateKey = "updateTimestamp"

        private var instance: StrikeLinesApplication? = null
        fun applicationContext(): Context = instance!!.applicationContext
    }

	init {
		instance = this
	}
}
