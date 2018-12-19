package com.strikelines.app.ui

import android.os.Bundle
import android.util.Log
import android.view.View
import com.strikelines.app.R
import com.strikelines.app.domain.models.Chart

class PurchaseBasemapsFragment : PurchaseSqliteDbFilesFragment() {

	companion object {
		const val TITLE = R.string.basemaps
	}

	override fun onRequestResult(result: String) {


		chartsList.addAll(sortResults(parseJson(result)))
		adapter.setData(chartsList)
	}

	override fun sortResults(results: List<Chart>) = listOf<Chart>()

}