package com.strikelines.app.ui

import android.os.Bundle
import android.util.Log
import android.view.View
import com.strikelines.app.R
import com.strikelines.app.StrikeLinesApplication
import com.strikelines.app.domain.Repository
import com.strikelines.app.domain.models.Chart

class PurchaseBasemapsFragment : PurchaseSqliteDbFilesFragment() {


	companion object {
		const val TITLE = R.string.basemaps
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		if(repo?.chartsList!!.isNotEmpty())
			onRequestResult(repo?.chartsList!!)

	}

	override fun onRequestResult(result: List<Chart>) {
		chartsList.addAll(sortResults(result))
		adapter.setData(chartsList)
	}

	override fun sortResults(results: List<Chart>) = results.filter { it.price.toInt() == 0 }

}