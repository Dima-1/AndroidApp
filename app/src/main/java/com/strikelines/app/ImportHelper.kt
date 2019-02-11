package com.strikelines.app

import android.net.Uri
import android.os.AsyncTask
import com.strikelines.app.ui.MainActivity
import com.strikelines.app.utils.PlatformUtil
import net.osmand.aidl.tiles.CopyFileParams
import java.io.*
import java.lang.ref.WeakReference
import java.net.URI

class ImportHelper(
	val app: StrikeLinesApplication,
	val listener: ImportHelperListener,
	val uri: Uri,
	val fileName: String
) : AsyncTask<Void, Void, Boolean>() {

	private val log = PlatformUtil.getLog(ImportHelper::class.java)
	private val chunkSize = 1024 * 256


	companion object {
		const val SQLITE_EXT = ".sqlitedb"
		const val CHARTS_EXT = ".charts"
	}

	override fun onPreExecute() {
		super.onPreExecute()
		listener.showProgressBar(true)
	}

	override fun doInBackground(vararg params: Void): Boolean? {
		return handleFileImport(uri, fileName)
	}

	override fun onPostExecute(result: Boolean?) {
		super.onPostExecute(result)

		if (result != null && result) {
			listener.showSnackBar(StrikeLinesApplication.getApp()!!.applicationContext
					.getString(R.string.importFileSuccess).format(fileName), 2)
		} else {
			listener.showSnackBar(StrikeLinesApplication.getApp()!!.applicationContext
					.getString(R.string.importFileError).format(fileName), 2)
		}

		listener.showProgressBar(false)
		listener.updateMapList()
	}

	private fun handleFileImport(uri: Uri, fileName: String): Boolean {
		if (fileName.endsWith(SQLITE_EXT)) {
			return fileImportImpl(uri, fileName)
		} else if (fileName.endsWith(CHARTS_EXT)) {
			val newFilename = fileName.removePrefix(CHARTS_EXT) + SQLITE_EXT
			return fileImportImpl(uri, newFilename)
		}
		return false
	}

	private fun fileImportImpl(uri: Uri, fileName: String): Boolean {
		var receivedDataSize = 0L
		var isError = false
		var isReceived = true
		var data = ByteArray(chunkSize)
		val copyStartTime = System.currentTimeMillis()
		try {
			val bis: DataInputStream
			if (uri.scheme == "content") {
				bis = DataInputStream(app.contentResolver.openInputStream(uri))
			} else {
				bis = DataInputStream(FileInputStream(
					app.applicationContext.contentResolver.openFileDescriptor(uri, "r")?.fileDescriptor))
			}

			var read = 0
			while (read != -1) {
				var errorCount = 0
				var isCopyComplete = false
				if (isReceived) {
					receivedDataSize += read
					read = bis.read(data)
					if (read == -1) isCopyComplete = true
				} else {
					errorCount++
					if (errorCount > 10) {
						isError = true
						break
					}
				}
				isReceived =
					app.osmandHelper.copyFile(CopyFileParams(fileName, data, copyStartTime, isCopyComplete))
			}
			bis.close()

			if (isError) {
				return false
			}

			return true
		} catch (ioe: IOException) {
			log.error(ioe.message, ioe)
			return false
		}
	}
}

interface ImportHelperListener {
	fun showProgressBar(visibility: Boolean)
	fun updateMapList()
	fun showSnackBar(message: String, action: Int)
}