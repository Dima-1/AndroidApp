package com.strikelines.app.ui

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.strikelines.app.R

abstract class PurchaseSqliteDbFilesFragment : Fragment() {

	companion object {
		const val TITLE = 0
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