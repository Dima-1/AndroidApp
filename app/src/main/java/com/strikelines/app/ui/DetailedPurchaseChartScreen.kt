package com.strikelines.app.ui

import android.content.pm.PackageManager
import android.graphics.Rect
import android.graphics.Typeface
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.Html
import android.util.Log
import com.strikelines.app.R
import com.strikelines.app.utils.clearTitleForWrecks
import com.strikelines.app.domain.GlideApp
import com.strikelines.app.domain.models.Chart
import kotlinx.android.synthetic.main.detailed_chart_screen.*
import java.lang.Exception
import android.view.WindowManager
import android.os.Build
import android.support.annotation.RequiresApi
import android.support.v4.app.ActivityCompat
import com.strikelines.app.StrikeLinesApplication
import com.strikelines.app.StrikeLinesApplication.Companion.DOWNLOAD_REQUEST_CODE
import com.strikelines.app.utils.AndroidUtils
import com.strikelines.app.utils.DownloadFileAsync
import com.strikelines.app.utils.clearGarbadge


class DetailedPurchaseChartScreen : AppCompatActivity() {

    companion object {
        private const val CHART_BUNDLE_KEY = "chart_details"
    }


    var chart: Chart? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.detailed_chart_screen)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            changeSystemBarVisibility(true)
//            val w = window
//            w.setFlags(
//                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
//                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
//            )
        }


        if (intent.extras != null) {
            getIntentContents(intent.extras)
        } else {
            this@DetailedPurchaseChartScreen.finish()
        }
        if (chart != null) {
            populateView(chart!!)
        }

    }


    private fun populateView(chart: Chart) {
        //val font = Typeface.createFromAsset(assets, "fonts/Overpass-Bold.ttf")
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
                Log.d("Scroll Change", "sY: $scrollY, olY: $oldScrollY")

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {

                    val bounds = Rect()
                    details_image.getHitRect(bounds)

                    val scrollBounds = Rect(scrollX, scrollY,
                            scrollX+details_scroll_view.width,
                            scrollY+details_scroll_view.height)

                    if(!Rect.intersects(scrollBounds, bounds)) {
                        changeSystemBarVisibility(false)
                    } else {
                        changeSystemBarVisibility(true)
                    }
                }
            }
        }
    }

    private fun downloadFreeChart(downloadUrl: String) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED
            ) {
                DownloadFileAsync(downloadUrl).execute()
            } else {
                requestPermissions(arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE), DOWNLOAD_REQUEST_CODE)
            }
        }

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == DOWNLOAD_REQUEST_CODE && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            chart?.let { DownloadFileAsync(chart!!.downloadurl).execute() }
        }
    }

    private fun getIntentContents(bundle: Bundle?) {
        try {
            chart = StrikeLinesApplication.chartsList.first { it.name == bundle?.getString(CHART_BUNDLE_KEY) }
        } catch (e: Exception) {
            Log.w(e.message, e)
        }
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    fun changeSystemBarVisibility(isVisible: Boolean) {
        val w = window
        when (isVisible) {
            true -> {
                w.setFlags(
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                )
            }
            false -> {
                w.clearFlags(
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
            }
        }


    }

}

