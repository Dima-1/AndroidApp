package com.strikelines.app.ui

import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.text.Html
import android.util.Log
import android.view.View
import com.strikelines.app.R
import com.strikelines.app.StrikeLinesApplication
import com.strikelines.app.StrikeLinesApplication.Companion.DOWNLOAD_REQUEST_CODE
import com.strikelines.app.domain.GlideApp
import com.strikelines.app.domain.models.Chart
import com.strikelines.app.utils.AndroidUtils
import com.strikelines.app.utils.DownloadHelperListener
import com.strikelines.app.utils.clearGarbadge
import com.strikelines.app.utils.clearTitleForWrecks
import kotlinx.android.synthetic.main.detailed_chart_screen.*


class DetailedPurchaseChartScreen : AppCompatActivity(), DownloadHelperListener {

	companion object {
		private const val CHART_BUNDLE_KEY = "chart_details"
	}

	private val app = StrikeLinesApplication.getApp()
	private val downloadHelper get() = app?.downloadHelper

	private var chart: Chart? = null
	private var isActivityVisible = false

	override fun onCreate(savedInstanceState: Bundle?) {

		super.onCreate(savedInstanceState)
		setContentView(R.layout.detailed_chart_screen)
		if (intent.extras != null) {
			getIntentContents(intent.extras)
		} else {
			this@DetailedPurchaseChartScreen.finish()
		}
		if (chart != null) {
			populateView(chart!!)
		}
	}

	override fun onResume() {
		super.onResume()
		downloadHelper?.listener = this
		isActivityVisible = true
	}

	override fun onPause() {
		super.onPause()
		downloadHelper?.listener = null
		isActivityVisible = false
	}

	override fun onDownloadStarted(title: String, path: String) {

	}

	override fun onDownloadProgressUpdate(title: String, path: String, progress: Int) {

	}

	override fun onDownloadCompleted(title: String, path: String, isSuccess: Boolean) {
		if (isActivityVisible) {
			if (isSuccess) {
				showSnackBar(resources.getString(R.string.download_success_msg).format(clearTitleForWrecks(title)),
					findViewById(android.R.id.content), action = 3, path = path)
			} else {
				showSnackBar(resources.getString(R.string.download_failed_msg).format(clearTitleForWrecks(title)),
					findViewById(android.R.id.content), action = 2)
			}
		}
	}

	override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
		if (requestCode == DOWNLOAD_REQUEST_CODE && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
			chart?.let {
				downloadHelper?.downloadFile(it.downloadurl, it.name)
			}
		}
	}

	private fun populateView(chart: Chart) {
		get_chart_btn.typeface = Typeface.createFromAsset(assets, "fonts/Overpass-Bold.ttf")
		details_back_btn.setOnClickListener { onBackPressed() }
		details_title.text = clearTitleForWrecks(chart.name)
		details_chart_price.text = if (chart.price.toInt() > 0) "$${chart.price}" else "FREE"
		details_yellow_data_tv.text = chart.region
		details_description.text =
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
					Html.fromHtml(chart.description.clearGarbadge(), 0)
				else Html.fromHtml(chart.description.clearGarbadge())
		GlideApp.with(details_image)
			.load(chart.imageurl).placeholder(R.drawable.img_placeholder).into(details_image)
		if (chart.downloadurl.isEmpty()) {
			get_chart_btn.setOnClickListener { startActivity(AndroidUtils.getIntentForBrowser(chart.weburl)) }
			get_chart_btn.text = "${getString(R.string.shop_details_btn_tag_price_of_chart)}${chart.price}"
		} else {
			get_chart_btn.setOnClickListener { downloadFreeChart(chart.downloadurl) }
			get_chart_btn.text = getString(R.string.shop_details_btn_tag_free_chart)
		}
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			details_scroll_view.setOnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
				if (oldScrollY < scrollY && details_back_btn.alpha != 0f) {
					fadeOut(details_back_btn, 300)
				} else if (oldScrollY > scrollY && details_back_btn.alpha != 1f) {
					fadeIn(details_back_btn, 10)
				}
			}
		}
	}

	private fun fadeOut(view: View, duration: Long = 500) {
		view.apply {
			animate()
				.alpha(0f)
				.setDuration(duration)
				.setListener(null)
		}
	}

	private fun fadeIn(view: View, duration: Long = 500) {
		view.apply {
			animate()
				.alpha(1f)
				.setDuration(duration)
				.setListener(null)
		}
	}

	private fun downloadFreeChart(downloadUrl: String) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			if (AndroidUtils.hasPermissionToWriteExternalStorage(this)) {
				downloadHelper?.downloadFile(downloadUrl, chart!!.name)
			} else {
				requestPermissions(arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE), DOWNLOAD_REQUEST_CODE)
			}
		} else {
			downloadHelper?.downloadFile(downloadUrl, chart!!.name)
		}
	}

	private fun getIntentContents(bundle: Bundle?) {
		try {
			chart = StrikeLinesApplication.chartsList.first {
				it.name == bundle?.getString(CHART_BUNDLE_KEY)
			}
		} catch (e: Exception) {
			Log.w(e.message, e)
		}
	}

	private fun showSnackBar(
		msg: String,
		parentLayout: View,
		lengths: Int = Snackbar.LENGTH_LONG,
		action: Int,
		path: String = ""
	) {
		val snackbar = Snackbar.make(parentLayout, msg, lengths)
		when (action) {
			2 -> snackbar.setAction(getString(R.string.snack_ok_btn)) { snackbar.dismiss() }
			3 -> snackbar.setAction(getString(R.string.open_action)) {
				if (path.isNotEmpty()) app?.openFile(path)
			}
		}
		snackbar.show()
	}
}