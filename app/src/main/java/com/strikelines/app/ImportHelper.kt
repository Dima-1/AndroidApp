package com.strikelines.app

import android.net.Uri
import android.os.AsyncTask
import android.provider.OpenableColumns
import com.strikelines.app.utils.PlatformUtil
import net.osmand.aidl.tiles.CopyFileParams
import java.io.*

class ImportHelper(
	val app: StrikeLinesApplication,

	val uri: Uri
) : AsyncTask<Void, Void, Boolean>() {

	private val log = PlatformUtil.getLog(ImportHelper::class.java)
	private val chunkSize = 1024 * 256
	private val fileName by lazy {
		getNameFromContentUri(uri)
	}
	var isCopyInProgress = false
	var listener: ImportHelperListener? = null


	companion object {
		const val SQLITE_EXT = ".sqlitedb"
		const val CHARTS_EXT = ".charts"

		const val FILE_PARAMS_ERROR = -1001
		const val TOO_BIG_PARTS = -1002
		const val DOUBLE_COPY = -1003
		const val IO_ERROR = -1004
		const val UNKNOWN_FILE_TYPE = -1005
	}

	override fun onPreExecute() {
		super.onPreExecute()
		listener?.fileCopyStarted(fileName)
	}

	override fun doInBackground(vararg params: Void): Boolean? {
		return if (fileName!=null) {
			isCopyInProgress = true
			handleFileImport(uri)

		} else {
			listener?.fileCopyFinished(fileName, -1)
			false
		}
	}

	override fun onPostExecute(result: Boolean?) {
		super.onPostExecute(result)
		isCopyInProgress = false
		if (result != null && result) {
			listener?.fileCopyFinished(fileName, 1)
		} else {
			listener?.fileCopyFinished(fileName, -1)
		}
	}

	private fun handleFileImport(uri: Uri): Boolean {
		if (fileName!!.endsWith(SQLITE_EXT)) {
			return fileImportImpl(uri, fileName!!)
		} else if (fileName!!.endsWith(CHARTS_EXT)) {
			val newFilename = fileName!!.removePrefix(CHARTS_EXT) + SQLITE_EXT
			return fileImportImpl(uri, newFilename)
		}
		return false
	}

	private fun fileImportImpl(uri: Uri, fileName: String): Boolean {
		var isError = false
		var responseId = 0
		val data = ByteArray(chunkSize)
		var id = 0
		try {
			val bis: DataInputStream = if (uri.scheme == "content") {
				DataInputStream(app.contentResolver.openInputStream(uri))
			} else {
				DataInputStream(FileInputStream(
					app.applicationContext.contentResolver.openFileDescriptor(uri, "r")?.fileDescriptor))
			}

			var read = 0
			while (read != -1) {
				var isCopyComplete = false


				if (responseId >= 0) {
					read = bis.read(data)
					if (read == -1) {
						isCopyComplete = true
					}
				} else if (responseId < 0) {
					when(responseId) {
						FILE_PARAMS_ERROR -> listener?.fileCopyError("Check file parameters", fileName)
						TOO_BIG_PARTS -> {log.error("Array should not be large")}
						DOUBLE_COPY -> {listener?.fileCopyError("Wait until copying of file with same name complete", fileName)}
						IO_ERROR -> {listener?.fileCopyError("I/O error", fileName)}
						UNKNOWN_FILE_TYPE -> {listener?.fileCopyError("Unknown/Unsupported File Type", fileName)}
					}
				}

				if(responseId==0) {
					id = responseId
				}


				responseId = app.osmandHelper.copyFile(CopyFileParams(fileName, data, id, isCopyComplete))
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
	fun fileCopyStarted(fileName: String?)
	fun fileCopyError(msg: String, fileName: String?)
	fun fileCopyFinished (fileName: String?, result: Int)
}