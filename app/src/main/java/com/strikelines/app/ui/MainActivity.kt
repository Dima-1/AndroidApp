package com.strikelines.app.ui

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import com.strikelines.app.OsmandCustomizationConstants
import com.strikelines.app.OsmandCustomizationConstants.PLUGIN_RASTER_MAPS
import com.strikelines.app.OsmandHelper
import com.strikelines.app.OsmandHelper.Companion.APP_MODE_AIRCRAFT
import com.strikelines.app.OsmandHelper.Companion.APP_MODE_BICYCLE
import com.strikelines.app.OsmandHelper.Companion.APP_MODE_BOAT
import com.strikelines.app.OsmandHelper.Companion.APP_MODE_BUS
import com.strikelines.app.OsmandHelper.Companion.APP_MODE_CAR
import com.strikelines.app.OsmandHelper.Companion.APP_MODE_PEDESTRIAN
import com.strikelines.app.OsmandHelper.Companion.APP_MODE_TRAIN
import com.strikelines.app.OsmandHelper.Companion.METRIC_CONST_NAUTICAL_MILES
import com.strikelines.app.OsmandHelper.Companion.SPEED_CONST_NAUTICALMILES_PER_HOUR
import com.strikelines.app.OsmandHelper.OsmandHelperListener
import com.strikelines.app.R
import com.strikelines.app.StrikeLinesApplication
import com.strikelines.app.ui.adapters.LockableViewPager
import com.strikelines.app.utils.AndroidUtils
import kotlinx.android.synthetic.main.activity_main.*
import java.lang.ref.WeakReference

class MainActivity : AppCompatActivity(), OsmandHelperListener {

    private val app get() = application as StrikeLinesApplication
    private val osmandHelper get() = app.osmandHelper
    private val listeners = mutableListOf<WeakReference<OsmandHelperListener>>()
    private var mapsTabFragment: MapsTabFragment? = null
    private var purchasesTabFragment: PurchasesTabFragment? = null

    private lateinit var bottomNav: BottomNavigationView
    var regionList: MutableSet<String> = mutableSetOf()
    var regionToFilter: String = ""
    var snackView: View? = null

    val osmandHelperInitListener = object : OsmandHelper.OsmandAppInitCallback {
        override fun onOsmandInitialized() {
            setupOsmand()
        }
    }

    companion object {
        val fragmentNotifier = mutableMapOf<Int, FragmentDataNotifier?>()
        const val OPEN_DOWNLOADS_TAB_KEY = "open_downloads_tab_key"
        var isOsmandFABWasClicked = false
        var isOsmandConnected = false
        var chartsDataIsReady = false
        private const val MAPS_TAB_POS = 0
        private const val DOWNLOADS_TAB_POS = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        osmandHelper.onOsmandInitCallback = osmandHelperInitListener
        initChartsList()

        snackView = findViewById(android.R.id.content)
        val viewPager = findViewById<LockableViewPager>(R.id.view_pager).apply {
            swipeLocked = true
            offscreenPageLimit = 2
            adapter = ViewPagerAdapter(supportFragmentManager)
        }

        bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation).apply {
            setOnNavigationItemSelectedListener {
                var pos = -1
                when (it.itemId) {
                    R.id.action_maps -> pos = MAPS_TAB_POS
                    R.id.action_downloads -> pos = DOWNLOADS_TAB_POS
                }
                if (pos != -1 && pos != viewPager.currentItem) {
                    viewPager.currentItem = pos
                    return@setOnNavigationItemSelectedListener true
                }
                false
            }
        }
        fab.setOnClickListener { view ->
            isOsmandFABWasClicked = true
            osmandHelper.openOsmand {
                // TODO: open OsmAnd on Google Play Store
                Toast.makeText(view.context, "OsmAnd Missing", Toast.LENGTH_SHORT).show()
            }
        }
        fab.setOnTouchListener(
            object : View.OnTouchListener {
                override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                    when (event?.action) {
                        MotionEvent.ACTION_UP -> {
                            big_fab_icon.setColorFilter(
                                ContextCompat.getColor(
                                    this@MainActivity,
                                    R.color.osmand_pressed_btn_bg
                                ),
                                android.graphics.PorterDuff.Mode.MULTIPLY
                            )
                            big_fab_label.setTextColor(
                                resources.getColor(R.color.osmand_pressed_btn_bg))
                        }
                        MotionEvent.ACTION_DOWN -> {
                            big_fab_icon.setColorFilter(
                                ContextCompat.getColor(
                                    this@MainActivity,
                                    R.color.osmand_pressed_btn_icon
                                ),
                                android.graphics.PorterDuff.Mode.MULTIPLY
                            )
                            big_fab_label.setTextColor(
                                resources.getColor(R.color.osmand_pressed_btn_text))
                        }

                    }
                    return v?.onTouchEvent(event) ?: true
                }
            }
        )

        if (osmandHelper.isOsmandBound() && !osmandHelper.isOsmandConnected()) {
            osmandHelper.connectOsmand()
        }
    }

    override fun onResume() {
        super.onResume()
        osmandHelper.listener = this
        StrikeLinesApplication.listener = appListener
        osmandHelper.onOsmandInitCallback = osmandHelperInitListener
    }

    override fun onRestart() {
        super.onRestart()
        setupOsmand()
    }

    override fun onPause() {
        super.onPause()
        if (!isOsmandFABWasClicked) {
            osmandHelper.restoreOsmand()
        }
        osmandHelper.onOsmandInitCallback = null
        osmandHelper.listener = null
        StrikeLinesApplication.listener = null
    }

    override fun onDestroy() {
        super.onDestroy()
        app.cleanupResources()
    }

    override fun onAttachFragment(fragment: Fragment?) {
        if (fragment is OsmandHelperListener) {
            listeners.add(WeakReference(fragment))
        }
        if (fragment is MapsTabFragment) {
            mapsTabFragment = fragment
        } else if (fragment is PurchasesTabFragment) {
            purchasesTabFragment = fragment
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra(OPEN_DOWNLOADS_TAB_KEY, false)) {
            AndroidUtils.dismissAllDialogs(supportFragmentManager)
            bottomNav.selectedItemId = R.id.action_maps
        }
    }

    override fun onOsmandConnectionStateChanged(connected: Boolean) {
        if (connected) {
            osmandHelper.registerForOsmandInitialization()
            isOsmandConnected = true
            if (isOsmandConnected && chartsDataIsReady) loading_indicator.visibility = View.GONE
        }
        listeners.forEach {
            it.get()?.onOsmandConnectionStateChanged(connected)
        }
    }

    private fun setupOsmand() {
        val logoUri = AndroidUtils.resourceToUri(
            this@MainActivity, R.drawable.img_strikelines_nav_drawer_logo
        )

        val exceptDefault = listOf(
            APP_MODE_CAR,
            APP_MODE_PEDESTRIAN,
            APP_MODE_BICYCLE,
            APP_MODE_BOAT,
            APP_MODE_AIRCRAFT,
            APP_MODE_BUS,
            APP_MODE_TRAIN
        )
        val exceptPedestrianAndDefault = listOf(
            APP_MODE_CAR,
            APP_MODE_BICYCLE,
            APP_MODE_BOAT,
            APP_MODE_AIRCRAFT,
            APP_MODE_BUS,
            APP_MODE_TRAIN
        )
        val exceptAirBoatDefault = listOf(APP_MODE_CAR, APP_MODE_BICYCLE, APP_MODE_PEDESTRIAN)
        val pedestrian = listOf(APP_MODE_PEDESTRIAN)
        val pedestrianBicycle = listOf(APP_MODE_PEDESTRIAN, APP_MODE_BICYCLE)
        val all = null
        val none = emptyList<String>()

        osmandHelper.apply {

            setNavDrawerLogoWithParams(logoUri, packageName, "strike_lines_app://main_activity")
            setNavDrawerFooterParams(packageName, "strike_lines_app://main_activity", resources.getString(R.string.app_name))
            setNavDrawerItems(
                packageName,
                listOf(getString(R.string.aidl_menu_item_download_charts)),
                listOf("strike_lines_app://main_activity"),
                listOf("ic_type_archive"),
                listOf(-1)
            )

            setDisabledPatterns(
                listOf(
                    OsmandCustomizationConstants.DRAWER_DASHBOARD_ID,
                    OsmandCustomizationConstants.DRAWER_MY_PLACES_ID,
                    OsmandCustomizationConstants.DRAWER_SEARCH_ID,
                    OsmandCustomizationConstants.DRAWER_DIRECTIONS_ID,
                    OsmandCustomizationConstants.DRAWER_CONFIGURE_SCREEN_ID,
                    OsmandCustomizationConstants.DRAWER_OSMAND_LIVE_ID,
                    OsmandCustomizationConstants.DRAWER_TRAVEL_GUIDES_ID,
                    OsmandCustomizationConstants.DRAWER_PLUGINS_ID,
                    OsmandCustomizationConstants.DRAWER_SETTINGS_ID,
                    OsmandCustomizationConstants.DRAWER_HELP_ID,
                    OsmandCustomizationConstants.DRAWER_BUILDS_ID,
                    OsmandCustomizationConstants.DRAWER_DIVIDER_ID,
                    OsmandCustomizationConstants.DRAWER_DOWNLOAD_MAPS_ID,
                    OsmandCustomizationConstants.MAP_CONTEXT_MENU_ACTIONS,
                    OsmandCustomizationConstants.CONFIGURE_MAP_ITEM_ID_SCHEME
                )
            )

            setEnabledIds(
                listOf(
                    OsmandCustomizationConstants.MAP_CONTEXT_MENU_MEASURE_DISTANCE,
                    OsmandCustomizationConstants.GPX_FILES_ID,
                    OsmandCustomizationConstants.MAP_SOURCE_ID,
                    OsmandCustomizationConstants.OVERLAY_MAP,
                    OsmandCustomizationConstants.UNDERLAY_MAP,
                    OsmandCustomizationConstants.CONTOUR_LINES
                )
            )

            setDisabledIds(
                listOf(
                    OsmandCustomizationConstants.ROUTE_PLANNING_HUD_ID,
                    OsmandCustomizationConstants.QUICK_SEARCH_HUD_ID
                )
            )

            changePluginState(PLUGIN_RASTER_MAPS, 1)

            // left
            regWidgetVisibility("next_turn", exceptPedestrianAndDefault)
            regWidgetVisibility("next_turn_small", pedestrian)
            regWidgetVisibility("next_next_turn", exceptPedestrianAndDefault)
            regWidgetAvailability("next_turn", exceptDefault)
            regWidgetAvailability("next_turn_small", exceptDefault)
            regWidgetAvailability("next_next_turn", exceptDefault)

            // right
            regWidgetVisibility("intermediate_distance", all)
            regWidgetVisibility("distance", all)
            regWidgetVisibility("time", all)
            regWidgetVisibility("intermediate_time", all)
            regWidgetVisibility("speed", exceptPedestrianAndDefault)
            regWidgetVisibility("max_speed", listOf(APP_MODE_CAR))
            regWidgetVisibility("altitude", pedestrianBicycle)
            regWidgetVisibility("gps_info", listOf(APP_MODE_BOAT))
            regWidgetAvailability("intermediate_distance", all)
            regWidgetAvailability("distance", all)
            regWidgetAvailability("time", all)
            regWidgetAvailability("intermediate_time", all)
            regWidgetAvailability("map_marker_1st", none)
            regWidgetAvailability("map_marker_2nd", none)
            regWidgetVisibility("bearing", listOf(APP_MODE_BOAT))

            // top
            regWidgetVisibility("config", none)
            regWidgetVisibility("layers", none)
            regWidgetVisibility("compass", none)
            regWidgetVisibility("street_name", exceptAirBoatDefault)
            regWidgetVisibility("back_to_location", all)
            regWidgetVisibility("monitoring_services", none)
            regWidgetVisibility("bgService", none)

            val bundle = Bundle()
            bundle.apply {
                putString("available_application_modes", "$APP_MODE_BOAT,")
                putString("application_mode", APP_MODE_BOAT)
                putString("default_application_mode_string", APP_MODE_BOAT)
                putBoolean("driving_region_automatic", false)
                putString("default_metric_system", METRIC_CONST_NAUTICAL_MILES)
                putString("default_speed_system", SPEED_CONST_NAUTICALMILES_PER_HOUR)
            }

            customizeOsmandSettings("strikelines", bundle)
        }
    }

    inner class ViewPagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {
        private val fragments = listOf<Fragment>(MapsTabFragment(), PurchasesTabFragment())
        override fun getItem(position: Int) = fragments[position]
        override fun getCount() = fragments.size
    }

    fun showToastMessage(msg: String) {
        Toast.makeText(this@MainActivity, msg, Toast.LENGTH_LONG).show()
    }

    fun showSnackBar(
        msg: String,
        parentLayout: View,
        lengths: Int = Snackbar.LENGTH_LONG,
        action: Int
    ) {
        val snackbar = Snackbar.make(parentLayout, msg, lengths)
        when (action) {
            1 -> snackbar.setAction(getString(R.string.snack_update_btn)) { app.loadCharts() }
            2 -> snackbar.setAction(getString(R.string.snack_ok_btn)) { snackbar.dismiss() }
        }
        snackbar.show()
    }

    fun initChartsList() {
        if (StrikeLinesApplication.isDataReadyFlag) {
            chartsDataIsReady = true
            if (chartsDataIsReady && isOsmandConnected) loading_indicator.visibility = View.GONE
            regionList.clear()
            regionList.add(resources.getString(R.string.all_regions))
            StrikeLinesApplication.chartsList.forEach { regionList.add(it.region) }
            notifyFragmentsOnDataChange()
        }
    }

    fun notifyFragmentsOnDataChange() {
        fragmentNotifier.forEach { (k, v) -> v?.onDataReady(true) }
    }

    private var appListener = object : StrikeLinesApplication.AppListener {
        override fun isDataReady(status: Boolean) {
            if (status) {
                initChartsList()
                snackView?.let {
                    showSnackBar(
                        getString(R.string.snack_msg_update_successful),
                        snackView!!,
                        action = 2
                    )
                }
            } else snackView?.let {
                showSnackBar(
                    getString(R.string.snack_msg_update_failed),
                    snackView!!,
                    action = 1
                )
            }
        }
    }

    interface FragmentDataNotifier {
        fun onDataReady(status: Boolean)
    }
}
