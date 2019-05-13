package com.strikelines.app.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.strikelines.app.R
import com.strikelines.app.StrikeLinesApplication
import com.strikelines.app.StrikeLinesApplication.Companion.DOWNLOAD_REQUEST_CODE
import com.strikelines.app.domain.models.Chart
import com.strikelines.app.ui.adapters.ShopAdapter
import com.strikelines.app.ui.adapters.ShopListener
import com.strikelines.app.utils.AndroidUtils

abstract class PurchaseSqliteDbFilesFragment : Fragment() {

	companion object {
		const val TITLE = 0
		const val LIST_STATE_KEY = "recycle_state"
		private const val CHART_BUNDLE_KEY = "chart_details"
	}

	private val app get() = StrikeLinesApplication.getApp()
	private val downloadHelper get() = app?.downloadHelper

	private val chartsList = mutableListOf<Chart>()

	private var adapter: ShopAdapter? = null
	private var downloadUrl: String = ""
	private var downloadTitle: String = ""

	private val listener: ShopListener? = object : ShopListener {
		override fun onDetailsClicked(item: Chart) = openDetailsScreen(item.name)
		override fun onDownloadClicked(item: Chart) = openUrl(item)
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
		retainInstance = true
		val view = inflater.inflate(R.layout.recycler_list_fragment, container, false)

		adapter = ShopAdapter(listener)
		view.findViewById<RecyclerView>(R.id.recycler_view).apply {
			layoutManager = LinearLayoutManager(activity, LinearLayout.VERTICAL, false)
			adapter = this@PurchaseSqliteDbFilesFragment.adapter
		}

		return view
	}

	override fun onResume() {
		super.onResume()
		getData()
	}

	fun openDetailsScreen(chartsName: String) {
		startActivity(Intent(activity, DetailedPurchaseChartScreen::class.java).apply {
			putExtra(CHART_BUNDLE_KEY, chartsName)
		})
	}

	fun openUrl(item: Chart) {
		if (item.downloadurl.isEmpty()) startActivity(AndroidUtils.getIntentForBrowser(item.weburl))
		else {
			Log.d("ListFragment:", "Download clicked")
			downloadUrl = item.downloadurl
			downloadTitle = item.name
			downloadFreeChart(downloadUrl)
		}
	}

	fun getData() {
		if (MainActivity.chartsDataIsReady) {
			onRequestResult(StrikeLinesApplication.chartsList)
		} else {
			onRequestResult(emptyList())
		}
	}

	private fun onRequestResult(result: List<Chart>) {
		chartsList.clear()
		chartsList.addAll(sortResults(result))
		adapter?.setData(chartsList)
	}

	abstract fun sortResults(results: List<Chart>): List<Chart>

	private fun downloadFreeChart(downloadUrl: String) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			if (AndroidUtils.hasPermissionToWriteExternalStorage(app!!)) {
				downloadHelper?.downloadFile(downloadUrl, downloadTitle)
			} else {
				requestPermissions(arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE), DOWNLOAD_REQUEST_CODE)
			}
		} else {
			downloadHelper?.downloadFile(downloadUrl, downloadTitle)
		}
	}

	override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
		if (requestCode == DOWNLOAD_REQUEST_CODE && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
			downloadHelper?.downloadFile(downloadUrl, downloadTitle)
		}
	}
}