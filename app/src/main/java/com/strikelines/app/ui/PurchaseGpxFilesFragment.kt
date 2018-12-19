package com.strikelines.app.ui

import android.os.Bundle
import android.util.Log
import android.view.View
import com.strikelines.app.R
import com.strikelines.app.domain.models.Chart

class PurchaseGpxFilesFragment : PurchaseSqliteDbFilesFragment() {

	companion object {
		const val TITLE = R.string.gpx_charts
	}

	override fun onRequestResult(result: String) {
		chartsList.addAll(sortResults(parseJson(result)))

		adapter.setData(chartsList)
	}

    override fun sortResults(results: List<Chart>) = results.filterNot { it.name.contains("3D ") }
}