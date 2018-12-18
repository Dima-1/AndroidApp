package com.strikelines.app.ui

import android.os.Bundle
import android.util.Log
import android.view.View
import com.strikelines.app.R

class PurchaseGpxFilesFragment : PurchaseSqliteDbFilesFragment() {

	companion object {
		const val TITLE = R.string.gpx_charts
	}

	override fun onRequestResult(result: String) {
		chartsList.addAll(parseJson(result))
		chartsList.forEach { Log.d("Chart", it.name) }
		adapter.setData(chartsList)
	}

}