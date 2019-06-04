package com.strikelines.app.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.MotionEvent
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import com.strikelines.app.ImportHelperListener
import com.strikelines.app.OsmandHelper.OsmandHelperListener
import com.strikelines.app.R
import com.strikelines.app.StrikeLinesApplication
import com.strikelines.app.StrikeLinesApplication.Companion.DOWNLOAD_REQUEST_CODE
import com.strikelines.app.StrikeLinesApplication.Companion.IMPORT_REQUEST_CODE
import com.strikelines.app.ui.adapters.LockableViewPager
import com.strikelines.app.utils.AndroidUtils
import com.strikelines.app.utils.DownloadHelperListener
import com.strikelines.app.utils.PlatformUtil
import com.strikelines.app.utils.clearTitleForWrecks
import kotlinx.android.synthetic.main.activity_main.*
import java.lang.ref.WeakReference


class MainActivity : AppCompatActivity(), OsmandHelperListener, ImportHelperListener, DownloadHelperListener {

	private val log = PlatformUtil.getLog(MainActivity::class.java)

	private val app get() = application as StrikeLinesApplication
	private val osmandHelper get() = app.osmandHelper
	private val importHelper get() = app.importHelper
	private val downloadHelper get() = app.downloadHelper

	private val listeners = mutableListOf<WeakReference<OsmandHelperListener>>()

	private var mapsTabFragment: MapsTabFragment? = null
	private var purchasesTabFragment: PurchasesTabFragment? = null

	private lateinit var bottomNav: BottomNavigationView
	private lateinit var progressBar: ProgressBar
	private var snackView: View? = null

	var regionList: MutableSet<String> = mutableSetOf()
	var regionToFilter: String = ""
	var isActivityVisible = false

	private var importUri: Uri? = null

	override fun fileCopyStarted(fileName: String?) {
		if (isActivityVisible) {
			showProgressBar(true)
		}
	}

	override fun fileCopyProgressUpdated(fileName: String?, progress: Int) {
		if (isActivityVisible) {
			showProgressBar(true)
			progressBar.progress = progress
		}
	}

	override fun fileCopyFinished(fileName: String?, success: Boolean) {
		if (isActivityVisible) {
			showProgressBar(false)
			if (success) {
				updateMapsList()
				showSnackBar(getString(R.string.importFileSuccess).format(fileName), action = 2)
			} else {
				showSnackBar(getString(R.string.importFileError).format(fileName), action = 2)
			}
		}
	}

	override fun onDownloadStarted(title: String, path: String) {

	}

	override fun onDownloadProgressUpdate(title: String, path: String, progress: Int) {
		if (isActivityVisible) {
			showProgressBar(true)
			progressBar.progress = progress
		}
	}

	override fun onDownloadCompleted(title: String, path: String, isSuccess: Boolean) {
		if (isActivityVisible) {
			showProgressBar(false)
			val message = getString(if (isSuccess) R.string.download_success_msg else R.string.download_failed_msg).format(clearTitleForWrecks(title))
			val action = if (isSuccess) 3 else 2
			showSnackBar(message, findViewById(android.R.id.content), action = action, path = path)
		}
	}

	override fun onOsmandConnectionStateChanged(connected: Boolean) {
		listeners.forEach {
			it.get()?.onOsmandConnectionStateChanged(connected)
		}
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)
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
		fab.setOnClickListener {
			StrikeLinesApplication.shouldOpenOsmand = true
			if (osmandHelper.canOpenOsmand()) {
				osmandHelper.checkOsmandInitialization()
			} else {
				installOsmandDialog()
				app.showToastMessage(getString(R.string.osmandIsMissing))
			}
		}
		fab.setOnTouchListener { v, event ->
			when (event?.action) {
				MotionEvent.ACTION_UP -> {
					big_fab_icon.setColorFilter(
						ContextCompat.getColor(this@MainActivity, R.color.osmand_pressed_btn_bg),
						android.graphics.PorterDuff.Mode.MULTIPLY
					)
					big_fab_label.setTextColor(resources.getColor(R.color.osmand_pressed_btn_bg))
				}
				MotionEvent.ACTION_DOWN -> {
					big_fab_icon.setColorFilter(
						ContextCompat.getColor(this@MainActivity, R.color.osmand_pressed_btn_icon),
						android.graphics.PorterDuff.Mode.MULTIPLY
					)
					big_fab_label.setTextColor(resources.getColor(R.color.osmand_pressed_btn_text))
				}
			}
			v?.onTouchEvent(event) ?: true
		}
		progressBar = findViewById<ProgressBar>(R.id.horizontal_progress).apply {
			val bgColor = ContextCompat.getColor(this@MainActivity, R.color.osmand_pressed_btn_bg)
			val progressColor = ContextCompat.getColor(this@MainActivity, R.color.osmand_pressed_btn_icon)
			progressDrawable = AndroidUtils.createProgressDrawable(bgColor, progressColor)
			indeterminateDrawable.setColorFilter(progressColor, android.graphics.PorterDuff.Mode.SRC_IN)
		}
	}

	override fun onResume() {
		super.onResume()
		isActivityVisible = true
		osmandHelper.listeners.add(this)
		importHelper.listener = this
		downloadHelper.listener = this
		StrikeLinesApplication.listener = appListener
		if (osmandHelper.isOsmandBound() && !osmandHelper.isOsmandConnected()) {
			osmandHelper.connectOsmand()
		}
		showProgressBar(importHelper.isCopying() || downloadHelper.isDownloading())
		checkIntentForFileImport(intent)
	}

	override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
		if (requestCode == IMPORT_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
			if (data != null) {
				val uri = data.data
				if (uri != null) {
					processFileImport(uri)
				}
			}
		} else {
			super.onActivityResult(requestCode, resultCode, data)
		}
	}

	override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
		if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
			if (requestCode == DOWNLOAD_REQUEST_CODE) {
				val uri = importUri
				if (uri != null) {
					importHelper.importFile(uri)
				}
			} else if (requestCode == IMPORT_REQUEST_CODE) {
				selectFileForImport()
			}
		}
	}

	override fun onPause() {
		super.onPause()
		isActivityVisible = false
		osmandHelper.listeners.remove(this)
		importHelper.listener = null
		downloadHelper.listener = null
		StrikeLinesApplication.listener = null
	}

	override fun onDestroy() {
		super.onDestroy()
		StrikeLinesApplication.shouldOpenOsmand = false
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

	fun selectFileForImport() {
		if (osmandHelper.isOsmandAvailiable()) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
				if (AndroidUtils.hasPermissionToWriteExternalStorage(this)) {
					importFile()
				} else {
					requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), IMPORT_REQUEST_CODE)
				}
			} else {
				importFile()
			}
		} else {
			installOsmandDialog()
		}
	}

	private fun checkIntentForFileImport(intent: Intent?) {
		if (Intent.ACTION_VIEW == intent?.action) {
			if (intent.data != null) {
				val uri = intent.data
				intent.action = null
				setIntent(null)
				val scheme = uri?.scheme
				if (uri != null && scheme != null && ("file" == scheme || "content" == scheme)) {
					if (importHelper.isCopying()) {
						showToastMessage(getString(R.string.copy_file_in_progress_alert))
					} else {
						processFileImport(uri)
					}
				}
			}
		}
	}

	private fun showProgressBar(isVisible: Boolean) {
		val visibility = if (isVisible) View.VISIBLE else View.GONE
		if (progressBar.visibility != visibility) {
			progressBar.visibility = visibility
		}
	}

	private fun updateMapsList() {
		for (fragment in supportFragmentManager.fragments) {
			if (fragment is MapsTabFragment) {
				fragment.fetchListItems()
			}
		}
	}

	private fun importFile() {
		val intent = Intent().apply {
			action = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
				Intent.ACTION_OPEN_DOCUMENT
			} else {
				Intent.ACTION_GET_CONTENT
			}
			type = "*/*"
		}
		if (AndroidUtils.isIntentSafe(this, intent)) {
			startActivityForResult(intent, IMPORT_REQUEST_CODE)
		}
	}

	private fun processFileImport(uri: Uri) {
		if (osmandHelper.isOsmandAvailiable()) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
				if (AndroidUtils.hasPermissionToWriteExternalStorage(this)) {
					importHelper.importFile(uri)
				} else {
					importUri = uri
					requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), DOWNLOAD_REQUEST_CODE)
				}
			} else {
				importHelper.importFile(uri)
			}
		} else {
			installOsmandDialog()
		}
	}

	inner class ViewPagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {
		private val fragments = listOf<Fragment>(MapsTabFragment(), PurchasesTabFragment())
		override fun getItem(position: Int) = fragments[position]
		override fun getCount() = fragments.size
	}

	private fun showToastMessage(msg: String) {
		Toast.makeText(this@MainActivity, msg, Toast.LENGTH_LONG).show()
	}

	private fun showSnackBar(
		msg: String,
		parentLayout: View? = snackView,
		lengths: Int = Snackbar.LENGTH_LONG,
		action: Int,
		path: String = ""
	) {
		val snackbar = Snackbar.make(parentLayout!!, msg, lengths)
		when (action) {
			1 -> snackbar.setAction(getString(R.string.snack_update_btn)) { app.loadCharts() }
			2 -> snackbar.setAction(getString(R.string.snack_ok_btn)) { snackbar.dismiss() }
			3 -> snackbar.setAction(getString(R.string.open_action)) {
				if (path.isNotEmpty()) app.openFile(path)
			}
		}
		snackbar.show()
	}

	fun initChartsList() {
		if (StrikeLinesApplication.isDataReadyFlag) {
			chartsDataIsReady = true
			regionList.clear()
			regionList.add(resources.getString(R.string.all_regions))
			StrikeLinesApplication.chartsList.forEach { regionList.add(it.region) }
			notifyFragmentsOnDataChange()
		}
	}

	fun notifyFragmentsOnDataChange() {
		fragmentNotifier.forEach { (_, v) -> v?.onDataReady(true) }
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
			} else
				snackView?.let { showSnackBar(getString(R.string.snack_msg_update_failed), snackView!!, action = 1) }
		}
	}

	interface FragmentDataNotifier {
		fun onDataReady(status: Boolean)
	}

	@SuppressLint("InflateParams")
	private fun installOsmandDialog() {
		val appPackageName = "net.osmand"
		val builder = AlertDialog.Builder(this)
		val dialogLayout = layoutInflater.inflate(R.layout.dialog_download_osmand, null)
		builder.setPositiveButton("OK", null)
		builder.setView(dialogLayout)
		builder.setPositiveButton("INSTALL") { dialog, _ ->
			try {
				startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$appPackageName")))
			} catch (anfe: android.content.ActivityNotFoundException) {
				startActivity(
					Intent(
						Intent.ACTION_VIEW,
						Uri.parse("https://play.google.com/store/apps/details?id=$appPackageName")
					)
				)
			}
			dialog.dismiss()
		}
		builder.setNegativeButton("CANCEL") { dialog, _ -> dialog.dismiss() }
		builder.show()
	}

	companion object {
		const val OPEN_DOWNLOADS_TAB_KEY = "open_downloads_tab_key"

		private const val MAPS_TAB_POS = 0
		private const val DOWNLOADS_TAB_POS = 1

		val fragmentNotifier = mutableMapOf<Int, FragmentDataNotifier?>()
		var chartsDataIsReady = false
	}
}