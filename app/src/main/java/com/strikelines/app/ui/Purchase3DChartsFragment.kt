package com.strikelines.app.ui


import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.strikelines.app.R


class Purchase3DChartsFragment : PurchaseSqliteDbFilesFragment() {


    companion object {
		const val TITLE = R.string.charts_3d

	}

    override fun onRequestResult(result: String) {
        chartsList.addAll(parseJson(result))
        chartsList.forEach { Log.d("Chart", it.name) }
        adapter.setData(chartsList)
    }




}