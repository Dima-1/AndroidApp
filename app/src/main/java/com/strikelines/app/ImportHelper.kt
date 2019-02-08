package com.strikelines.app

import android.os.AsyncTask
import com.strikelines.app.ui.MainActivity
import com.strikelines.app.utils.PlatformUtil
import net.osmand.aidl.tiles.CopyFileParams
import java.io.*
import java.lang.ref.WeakReference
import java.net.URI

class ImportHelper (
    val app: StrikeLinesApplication,
    activity: MainActivity,
    val uri: String,
    val fileName: String): AsyncTask<Void, Void, Boolean>( ){

    private val log = PlatformUtil.getLog(ImportHelper::class.java)
    private val chunkSize = 1024*256
    private val mainActivityRef by lazy {
        WeakReference<MainActivity>(activity)
    }

    companion object {
        const val SQLITE_EXT = ".sqlitedb"
        const val CHARTS_EXT = ".charts" //$NON-NLS-1$
    }

    override fun onPreExecute() {
        super.onPreExecute()
        val mainActivity: MainActivity? = mainActivityRef.get()
        mainActivity?.showLoader()
    }

    override fun doInBackground(vararg params: Void): Boolean? {
        return handleFileImport(uri, fileName)

    }

    override fun onPostExecute(result: Boolean?) {
        super.onPostExecute(result)
        val mainActivity: MainActivity? = mainActivityRef.get()
        if(result!=null && result) {
            mainActivity?.showSnackBar(StrikeLinesApplication.getApp()!!.applicationContext
                .getString(R.string.importFileSuccess).format(fileName), action = 2)
        } else {
            mainActivity?.showSnackBar(StrikeLinesApplication.getApp()!!.applicationContext
                .getString(R.string.importFileError).format(fileName), action = 2)
        }
        mainActivity?.isCopyingFile = false
        mainActivity?.dismissLoader()
        if(mainActivity?.isActivityVisible!!) {
            mainActivity.updateOsmandItemList()
        }
    }

    private fun handleFileImport(uri: String, fileName:String):Boolean {
        if (fileName.endsWith(SQLITE_EXT)) {
            return fileImportImpl(uri, fileName)
        } else if (fileName.endsWith(CHARTS_EXT)) {
            val newFilename = fileName.removePrefix(CHARTS_EXT)+ SQLITE_EXT
            return fileImportImpl(uri, newFilename)
        }
        return false
    }

    private fun fileImportImpl(uri: String, fileName: String):Boolean {
        val path = File(URI.create(uri)).absolutePath
        var counter = 0
        val copyStartTime = System.currentTimeMillis();
        try {
            var sentData = 0L
            var receivedData = 0L
            var data = ByteArray(chunkSize)
            val fileToCopy = File(path)
            val fileSize = fileToCopy.length()
            val bis: InputStream = DataInputStream(FileInputStream(fileToCopy))
            var read = 0

            var response = true
            do {
                if (fileSize - sentData <= chunkSize) {
                    data = ByteArray((fileSize - sentData).toInt())
                }
                if (response) {
                    receivedData += read
                    read = bis.read(data, 0, data.size)
                    sentData += read
                    counter++
                }

                response = app.osmandHelper.copyFile(CopyFileParams(fileName, fileSize, receivedData, data, copyStartTime))

            } while (read != 0)
            bis.close()
            return true
        } catch (ioe: IOException) {
            log.error(ioe.message, ioe)
            return false
        }
    }
}