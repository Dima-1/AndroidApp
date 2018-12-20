package com.strikelines.app.ui

import android.os.Bundle
import android.util.Log
import android.view.View
import com.strikelines.app.R
import com.strikelines.app.StrikeLinesApplication
import com.strikelines.app.StrikeLinesApplication.Companion.applicationContext
import com.strikelines.app.domain.RepoCallback
import com.strikelines.app.domain.Repository
import com.strikelines.app.domain.models.Chart

class PurchaseGpxFilesFragment : PurchaseSqliteDbFilesFragment() {



	companion object {
		const val TITLE = R.string.gpx_charts
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

    override fun sortResults(results: List<Chart>) = results.filterNot { it.name.contains("3D ") }.filterNot { it.price.toInt() == 0 }
}