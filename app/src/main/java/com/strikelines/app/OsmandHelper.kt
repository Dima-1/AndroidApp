package com.strikelines.app

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.os.RemoteException
import android.text.TextUtils
import com.strikelines.app.utils.AndroidUtils
import com.strikelines.app.utils.PlatformUtil
import net.osmand.aidl.IOsmAndAidlCallback
import net.osmand.aidl.IOsmAndAidlInterface
import net.osmand.aidl.OsmandAidlConstants.COPY_FILE_IO_ERROR
import net.osmand.aidl.copyfile.CopyFileParams
import net.osmand.aidl.customization.CustomizationInfoParams
import net.osmand.aidl.customization.OsmandSettingsInfoParams
import net.osmand.aidl.customization.OsmandSettingsParams
import net.osmand.aidl.customization.SetWidgetsParams
import net.osmand.aidl.gpx.*
import net.osmand.aidl.navdrawer.NavDrawerFooterParams
import net.osmand.aidl.navdrawer.NavDrawerHeaderParams
import net.osmand.aidl.navdrawer.NavDrawerItem
import net.osmand.aidl.navdrawer.SetNavDrawerItemsParams
import net.osmand.aidl.plugins.PluginParams
import net.osmand.aidl.search.SearchResult
import net.osmand.aidl.tiles.ASqliteDbFile
import java.io.File
import java.util.*

class OsmandHelper(private val app: Application) {

	private val log = PlatformUtil.getLog(OsmandHelper::class.java)

	private val osmandPackages = listOf("net.osmand.plus", "net.osmand", "net.osmand.dev") //,

	private var mIOsmAndAidlInterface: IOsmAndAidlInterface? = null
	private var osmandCallbackId: Long = 0
	private var initialized = false
	private var bound = false

	private var selectedOsmandPackage = ""
	private var openOsmandRequested = false

	var listener: OsmandHelperListener? = null
	var onOsmandInitCallbacks: MutableList<OsmandAppInitCallback> = mutableListOf()

	interface OsmandHelperListener {
		fun onOsmandConnectionStateChanged(connected: Boolean)
	}

	interface OsmandAppInitCallback {
		fun onOsmandInitialized()
	}

	private val iOsmAndAidlCallback: IOsmAndAidlCallback.Stub? = object: IOsmAndAidlCallback.Stub() {
		@Throws(RemoteException::class)
		override fun onGpxBitmapCreated(bitmap: AGpxBitmap?) {}

		@Throws(RemoteException::class)
		override fun onSearchComplete(resultSet: List<SearchResult>) {}

		@Throws(RemoteException::class)
		override fun onUpdate() {}

		@Throws(RemoteException::class)
		override fun onAppInitialized() {
			log.debug("osmand initialized")
			for (callback in onOsmandInitCallbacks) {
				callback.onOsmandInitialized()
			}
			if (openOsmandRequested) {
				openOsmandRequested = false
				setupOsmand()
				openOsmand()
			}
		}
	}

	/**
	 * Class for interacting with the main interface of the service.
	 */
	private val mConnection = object : ServiceConnection {

		override fun onServiceConnected(name: ComponentName, service: IBinder) {
			// This is called when the connection with the service has been
			// established, giving us the service object we can use to
			// interact with the service.  We are communicating with our
			// service through an IDL interface, so get a client-side
			// representation of that from the raw service object.
			mIOsmAndAidlInterface = IOsmAndAidlInterface.Stub.asInterface(service)
			initialized = true
			log.debug("onServiceConnected")
			listener?.onOsmandConnectionStateChanged(true)
			registerForOsmandInitialization()
		}

		override fun onServiceDisconnected(name: ComponentName) {
			// This is called when the connection with the service has been
			// unexpectedly disconnected -- that is, its process crashed.
			log.debug("onServiceDisconnected")
			mIOsmAndAidlInterface = null
			listener?.onOsmandConnectionStateChanged(false)
		}
	}

	init {
		connectOsmand()
	}

	fun isOsmandBound() = initialized && bound

	fun isOsmandConnected() = mIOsmAndAidlInterface != null

	fun connectOsmand() {
		selectedOsmandPackage = getOsmandPackage()
		if (bindService(selectedOsmandPackage)) {
			bound = true
		} else {
			bound = false
			initialized = true
		}
	}

	fun cleanupResources() {
		try {
			if (mIOsmAndAidlInterface != null) {
				mIOsmAndAidlInterface = null
				app.unbindService(mConnection)
			}
		} catch (e: Throwable) {
			log.error(e)
		}
	}

	fun initAndOpenOsmand() {
		openOsmandRequested = true
		if (initialized && !isOsmandConnected()) {
			connectOsmand()
		} else {
			registerForOsmandInitialization()
		}
	}

	fun isOsmandOpening() = openOsmandRequested

	fun cancelOsmandOpening() {
		openOsmandRequested = false
	}

	fun canOpenOsmand() = app.packageManager.getLaunchIntentForPackage(selectedOsmandPackage) != null

	fun openOsmand() {
		val intent = app.packageManager.getLaunchIntentForPackage(selectedOsmandPackage)
		if (intent != null) {
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
			intent.putExtra(SHOW_OSMAND_WELCOME_SCREEN, false)
			app.startActivity(intent)
		}
	}

	fun setupOsmand() {
		val logoUri = AndroidUtils.resourceToUri(app, R.drawable.img_strikelines_nav_drawer_logo)
		val intent = "strike_lines_app://main_activity"
		val navDrawerHeaderParams = NavDrawerHeaderParams(logoUri.toString(), app.packageName, intent)
		val navDrawerFooterParams = NavDrawerFooterParams(app.packageName, intent, app.resources.getString(R.string.app_name))
		val navDrawerItemsParams = SetNavDrawerItemsParams(app.packageName, listOf(NavDrawerItem(app.getString(R.string.aidl_menu_item_download_charts), intent, "ic_type_archive", -1)))

		val featuresEnabledIds = getFeaturesEnabledIds()
		val featuresDisabledIds = getFeaturesDisabledIds()
		val featuresDisabledPatterns = getFeaturesDisabledPatterns()
		val visibilityWidgetsParams = getVisibilityWidgetsParams()
		val availabilityWidgetsParams = getAvailabilityWidgetsParams()
		val settingsParams = getCustomOsmandSettingsParams()
		val pluginParams = arrayListOf(PluginParams(OsmandCustomizationConstants.PLUGIN_RASTER_MAPS, 1))

		val customizationInfoParams = CustomizationInfoParams(
			settingsParams,
			navDrawerHeaderParams,
			navDrawerFooterParams,
			navDrawerItemsParams,
			visibilityWidgetsParams,
			availabilityWidgetsParams,
			pluginParams,
			featuresEnabledIds,
			featuresDisabledIds,
			null,
			featuresDisabledPatterns
		)
		setCustomization(customizationInfoParams)
	}

	fun isOsmandAvailable():Boolean =
		app.packageManager.getLaunchIntentForPackage(getOsmandPackage()) != null


	fun registerForOsmandInitialization(): Boolean {
		if (mIOsmAndAidlInterface != null) {
			try {
				return mIOsmAndAidlInterface!!.registerForOsmandInitListener(iOsmAndAidlCallback)
			} catch (e: RemoteException) {
				e.printStackTrace()
			}
		}
		return false
	}

	fun setNavDrawerLogo(uri: Uri) {
		if (mIOsmAndAidlInterface != null) {
			try {
				mIOsmAndAidlInterface!!.setNavDrawerLogo(uri.toString())
			} catch (e: RemoteException) {
				log.error(e)
			}
		}
	}

	fun setNavDrawerLogoWithParams(uri: Uri, packageName: String, intent: String) {
		if (mIOsmAndAidlInterface != null) {
			try {
				mIOsmAndAidlInterface!!.setNavDrawerLogoWithParams(
					NavDrawerHeaderParams(uri.toString(), packageName, intent))
			} catch (e: RemoteException) {
				log.error(e)
			}
		}
	}

	fun setNavDrawerFooterParams(packageName: String, intent: String, appName: String) {
		if (mIOsmAndAidlInterface != null) {
			try {
				mIOsmAndAidlInterface!!.setNavDrawerFooterWithParams(
					NavDrawerFooterParams(packageName, intent, appName))
			} catch (e: RemoteException) {
				log.error(e)
			}
		}
	}

    /**
     * Method for adding up to 3 items to the OsmAnd navigation drawer.
     *
     * @param appPackage - current application package.
     * @param names - list of names for items.
     * @param uris - list of uris for intents.
     * @param iconNames - list of icon names for items.
     * @param flags - list of flags for intents. Use -1 if no flags needed.
     */
    fun setNavDrawerItems(
        appPackage: String,
        names: List<String>,
        uris: List<String>,
        iconNames: List<String>,
        flags: List<Int>
    ): Boolean {
        if (mIOsmAndAidlInterface != null) {
            try {
                val items = ArrayList<NavDrawerItem>()
                for (i in names.indices) {
                    items.add(NavDrawerItem(names[i], uris[i], iconNames[i], flags[i]))
                }
                return mIOsmAndAidlInterface!!.setNavDrawerItems(SetNavDrawerItemsParams(appPackage, items))
            } catch (e: RemoteException) {
                e.printStackTrace()
            }

        }
        return false
    }

	fun setEnabledIds(ids: List<String>) {
		if (mIOsmAndAidlInterface != null) {
			try {
				mIOsmAndAidlInterface!!.setEnabledIds(ids)
			} catch (e: RemoteException) {
				log.error(e)
			}
		}
	}

	fun setDisabledIds(ids: List<String>) {
		if (mIOsmAndAidlInterface != null) {
			try {
				mIOsmAndAidlInterface!!.setDisabledIds(ids)
			} catch (e: RemoteException) {
				log.error(e)
			}
		}
	}

	fun setEnabledPatterns(patterns: List<String>) {
		if (mIOsmAndAidlInterface != null) {
			try {
				mIOsmAndAidlInterface!!.setEnabledPatterns(patterns)
			} catch (e: RemoteException) {
				log.error(e)
			}
		}
	}

	fun setDisabledPatterns(patterns: List<String>) {
		if (mIOsmAndAidlInterface != null) {
			try {
				mIOsmAndAidlInterface!!.setDisabledPatterns(patterns)
			} catch (e: RemoteException) {
				log.error(e)
			}
		}
	}

	fun regWidgetVisibility(widgetId: String, appModesKeys: List<String>?) {
		if (mIOsmAndAidlInterface != null) {
			try {
				mIOsmAndAidlInterface!!.regWidgetVisibility(SetWidgetsParams(widgetId, appModesKeys))
			} catch (e: RemoteException) {
				log.error(e)
			}
		}
	}

	fun regWidgetAvailability(widgetId: String, appModesKeys: List<String>?) {
		if (mIOsmAndAidlInterface != null) {
			try {
				mIOsmAndAidlInterface!!.regWidgetAvailability(SetWidgetsParams(widgetId, appModesKeys))
			} catch (e: RemoteException) {
				log.error(e)
			}
		}
	}

	fun customizeOsmandSettings(sharedPreferencesName: String, bundle: Bundle? = null) {
		if (mIOsmAndAidlInterface != null) {
			try {
				mIOsmAndAidlInterface!!.customizeOsmandSettings(OsmandSettingsParams(sharedPreferencesName, bundle))
			} catch (e: RemoteException) {
				log.error(e)
			}
		}
	}

	fun setCustomization(customizationInfoParams: CustomizationInfoParams) {
		if (mIOsmAndAidlInterface != null) {
			try {
				mIOsmAndAidlInterface!!.setCustomization(customizationInfoParams)
			} catch (e: RemoteException) {
				log.error(e)
			}
		}
	}

	/**
	 * Get list of active GPX files.
	 *
	 * @return list of active gpx files.
	 */
	val activeGpxFiles: List<ASelectedGpxFile>?
		get() {
			if (mIOsmAndAidlInterface != null) {
				try {
					val res = mutableListOf<ASelectedGpxFile>()
					if (mIOsmAndAidlInterface!!.getActiveGpx(res)) {
						return res
					}
				} catch (e: Throwable) {
					log.error(e)
				}

			}
			return null
		}

	/**
	 * Get list of all imported GPX files.
	 *
	 * @return list of imported gpx files.
	 */
	val importedGpxFiles: List<AGpxFile>?
		get() {
			if (mIOsmAndAidlInterface != null) {
				try {
					val files = mutableListOf<AGpxFile>()
					mIOsmAndAidlInterface!!.getImportedGpx(files)
					return files
				} catch (e: RemoteException) {
					log.error(e)
				}
			}
			return null
		}

	/**
	 * Import GPX file to OsmAnd.
	 * OsmAnd must have rights to access location. Not recommended.
	 *
	 * @param file      - File which represents GPX track.
	 * @param fileName  - Destination file name. May contain dirs.
	 * @param color     - color of gpx. Can be one of: "red", "orange", "lightblue", "blue", "purple",
	 * "translucent_red", "translucent_orange", "translucent_lightblue",
	 * "translucent_blue", "translucent_purple"
	 * @param show      - show track on the map after import
	 */
	fun importGpxFromFile(file: File, fileName: String, color: String, show: Boolean): Boolean {
		if (mIOsmAndAidlInterface != null) {
			try {
				return mIOsmAndAidlInterface!!.importGpx(ImportGpxParams(file, fileName, color, show))
			} catch (e: RemoteException) {
				log.error(e)
			}

		}
		return false
	}

	/**
	 * Import GPX file to OsmAnd.
	 *
	 * @param gpxUri    - URI created by FileProvider.
	 * @param fileName  - Destination file name. May contain dirs.
	 * @param color     - color of gpx. Can be one of: "", "red", "orange", "lightblue", "blue", "purple",
	 * "translucent_red", "translucent_orange", "translucent_lightblue",
	 * "translucent_blue", "translucent_purple"
	 * @param show      - show track on the map after import
	 */
	fun importGpxFromUri(gpxUri: Uri, fileName: String, color: String, show: Boolean): Boolean {
		if (mIOsmAndAidlInterface != null && !TextUtils.isEmpty(selectedOsmandPackage)) {
			try {
				app.grantUriPermission(selectedOsmandPackage, gpxUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
				return mIOsmAndAidlInterface!!.importGpx(ImportGpxParams(gpxUri, fileName, color, show))
			} catch (e: RemoteException) {
				log.error(e)
			}

		}
		return false
	}

	/**
	 * Import GPX file to OsmAnd.
	 *
	 * @param data      - Raw contents of GPX file. Sent as intent's extra string parameter.
	 * @param fileName  - Destination file name. May contain dirs.
	 * @param color     - color of gpx. Can be one of: "red", "orange", "lightblue", "blue", "purple",
	 * "translucent_red", "translucent_orange", "translucent_lightblue",
	 * "translucent_blue", "translucent_purple"
	 * @param show      - show track on the map after import
	 */
	fun importGpxFromData(data: String, fileName: String, color: String, show: Boolean): Boolean {
		if (mIOsmAndAidlInterface != null) {
			try {
				return mIOsmAndAidlInterface!!.importGpx(ImportGpxParams(data, fileName, color, show))
			} catch (e: RemoteException) {
				log.error(e)
			}

		}
		return false
	}

	/**
	 * Show GPX file on map.
	 *
	 * @param fileName - file name to show. Must be imported first.
	 */
	fun showGpx(fileName: String): Boolean {
		if (mIOsmAndAidlInterface != null) {
			try {
				return mIOsmAndAidlInterface!!.showGpx(ShowGpxParams(fileName))
			} catch (e: RemoteException) {
				log.error(e)
			}

		}
		return false
	}

	/**
	 * Hide GPX file.
	 *
	 * @param fileName - file name to hide.
	 */
	fun hideGpx(fileName: String): Boolean {
		if (mIOsmAndAidlInterface != null) {
			try {
				return mIOsmAndAidlInterface!!.hideGpx(HideGpxParams(fileName))
			} catch (e: RemoteException) {
				log.error(e)
			}

		}
		return false
	}

	/**
	 * Remove GPX file.
	 *
	 * @param fileName - file name to remove;
	 */
	fun removeGpx(fileName: String): Boolean {
		if (mIOsmAndAidlInterface != null) {
			try {
				return mIOsmAndAidlInterface!!.removeGpx(RemoveGpxParams(fileName))
			} catch (e: RemoteException) {
				log.error(e)
			}

		}
		return false
	}

	val sqliteDbFiles: List<ASqliteDbFile>?
		get() {
			if (mIOsmAndAidlInterface != null) {
				try {
					val files = mutableListOf<ASqliteDbFile>()
					mIOsmAndAidlInterface!!.getSqliteDbFiles(files)
					return files
				} catch (e: RemoteException) {
					log.error(e)
				}
			}
			return null
		}

	val activeSqliteDbFiles: List<ASqliteDbFile>?
		get() {
			if (mIOsmAndAidlInterface != null) {
				try {
					val files = mutableListOf<ASqliteDbFile>()
					mIOsmAndAidlInterface!!.getActiveSqliteDbFiles(files)
					return files
				} catch (e: RemoteException) {
					log.error(e)
				}
			}
			return null
		}

	fun showSqliteDbFile(fileName: String): Boolean {
		if (mIOsmAndAidlInterface != null) {
			try {
				return mIOsmAndAidlInterface!!.showSqliteDbFile(fileName)
			} catch (e: RemoteException) {
				log.error(e)
			}
		}
		return false
	}

	fun hideSqliteDbFile(fileName: String): Boolean {
		if (mIOsmAndAidlInterface != null) {
			try {
				return mIOsmAndAidlInterface!!.hideSqliteDbFile(fileName)
			} catch (e: RemoteException) {
				log.error(e)
			}
		}
		return false
	}

	fun restoreOsmand(): Boolean {
		if (mIOsmAndAidlInterface != null) {
			try {
				return mIOsmAndAidlInterface!!.restoreOsmand()
			} catch (e: RemoteException) {
				log.error(e)
			}
		}
		return false
	}

	fun copyFile(filePart: CopyFileParams): Int {
		if (mIOsmAndAidlInterface != null) {
			try {
				return mIOsmAndAidlInterface!!.copyFile(filePart)
			} catch (e: RemoteException) {
				log.error(e)
			}
		}
		return COPY_FILE_IO_ERROR
	}

	fun changePluginState(pluginId: String, newStatus: Int): Boolean {
		if (mIOsmAndAidlInterface != null) {
			try {
				return mIOsmAndAidlInterface!!.changePluginState(PluginParams(pluginId, newStatus))
			} catch (e: RemoteException) {
				log.error(e)
			}
		}
		return false
	}

	fun areOsmandSettingsCustomized(sharedPreferencesName: String): Boolean {
		if (mIOsmAndAidlInterface != null) {
			try {
				return mIOsmAndAidlInterface!!.areOsmandSettingsCustomized(OsmandSettingsInfoParams(sharedPreferencesName))
			} catch (e: RemoteException) {
				log.error(e)
			}
		}
		return false
	}

	private fun getFeaturesEnabledIds(): List<String> {
		return listOf(
			OsmandCustomizationConstants.MAP_CONTEXT_MENU_MEASURE_DISTANCE,
			OsmandCustomizationConstants.GPX_FILES_ID,
			OsmandCustomizationConstants.MAP_SOURCE_ID,
			OsmandCustomizationConstants.OVERLAY_MAP,
			OsmandCustomizationConstants.UNDERLAY_MAP,
			OsmandCustomizationConstants.CONTOUR_LINES
		)
	}

	private fun getFeaturesDisabledIds(): List<String> {
		return listOf(
			OsmandCustomizationConstants.ROUTE_PLANNING_HUD_ID,
			OsmandCustomizationConstants.QUICK_SEARCH_HUD_ID
		)
	}

	private fun getFeaturesDisabledPatterns(): List<String> {
		return listOf(
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
	}

	private fun getVisibilityWidgetsParams(): ArrayList<SetWidgetsParams> {
		return arrayListOf(
			SetWidgetsParams("next_turn", appModesExceptPedestrianAndDefault),
			SetWidgetsParams("next_turn_small", appModesPedestrian),
			SetWidgetsParams("next_next_turn", appModesExceptPedestrianAndDefault),
			SetWidgetsParams("intermediate_distance", appModesAll),
			SetWidgetsParams("distance", appModesAll),
			SetWidgetsParams("time", appModesAll),
			SetWidgetsParams("intermediate_time", appModesAll),
			SetWidgetsParams("speed", appModesExceptPedestrianAndDefault),
			SetWidgetsParams("max_speed", listOf(APP_MODE_CAR)),
			SetWidgetsParams("altitude", appModesPedestrianBicycle),
			SetWidgetsParams("gps_info", listOf(APP_MODE_BOAT)),
			SetWidgetsParams("bearing", listOf(APP_MODE_BOAT)),
			SetWidgetsParams("ruler", appModesAll),
			SetWidgetsParams("config", appModesNone),
			SetWidgetsParams("layers", appModesNone),
			SetWidgetsParams("compass", appModesNone),
			SetWidgetsParams("street_name", appModesExceptAirBoatDefault),
			SetWidgetsParams("back_to_location", appModesAll),
			SetWidgetsParams("monitoring_services", appModesNone),
			SetWidgetsParams("bgService", appModesNone)
		)
	}

	private fun getAvailabilityWidgetsParams(): ArrayList<SetWidgetsParams> {
		return arrayListOf(
			SetWidgetsParams("next_turn", appModesExceptDefault),
			SetWidgetsParams("next_turn_small", appModesExceptDefault),
			SetWidgetsParams("next_next_turn", appModesExceptDefault),
			SetWidgetsParams("intermediate_distance", appModesAll),
			SetWidgetsParams("distance", appModesAll),
			SetWidgetsParams("time", appModesAll),
			SetWidgetsParams("intermediate_time", appModesAll),
			SetWidgetsParams("map_marker_1st", appModesNone),
			SetWidgetsParams("map_marker_2nd", appModesNone)
		)
	}

	private fun getCustomOsmandSettingsParams(): OsmandSettingsParams {
		val areOsmandCustomized = areOsmandSettingsCustomized(OSMAND_SHARED_PREFERENCES_NAME)
		log.debug("areOsmandCustomized $areOsmandCustomized")
		val bundle = Bundle().apply {
			putString("available_application_modes", "$APP_MODE_BOAT,")
			putString("application_mode", APP_MODE_BOAT)
			putString("default_application_mode_string", APP_MODE_BOAT)
			putBoolean("driving_region_automatic", false)
			putBoolean("show_osmand_welcome_screen", false)
			putBoolean("show_coordinates_widget", true)
			putBoolean("show_compass_ruler", true)
			putString("map_info_controls", "ruler;")
			putString("default_metric_system", METRIC_CONST_NAUTICAL_MILES)
			putString("default_speed_system", SPEED_CONST_NAUTICALMILES_PER_HOUR)
			if (!areOsmandCustomized) {
				putBoolean("map_online_data", true)
			}
		}
		return OsmandSettingsParams(OSMAND_SHARED_PREFERENCES_NAME, bundle)
	}

	private fun bindService(packageName: String): Boolean {
		return if (mIOsmAndAidlInterface == null) {
			val intent = Intent("net.osmand.aidl.OsmandAidlService")
			intent.`package` = packageName
			app.bindService(intent, mConnection, Context.BIND_AUTO_CREATE)
		} else {
			true
		}
	}

	private fun getOsmandPackage() =
		osmandPackages.firstOrNull { AndroidUtils.isAppInstalled(app, it) } ?: ""

	companion object {
		const val APP_MODE_CAR = "car"
		const val APP_MODE_PEDESTRIAN = "pedestrian"
		const val APP_MODE_BICYCLE = "bicycle"
		const val APP_MODE_BOAT = "boat"
		const val APP_MODE_AIRCRAFT = "aircraft"
		const val APP_MODE_BUS = "bus"
		const val APP_MODE_TRAIN = "train"

		const val SPEED_CONST_KILOMETERS_PER_HOUR = "KILOMETERS_PER_HOUR"
		const val SPEED_CONST_MILES_PER_HOUR = "MILES_PER_HOUR"
		const val SPEED_CONST_METERS_PER_SECOND = "METERS_PER_SECOND"
		const val SPEED_CONST_MINUTES_PER_MILE = "MINUTES_PER_MILE"
		const val SPEED_CONST_MINUTES_PER_KILOMETER = "MINUTES_PER_KILOMETER"
		const val SPEED_CONST_NAUTICALMILES_PER_HOUR = "NAUTICALMILES_PER_HOUR"

		const val METRIC_CONST_KILOMETERS_AND_METERS = "KILOMETERS_AND_METERS"
		const val METRIC_CONST_MILES_AND_FEET = "MILES_AND_FEET"
		const val METRIC_CONST_MILES_AND_METERS = "MILES_AND_METERS"
		const val METRIC_CONST_MILES_AND_YARDS = "MILES_AND_YARDS"
		const val METRIC_CONST_NAUTICAL_MILES = "NAUTICAL_MILES"

		const val SHOW_OSMAND_WELCOME_SCREEN = "show_osmand_welcome_screen"

		const val OSMAND_SHARED_PREFERENCES_NAME = "strikelines"

		private val appModesAll = null
		private val appModesNone = emptyList<String>()
		private val appModesPedestrian = listOf(APP_MODE_PEDESTRIAN)
		private val appModesPedestrianBicycle = listOf(APP_MODE_PEDESTRIAN, APP_MODE_BICYCLE)
		private val appModesExceptAirBoatDefault = listOf(APP_MODE_CAR, APP_MODE_BICYCLE, APP_MODE_PEDESTRIAN)

		private val appModesExceptPedestrianAndDefault = listOf(
			APP_MODE_CAR,
			APP_MODE_BICYCLE,
			APP_MODE_BOAT,
			APP_MODE_AIRCRAFT,
			APP_MODE_BUS,
			APP_MODE_TRAIN
		)

		private val appModesExceptDefault = listOf(
			APP_MODE_CAR,
			APP_MODE_PEDESTRIAN,
			APP_MODE_BICYCLE,
			APP_MODE_BOAT,
			APP_MODE_AIRCRAFT,
			APP_MODE_BUS,
			APP_MODE_TRAIN
		)

		fun getSqliteDbFileHumanReadableName(fileName: String): String {
			return getFileHumanReadableName(fileName)
		}

		fun getGpxFileHumanReadableName(fileName: String): String {
			return getFileHumanReadableName(fileName)
		}

		private fun getFileHumanReadableName(fileName: String): String {
			var name = fileName
			val ext = name.lastIndexOf('.')
			if (ext != -1) {
				name = name.substring(0, ext)
			}
			return name.replace('_', ' ').capitalize().trim()
		}
	}
}