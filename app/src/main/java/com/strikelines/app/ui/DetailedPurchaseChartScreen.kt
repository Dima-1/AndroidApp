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
import com.strikelines.app.utils.AndroidUtils


class DetailedPurchaseChartScreen : AppCompatActivity() {

    companion object {
        private const val CHART_BUNDLE_KEY = "chart_details"
//        fun newInstance(chart: Chart) = DetailedPurchaseChartScreen().apply {
//            this.chart = chart
//        }
    }

    var chart: Chart? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.detailed_chart_screen)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            val w = window // in Activity's onCreate() for instance
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
        details_description.text = Html.fromHtml(chart.description)
        GlideApp.with(details_image).load(chart.imageurl).into(details_image)
        get_chart_btn.setOnClickListener { startActivity(AndroidUtils.getIntentForBrowser(chart.imageurl))}

    }

    private fun getIntentContents(bundle: Bundle?) {
        try {
            chart = bundle?.getParcelable(CHART_BUNDLE_KEY)
            Log.d("Details", chart.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

