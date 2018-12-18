package com.strikelines.app.ui

import android.os.Bundle
import android.util.Log
import android.view.View
import com.strikelines.app.R

class PurchaseBasemapsFragment : PurchaseSqliteDbFilesFragment() {

	companion object {
		const val TITLE = R.string.basemaps
	}

	override fun onRequestResult(result: String) {
		chartsList.addAll(parseJson(result))
		chartsList.forEach { Log.d("Chart", it.name) }
		adapter.setData(chartsList)
	}

}