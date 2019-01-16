package com.strikelines.app.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat.checkSelfPermission
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.strikelines.app.*
import com.strikelines.app.domain.models.Chart
import com.strikelines.app.ui.adapters.ShopAdapter
import com.strikelines.app.ui.adapters.ShopListener
import com.strikelines.app.utils.AndroidUtils
import com.strikelines.app.utils.DownloadFileAsync

abstract class PurchaseSqliteDbFilesFragment : Fragment() {

	companion object {
		const val TITLE = 0
		const val LIST_STATE_KEY = "recycle_state"
		private const val CHART_BUNDLE_KEY = "chart_details"
	}

	protected val chartsList = mutableListOf<Chart>()

	lateinit var recycleView: RecyclerView
	var adapter: ShopAdapter? = null
	private var downloadUrl: String? = null


	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
		retainInstance = true
		val view = inflater.inflate(R.layout.recycler_list_fragment, container, false)
		recycleView = view.findViewById(R.id.recycler_view)
		adapter = ShopAdapter(listener)
		recycleView.layoutManager = LinearLayoutManager(activity, LinearLayout.VERTICAL, false)
		recycleView.adapter = this.adapter
		return view
	}

	override fun onResume() {
		super.onResume()
		getData()
	}

	private val listener: ShopListener? = object : ShopListener {
		override fun onDetailsClicked(item: Chart) = openDetailsScreen(item.name)

		override fun onDownloadClicked(item: Chart) = openUrl(item)
	}

	fun openDetailsScreen(chartsName: String) {
		startActivity(Intent(activity, DetailedPurchaseChartScreen::class.java).apply {
			putExtra(CHART_BUNDLE_KEY, chartsName)
		})
	}

	fun openUrl(item: Chart) {
		if (item.downloadurl.isEmpty()) startActivity(AndroidUtils.getIntentForBrowser(item.weburl))
		else {
			downloadUrl = item.downloadurl
			downloadFreeChart(downloadUrl!!)
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

			if (checkSelfPermission(activity!!, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
				== PackageManager.PERMISSION_GRANTED
			) {
				DownloadFileAsync(downloadUrl).execute()
			} else {
				requestPermissions(
					arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
					StrikeLinesApplication.DOWNLOAD_REQUEST_CODE
				)
			}
		}

	}

	override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
		if (requestCode == StrikeLinesApplication.DOWNLOAD_REQUEST_CODE && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
			downloadUrl.let { DownloadFileAsync(downloadUrl!!).execute() }
		}
	}

}