package com.strikelines.app

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.IBinder
import android.os.RemoteException
import net.osmand.aidl.IOsmAndAidlInterface

class OsmandHelper(private val app: Application) {

	private val osmandPackages = listOf("net.osmand.plus", "net.osmand", "net.osmand.dev")

	private var mIOsmAndAidlInterface: IOsmAndAidlInterface? = null

	private var initialized = false
	private var bound = false

	var selectedOsmandPackage = ""
		private set

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
			e.printStackTrace()
		}
	}

	fun openOsmand(onOsmandMissingAction: (() -> Unit)?) {
		val intent = app.packageManager.getLaunchIntentForPackage(selectedOsmandPackage)
		if (intent != null) {
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
				e.printStackTrace()
			}
		}
	}

	fun setEnabledIds(ids: List<String>) {
		if (mIOsmAndAidlInterface != null) {
			try {
				mIOsmAndAidlInterface!!.setEnabledIds(ids)
			} catch (e: RemoteException) {
				e.printStackTrace()
			}
		}
	}

	fun setDisabledIds(ids: List<String>) {
		if (mIOsmAndAidlInterface != null) {
			try {
				mIOsmAndAidlInterface!!.setDisabledIds(ids)
			} catch (e: RemoteException) {
				e.printStackTrace()
			}
		}
	}

	fun setEnabledPatterns(patterns: List<String>) {
		if (mIOsmAndAidlInterface != null) {
			try {
				mIOsmAndAidlInterface!!.setEnabledPatterns(patterns)
			} catch (e: RemoteException) {
				e.printStackTrace()
			}
		}
	}

	fun setDisabledPatterns(patterns: List<String>) {
		if (mIOsmAndAidlInterface != null) {
			try {
				mIOsmAndAidlInterface!!.setDisabledPatterns(patterns)
			} catch (e: RemoteException) {
				e.printStackTrace()
			}
		}
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
}