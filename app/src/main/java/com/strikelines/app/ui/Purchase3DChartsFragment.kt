package com.strikelines.app.ui


import android.os.Bundle
import android.view.View
import com.strikelines.app.R


class Purchase3DChartsFragment : PurchaseSqliteDbFilesFragment() {

	companion object {
		const val TITLE = R.string.charts_3d
	}


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requestChartsFromApi()
    }

    override fun onResume() {
        super.onResume()

    }





}