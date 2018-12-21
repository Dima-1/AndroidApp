package com.strikelines.app.ui

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
import com.strikelines.app.StrikeLinesApplication
import com.strikelines.app.utils.AndroidUtils
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
            val w = window
            w.setFlags(
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            )
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
        details_back_btn.setOnClickListener { onBackPressed() }
        details_title.text = clearTitleForWrecks(chart.name)
        details_chart_price.text = if (chart.price.toInt() > 0) "$${chart.price}" else "FREE"
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
    }

    private fun downloadFreeChart(downloadurl: String) {

    }

    private fun getIntentContents(bundle: Bundle?) {
        try {
            chart = StrikeLinesApplication.chartsList.first { it.name == bundle?.getString(CHART_BUNDLE_KEY)}
        } catch (e: Exception) {
            Log.w(e.message, e)
        }
    }

}

