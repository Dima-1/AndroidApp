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
import android.util.Log
import com.strikelines.app.utils.AndroidUtils
import com.strikelines.app.utils.PlatformUtil
import net.osmand.aidl.IOsmAndAidlInterface
import net.osmand.aidl.customization.OsmandSettingsParams
import net.osmand.aidl.customization.SetWidgetsParams
import net.osmand.aidl.gpx.*
import net.osmand.aidl.navdrawer.NavDrawerItem
import net.osmand.aidl.navdrawer.SetNavDrawerItemsParams
import net.osmand.aidl.tiles.ASqliteDbFile
import java.io.File
import java.util.ArrayList

class OsmandHelper(private val app: Application) {

	private val log = PlatformUtil.getLog(OsmandHelper::class.java)

	private val osmandPackages = listOf("net.osmand.plus", "net.osmand", "net.osmand.dev")

	private var mIOsmAndAidlInterface: IOsmAndAidlInterface? = null

	private var initialized = false
	private var bound = false

	private var selectedOsmandPackage = ""

	var listener: OsmandHelperListener? = null

	interface OsmandHelperListener {
		fun onOsmandConnectionStateChanged(connected: Boolean)
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
			listener?.onOsmandConnectionStateChanged(true)
		}

		override fun onServiceDisconnected(name: ComponentName) {
			// This is called when the connection with the service has been
			// unexpectedly disconnected -- that is, its process crashed.
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

	fun openOsmand(onOsmandMissingAction: (() -> Unit)?) {
		val intent = app.packageManager.getLaunchIntentForPackage(selectedOsmandPackage)
		if (intent != null) {
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
			app.startActivity(intent)
		} else {
			onOsmandMissingAction?.invoke()
		}
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

	fun setNavDrawerLogoWithParams(uri: Uri, packageName: String, intent:String) {
		if (mIOsmAndAidlInterface != null) {
			try {
				mIOsmAndAidlInterface!!.setNavDrawerLogoWithParams(uri.toString(), packageName, intent)
				//mIOsmAndAidlInterface!!.setNavDrawerLogo(uri.toString())
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
