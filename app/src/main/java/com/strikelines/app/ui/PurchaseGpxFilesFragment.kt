package com.strikelines.app.ui

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.strikelines.app.R

class PurchaseGpxFilesFragment : PurchaseSqliteDbFilesFragment() {

	companion object {
		const val TITLE = R.string.gpx_charts
	}

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?): View? {

		val activity = activity

		val view = inflater.inflate(R.layout.recycler_list_fragment, container, false)

		return view;
	}
}