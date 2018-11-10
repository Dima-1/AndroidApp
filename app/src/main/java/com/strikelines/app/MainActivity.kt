package com.strikelines.app

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import com.strikelines.app.OsmandHelper.Companion.APP_MODE_AIRCRAFT
import com.strikelines.app.OsmandHelper.Companion.APP_MODE_BICYCLE
import com.strikelines.app.OsmandHelper.Companion.APP_MODE_BOAT
import com.strikelines.app.OsmandHelper.Companion.APP_MODE_BUS
import com.strikelines.app.OsmandHelper.Companion.APP_MODE_CAR
import com.strikelines.app.OsmandHelper.Companion.APP_MODE_PEDESTRIAN
import com.strikelines.app.OsmandHelper.Companion.APP_MODE_TRAIN
import com.strikelines.app.OsmandHelper.Companion.METRIC_CONST_NAUTICAL_MILES
import com.strikelines.app.OsmandHelper.Companion.SPEED_CONST_NAUTICALMILES_PER_HOUR
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

	private val app get() = application as StrikeLinesApplication
	private val osmandHelper get() = app.osmandHelper

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)

		fab.setOnClickListener { view ->
			setupOsmand()
			osmandHelper.openOsmand {
				// TODO: open OsmAnd on Google Play Store
				Toast.makeText(view.context, "OsmAnd Missing", Toast.LENGTH_SHORT).show()
			}
		}

		if (osmandHelper.isOsmandBound() && !osmandHelper.isOsmandConnected()) {
			osmandHelper.connectOsmand()
		}
	}

	override fun onDestroy() {
		super.onDestroy()
		app.cleanupResources()
	}

	private fun setupOsmand() {
		val logoUri = AndroidUtils.resourceToUri(
			this@MainActivity, R.drawable.img_strikelines_nav_drawer_logo
		)

		val exceptDefault = listOf(APP_MODE_CAR, APP_MODE_PEDESTRIAN, APP_MODE_BICYCLE, APP_MODE_BOAT, APP_MODE_AIRCRAFT, APP_MODE_BUS, APP_MODE_TRAIN)
		val exceptPedestrianAndDefault = listOf(APP_MODE_CAR, APP_MODE_BICYCLE, APP_MODE_BOAT, APP_MODE_AIRCRAFT, APP_MODE_BUS, APP_MODE_TRAIN)
		val exceptAirBoatDefault = listOf(APP_MODE_CAR, APP_MODE_BICYCLE, APP_MODE_PEDESTRIAN)
		val pedestrian = listOf(APP_MODE_PEDESTRIAN)
		val pedestrianBicycle = listOf(APP_MODE_PEDESTRIAN, APP_MODE_BICYCLE)

		val all = null
		val none = emptyList<String>()


		osmandHelper.apply {
			setNavDrawerLogo(logoUri)
			setDisabledPatterns(
				listOf(
					OsmandCustomizationConstants.DRAWER_ITEM_ID_SCHEME,
					OsmandCustomizationConstants.CONFIGURE_MAP_ITEM_ID_SCHEME
				)
			)
			setEnabledIds(
				listOf(
					OsmandCustomizationConstants.DRAWER_MAP_MARKERS_ID,
					OsmandCustomizationConstants.DRAWER_MEASURE_DISTANCE_ID,
					OsmandCustomizationConstants.DRAWER_CONFIGURE_MAP_ID,
					OsmandCustomizationConstants.DRAWER_DOWNLOAD_MAPS_ID,
					//OsmandCustomizationConstants.SHOW_CATEGORY_ID,
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
}
