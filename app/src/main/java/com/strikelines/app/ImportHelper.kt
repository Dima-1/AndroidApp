package com.strikelines.app

import android.net.Uri
import android.os.AsyncTask
import android.provider.OpenableColumns
import com.strikelines.app.utils.PlatformUtil
import net.osmand.aidl.tiles.CopyFileParams
import java.io.*

class ImportHelper(
	val app: StrikeLinesApplication,
	val listener: ImportHelperListener,
	val uri: Uri
) : AsyncTask<Void, Void, Boolean>() {

	private val log = PlatformUtil.getLog(ImportHelper::class.java)
	private val chunkSize = 1024 * 256
	private val fileName by lazy {
		getNameFromContentUri(uri)!!
	}



	companion object {
		const val SQLITE_EXT = ".sqlitedb"
		const val CHARTS_EXT = ".charts"
	}

	override fun onPreExecute() {
		super.onPreExecute()
		listener.fileCopying(true)
	}

	override fun doInBackground(vararg params: Void): Boolean? {
		return handleFileImport(uri)
	}

	override fun onPostExecute(result: Boolean?) {
		super.onPostExecute(result)

		if (result != null && result) {
			listener.copyFinished(fileName, 1)
		} else {
			listener.copyFinished(fileName, -1)
		}

		listener.fileCopying(false)

	}

	private fun handleFileImport(uri: Uri): Boolean {
		if (fileName.endsWith(SQLITE_EXT)) {
			return fileImportImpl(uri, fileName)
		} else if (fileName.endsWith(CHARTS_EXT)) {
			val newFilename = fileName.removePrefix(CHARTS_EXT) + SQLITE_EXT
			return fileImportImpl(uri, newFilename)
		}
		return false
	}

	private fun fileImportImpl(uri: Uri, fileName: String): Boolean {
		var isError = false
		var isReceived = true
		val data = ByteArray(chunkSize)
		val copyStartTime = System.currentTimeMillis()
		try {
			val bis: DataInputStream = if (uri.scheme == "content") {
				DataInputStream(app.contentResolver.openInputStream(uri))
			} else {
				DataInputStream(FileInputStream(
					app.applicationContext.contentResolver.openFileDescriptor(uri, "r")?.fileDescriptor))
			}

			var read = 0
			while (read != -1) {
				var errorCount = 0
				var isCopyComplete = false
				if (isReceived) {
					read = bis.read(data)
					if (read == -1) {
						isCopyComplete = true
					}
				} else {
					errorCount++
					if (errorCount > 10) {
						isError = true
						break
					}
				}
				isReceived = app.osmandHelper.copyFile(CopyFileParams(fileName, data, copyStartTime, isCopyComplete))
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


	private fun getNameFromContentUri(contentUri: Uri): String? {
		val name: String?
		val returnCursor = app.contentResolver.query(contentUri, null, null, null, null)
		if (returnCursor != null && returnCursor.moveToFirst()) {
			val columnIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
			if (columnIndex != -1) {
				name = returnCursor.getString(columnIndex)
			} else {
				name = contentUri.lastPathSegment
			}
		} else {
			name = null
		}
		if (returnCursor != null && !returnCursor.isClosed) {
			returnCursor.close()
		}
		return name
	}
}

interface ImportHelperListener {
	fun fileCopying(isCopying: Boolean)
	fun copyFinished( fileName: String, result: Int)
}