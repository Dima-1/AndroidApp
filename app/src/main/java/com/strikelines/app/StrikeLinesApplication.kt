package com.strikelines.app

import android.app.Application

class StrikeLinesApplication : Application() {

	lateinit var osmandHelper: OsmandHelper private set

	override fun onCreate() {
		super.onCreate()
		osmandHelper = OsmandHelper(this)
	}

	fun cleanupResources() {
		osmandHelper.cleanupResources()
	}
}
