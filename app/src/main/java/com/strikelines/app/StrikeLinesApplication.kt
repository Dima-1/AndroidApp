package com.strikelines.app

import android.app.Application
import android.content.Context
import android.os.Handler
import android.widget.Toast
import com.strikelines.app.domain.Repository

class StrikeLinesApplication : Application() {

	private val uiHandler = Handler()
	lateinit var osmandHelper: OsmandHelper private set

	override fun onCreate() {
		super.onCreate()
		val context:Context = StrikeLinesApplication.applicationContext()
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

	fun showToastMessage(msg: String) {
		uiHandler.post { Toast.makeText(this@StrikeLinesApplication, msg, Toast.LENGTH_LONG).show() }
	}

    companion object {
        private var instance: StrikeLinesApplication? = null

        fun applicationContext(): Context = instance!!.applicationContext

    }

	init {
		instance = this
	}
}
