package com.strikelines.app.ui


import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.strikelines.app.R
import com.strikelines.app.StrikeLinesApplication
import com.strikelines.app.domain.Repository
import com.strikelines.app.domain.models.Chart


class Purchase3DChartsFragment : PurchaseSqliteDbFilesFragment() {




    companion object {
		const val TITLE = R.string.charts_3d
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

    override fun sortResults(results: List<Chart>): List<Chart> = results.filter { it.name.contains("3D ") }

//    override fun onSaveInstanceState(outState: Bundle) {
//        super.onSaveInstanceState(outState)
//        outState.putParcelable(LIST_STATE_KEY, recycleView.layoutManager?.onSaveInstanceState())
//    }



}