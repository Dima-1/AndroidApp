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
			osmandHelper.apply {
				setNavDrawerLogo(
					AndroidUtils.resourceToUri(
						view.context, R.drawable.img_strikelines_nav_drawer_logo
					)
				)
				openOsmand {
					// TODO: open OsmAnd on Google Play Store
					Toast.makeText(view.context, "OsmAnd Missing", Toast.LENGTH_SHORT).show()
				}
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
}
