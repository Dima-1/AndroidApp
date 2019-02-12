package com.strikelines.app

import android.net.Uri
import android.os.AsyncTask
import android.provider.OpenableColumns
import com.strikelines.app.utils.PlatformUtil
import net.osmand.aidl.OsmandAidlConstants.*
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
		const val COPY_SUCCESSFUL = 1
		const val COPY_FAILED = -1
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
			listener?.fileCopyFinished(fileName, COPY_FAILED)
			false
		}
	}

	override fun onPostExecute(result: Boolean?) {
		super.onPostExecute(result)
		isCopyInProgress = false
		if (result != null && result) {
			listener?.fileCopyFinished(fileName, COPY_SUCCESSFUL)
		} else {
			listener?.fileCopyFinished(fileName, COPY_FAILED)
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
		val data = ByteArray(chunkSize)
		val startTime = System.currentTimeMillis()

		try {
			val bis: DataInputStream = if (uri.scheme == "content") {
				DataInputStream(app.contentResolver.openInputStream(uri))
			} else {
				DataInputStream(FileInputStream(
					app.applicationContext.contentResolver.openFileDescriptor(uri, "r")?.fileDescriptor))
			}

			var tryCount = 0
			var response = COPY_FILE_START_FLAG
			var actionStatus = COPY_FILE_START
			var read = 0

			while (read != -1 && !isError) {
				when(response) {
					COPY_FILE_START -> {
						read = bis.read()
					}
					COPY_FILE_OK_RESPONSE -> {
						read = bis.read(data)
						actionStatus = if (read == -1) {
							COPY_FILE_FINISH_FLAG
						} else {
							COPY_FILE_IN_PROGRESS_FLAG
						}
					}
					COPY_FILE_WRITE_LOCK_ERROR -> {
						if(tryCount < 3) {
							log.error(app.applicationContext.getString(R.string.copy_file_write_lock_error_msg))
							tryCount++
							Thread.sleep(COPY_FILE_VALID_PAUSE)
						} else {
							isError = true
						}
					}
					COPY_FILE_PARAMS_ERROR -> {
						log.error (app.applicationContext.getString(R.string.file_params_error))
						isError = true
					}
					COPY_FILE_PART_SIZE_LIMIT_ERROR -> {
						log.error (app.applicationContext.getString(R.string.part_size_limit_error))
						isError = true
					}
					COPY_FILE_IO_ERROR -> {
						log.error (app.applicationContext.getString(R.string.io_error))
						isError = true
					}
					COPY_FILE_UNSUPPORTED_FILE_TYPE_ERROR -> {
						log.error(app.applicationContext.getString(R.string.unsupported_type_error))
						isError = true
					}
				}
				response = app.osmandHelper.copyFile(CopyFileParams(fileName, data, startTime, actionStatus))

			}
			bis.close()

			return !isError

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
			name = if (columnIndex != -1) {
				returnCursor.getString(columnIndex)
			} else {
				contentUri.lastPathSegment
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
	fun fileCopyFinished (fileName: String?, result: Int)
}