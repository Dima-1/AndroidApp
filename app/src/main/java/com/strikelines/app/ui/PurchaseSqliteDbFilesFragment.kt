package com.strikelines.app.ui

import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.google.gson.GsonBuilder
import com.strikelines.app.*
import com.strikelines.app.domain.RepoCallback
import com.strikelines.app.domain.Repository
import com.strikelines.app.domain.models.Chart
import com.strikelines.app.domain.models.Charts
import com.strikelines.app.ui.shopadapter.ShopAdapter
import com.strikelines.app.ui.shopadapter.ShopListener
import com.strikelines.app.utils.AndroidUtils
import com.strikelines.app.utils.GetRequestAsync
import com.strikelines.app.utils.OnRequestResultListener
import kotlinx.android.synthetic.main.recycler_list_fragment.*

abstract class PurchaseSqliteDbFilesFragment : Fragment() {

    companion object {
        const val TITLE = 0
        const val LIST_STATE_KEY = "recycle_state"
        private const val CHART_BUNDLE_KEY = "chart_details"
    }

    protected var repo:Repository? = null

    protected val gson by lazy { GsonBuilder().setLenient().create() }
    protected val url = "https://strikelines.com/api/charts/?key=A3dgmiOM1ul@IG1N=*@q"
    protected val chartsList = mutableListOf<Chart>()

    lateinit var recycleView: RecyclerView
    lateinit var adapter: ShopAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repo = Repository.getInstance(StrikeLinesApplication.applicationContext(), repoCallback)
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?): View? {

        retainInstance = true
        val view = inflater.inflate(R.layout.recycler_list_fragment, container, false)
        recycleView = view.findViewById(R.id.recycler_view)
        adapter = ShopAdapter(listener)
        recycleView.layoutManager = LinearLayoutManager(activity, LinearLayout.VERTICAL, false)
        recycleView.adapter = this.adapter
        return view
    }


    val listener: ShopListener? = object : ShopListener {
        override fun onDetailsClicked(item: Chart) {
            openDetailsScreen(item)
        }

        override fun onDownloadClicked(url: String) {
            openBrowser(url)
        }
    }

    fun openDetailsScreen(item: Chart) {
        startActivity(Intent(activity, DetailedPurchaseChartScreen::class.java).apply {
            putExtra(CHART_BUNDLE_KEY, item)
        })
    }

    fun openBrowser(url: String) {
        startActivity(AndroidUtils.getIntentForBrowser(url))
    }


    private val repoCallback =  object:RepoCallback{
        override fun onLoadingComplete(status: String) {
            Log.w("inFragment", status)
        }

        override fun isResourcesLoading(status: Boolean) {
            Log.w("inFragment loading", status.toString())
        }
    }

    abstract fun onRequestResult(result: List<Chart>)

    abstract fun sortResults(results:List<Chart>):List<Chart>

    protected fun parseJson(response: String?): List<Chart> {
        return if (response != null) {
            val charts: Charts = gson.fromJson(response, Charts::class.java)
            charts.charts
        } else emptyList()

    }


}