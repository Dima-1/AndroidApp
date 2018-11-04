package com.strikelines.app

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
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
		osmandHelper.apply {
			setNavDrawerLogo(logoUri)
			setDisabledPatterns(listOf(OsmandCustomizationConstants.DRAWER_ITEM_ID_SCHEME))
			setEnabledIds(
				listOf(
					OsmandCustomizationConstants.DRAWER_MAP_MARKERS_ID,
					OsmandCustomizationConstants.DRAWER_MEASURE_DISTANCE_ID,
					OsmandCustomizationConstants.DRAWER_CONFIGURE_MAP_ID,
					OsmandCustomizationConstants.DRAWER_DOWNLOAD_MAPS_ID
				)
			)
			setDisabledIds(listOf(OsmandCustomizationConstants.ROUTE_PLANNING_HUD_ID))
			setDisabledIds(listOf(OsmandCustomizationConstants.QUICK_SEARCH_HUD_ID))
		}
	}
}
