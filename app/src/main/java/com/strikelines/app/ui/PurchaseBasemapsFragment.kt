package com.strikelines.app.ui

import android.os.Bundle
import com.strikelines.app.R
import com.strikelines.app.domain.models.Chart

class PurchaseBasemapsFragment : PurchaseSqliteDbFilesFragment() {

	companion object {
		const val TITLE = R.string.basemaps
	}

	var fragmentNotifier: MainActivity.FragmentDataNotifier? = null

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		fragmentNotifier = object : MainActivity.FragmentDataNotifier {
			override fun onDataReady(status: Boolean) =  getData()
		}
	}

	override fun onResume() {
		super.onResume()
		MainActivity.fragmentNotifier[TITLE] = fragmentNotifier
	}

	override fun onPause() {
		super.onPause()
		MainActivity.fragmentNotifier.remove(TITLE)
	}

	override fun sortResults(results: List<Chart>): List<Chart> {
		val filteredResults = results.filter { it.price.toInt() == 0 }
		activity?.let {
			if ((activity as MainActivity).regionToFilter != "")
				return filteredResults.filter { it.region == (activity!! as MainActivity).regionToFilter }
		}
		return filteredResults
	}

}