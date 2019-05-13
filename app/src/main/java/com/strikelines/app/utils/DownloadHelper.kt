package com.strikelines.app.utils

import android.os.AsyncTask
import android.os.Environment
import com.strikelines.app.StrikeLinesApplication
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.util.*

class DownloadHelper(private val app: StrikeLinesApplication) : DownloadTaskListener {

	private val currentRunningTasks = Collections.synchronizedList(ArrayList<DownloadFileAsync>())

	var listener: DownloadHelperListener? = null

	fun downloadFile(downloadUrl: String, title: String) {
		DownloadFileAsync(downloadUrl, this, title).execute()
	}

	fun isDownloading(): Boolean = currentRunningTasks.isNotEmpty()

	override fun onDownloadStarted(title: String, path: String) {
		listener?.onDownloadStarted(title, path)
	}

	override fun onDownloadProgressUpdate(title: String, path: String, progress: Int) {
		listener?.onDownloadProgressUpdate(title, path, progress)
	}

	override fun onDownloadCompleted(title: String, path: String, isSuccess: Boolean) {
		listener?.onDownloadCompleted(title, path, isSuccess)
	}

	private inner class DownloadFileAsync(
		private val downloadUrl: String,
		private val downloadTaskListener: DownloadTaskListener,
		private val title: String = ""
	) : AsyncTask<Void, Int, Boolean>() {

		private val log = PlatformUtil.getLog(DownloadFileAsync::class.java)

		private val path = "${Environment.getExternalStorageDirectory().absolutePath}/strikelines/"
		private val fileName = downloadUrl.substringBeforeLast('/').substringAfterLast('/') +
				".${downloadUrl.substringAfterLast('.')}"

		override fun onPreExecute() {
			currentRunningTasks.add(this)
			super.onPreExecute()
			downloadTaskListener.onDownloadStarted(fileName, path)
		}

		override fun onProgressUpdate(vararg values: Int?) {
			val progress = values.firstOrNull()
			if (progress != null) {
				downloadTaskListener.onDownloadProgressUpdate(fileName, path, progress)
			}
		}

		override fun doInBackground(vararg params: Void?): Boolean {
			var res = false
			try {
				val dir = File(path)
				if (!dir.exists()) dir.mkdir()
				val file = File(dir, fileName)

				val urlcon = URL(downloadUrl).openConnection()
				urlcon.readTimeout = 60000
				urlcon.connectTimeout = 60000
				urlcon.connect()
				val inputStream = BufferedInputStream(urlcon.getInputStream(), 4096)
				val fileSize = urlcon.contentLength
				var readBytes = 0L
				val chunkSize = fileSize / 100
				var progressCounter = 0

				val outputStream = FileOutputStream(file)
				val data = ByteArray(1024)
				var total: Long = 0
				var count = inputStream.read(data)

				while (count != -1) {
					readBytes += count
					if (readBytes >= chunkSize) {
						progressCounter++
						readBytes -= chunkSize
						publishProgress(progressCounter)
					}
					total += count.toLong()
					outputStream.write(data, 0, count)
					count = inputStream.read(data)
				}

				outputStream.flush()
				outputStream.close()
				inputStream.close()
				res = true
			} catch (e: Exception) {
				log.error(e)
			}
			return res
		}

		override fun onPostExecute(result: Boolean?) {
			if (result != null && result) {
				downloadTaskListener.onDownloadCompleted(title, path + fileName, true)
			} else {
				downloadTaskListener.onDownloadCompleted(title, "", false)
			}
			currentRunningTasks.remove(this)
		}
	}
}

interface DownloadHelperListener {
	fun onDownloadStarted(title: String, path: String)
	fun onDownloadProgressUpdate(title: String, path: String, progress: Int)
	fun onDownloadCompleted(title: String, path: String, isSuccess: Boolean)
}

interface DownloadTaskListener {
	fun onDownloadStarted(title: String, path: String)
	fun onDownloadProgressUpdate(title: String, path: String, progress: Int)
	fun onDownloadCompleted(title: String, path: String, isSuccess: Boolean)
}