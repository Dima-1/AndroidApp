package com.strikelines.app

import android.app.Application
import android.os.Handler

class StrikeLinesApplication : Application() {

	private val uiHandler = Handler()
	lateinit var osmandHelper: OsmandHelper private set

	override fun onCreate() {
		super.onCreate()
		osmandHelper = OsmandHelper(this)
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
}
