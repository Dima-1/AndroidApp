package com.strikelines.app.ui

import android.os.Bundle
import com.strikelines.app.R
import com.strikelines.app.domain.models.Chart

class PurchaseGpxFilesFragment : PurchaseSqliteDbFilesFragment() {


	companion object {
		const val TITLE = R.string.gpx_charts
	}

	var fragmentNotifier: MainActivity.FragmentDataNotifier? = null

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		fragmentNotifier = object : MainActivity.FragmentDataNotifier {
			override fun onDataReady(status: Boolean) {
				getData()
			}
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
		val filteredResults = results.filterNot { it.name.contains("3D ") }
			.filterNot { it.price.toInt() == 0 }
		activity?.let {
			if ((activity as MainActivity).regionToFilter != "")
				return filteredResults.filter { it.region == (activity!! as MainActivity).regionToFilter }
		}

		return filteredResults
	}
}