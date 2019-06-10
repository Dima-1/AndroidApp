package com.strikelines.app.ui

import android.os.Bundle
import android.support.annotation.NonNull
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.strikelines.app.OsmandHelper
import com.strikelines.app.OsmandHelper.OsmandHelperListener
import com.strikelines.app.R
import com.strikelines.app.StrikeLinesApplication
import com.strikelines.app.ui.adapters.LocalItemsViewAdapter
import com.strikelines.app.ui.adapters.LocalItemsViewAdapter.*
import com.strikelines.app.utils.AndroidUtils
import com.strikelines.app.utils.UiUtils
import net.osmand.aidl.gpx.AGpxFile
import net.osmand.aidl.tiles.ASqliteDbFile

class MapsTabFragment : Fragment(), OsmandHelperListener, OnCheckedListener {

	private val app get() = activity?.application as? StrikeLinesApplication
	private val osmandHelper get() = app?.osmandHelper

	private lateinit var listView: RecyclerView
	private val viewAdapter = LocalItemsViewAdapter()

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
		val view = inflater.inflate(R.layout.maps_tab_fragment, container, false)
		viewAdapter.listener = this
		listView = view.findViewById<RecyclerView>(R.id.recycler_view)
		listView.apply {
			layoutManager = LinearLayoutManager(context)
			adapter = this@MapsTabFragment.viewAdapter
		}
		view.findViewById<TextView>(R.id.import_button).apply {
			val mainActivity = activity as? MainActivity
			if (mainActivity != null) {
				val icon = UiUtils(mainActivity).getIcon(R.drawable.ic_action_import, R.color.accent_color)
				setCompoundDrawablesWithIntrinsicBounds(null, null, icon, null)
				setOnClickListener {
					mainActivity.selectFileForImport()
				}
			}
		}

		return view
	}

	override fun onResume() {
		super.onResume()
		val osmandHelper = osmandHelper
		if (osmandHelper != null && osmandHelper.isOsmandBound()) {
			fetchListItems()
		}
	}

	fun fetchListItems() {
		val listItems = mutableListOf<ListItem>()

		val gpxFiles = osmandHelper?.importedGpxFiles
		if (gpxFiles != null && gpxFiles.isNotEmpty()) {

			val gpxHeaderItem = HeaderItem()
			gpxHeaderItem.title = getString(R.string.gpx_charts)
			gpxHeaderItem.description = getString(R.string.select_multiple)
			listItems.add(gpxHeaderItem)
			for (gpx in gpxFiles) {
				if (!gpx.fileName.contains("rec")) {
					val gpxItem = LocalGpxItem()
					gpxItem.imageId = R.drawable.img_gpx_chart
					gpxItem.title = OsmandHelper.getGpxFileHumanReadableName(gpx.fileName)
					gpxItem.description =
							"${AndroidUtils.formatSize(gpx.fileSize)} • ${gpx.details?.wptPoints.toString()} waypoints"
					gpxItem.selected = gpx.isActive
					gpxItem.multiselection = true
					gpxItem.data = gpx
					listItems.add(gpxItem)
				}
			}
		}

		val sqliteDbFiles = osmandHelper?.sqliteDbFiles
		if (sqliteDbFiles != null && sqliteDbFiles.isNotEmpty()) {
			val charts3dHeader = HeaderItem()
			charts3dHeader.title = getString(R.string.charts_3d)
			charts3dHeader.description = getString(R.string.select_single)
			listItems.add(charts3dHeader)
			for (sqliteDbFile in sqliteDbFiles) {
				val chartItem = Local3DChartItem()
				chartItem.imageId = R.drawable.img_3d_chart
				chartItem.title =
						OsmandHelper.getSqliteDbFileHumanReadableName(sqliteDbFile.fileName)
				chartItem.description = AndroidUtils.formatSize(sqliteDbFile.fileSize)
				chartItem.selected = sqliteDbFile.isActive
				chartItem.multiselection = false
				chartItem.data = sqliteDbFile
				listItems.add(chartItem)
			}
		}

		listView.post {
			viewAdapter.items = listItems
		}
	}

	override fun onOsmandConnectionStateChanged(connected: Boolean) {
		fetchListItems()
	}

	override fun onCheckedChanged(@NonNull listItem: ListItem, isChecked: Boolean): Boolean {
		var itemVisibilityChanged: Boolean? = null
		when (listItem) {
			is LocalGpxItem -> {
				val gpx = listItem.data as? AGpxFile
				if (gpx != null) {
					itemVisibilityChanged = if (isChecked) {
						osmandHelper?.showGpx(gpx.fileName)
					} else {
						osmandHelper?.hideGpx(gpx.fileName)
					}
				}
			}
			is Local3DChartItem -> {
				val sqliteDbFile = listItem.data as? ASqliteDbFile
				if (sqliteDbFile != null) {
					itemVisibilityChanged = if (isChecked) {
						osmandHelper?.showSqliteDbFile(sqliteDbFile.fileName)
					} else {
						osmandHelper?.hideSqliteDbFile(sqliteDbFile.fileName)
					}
				}
			}
		}
		return itemVisibilityChanged ?: false
	}
}