package com.strikelines.app

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
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
    val uri: Uri,
    val fileName: String): AsyncTask<Void, Void, Boolean>( ){

    private val log = PlatformUtil.getLog(ImportHelper::class.java)
    private val chunkSize = 1024*256
    private val mainActivityRef by lazy {
        WeakReference<MainActivity>(activity)
    }

    companion object {
        const val SQLITE_EXT = ".sqlitedb"
        const val CHARTS_EXT = ".charts"
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

    private fun handleFileImport(uri: Uri, fileName:String):Boolean {
        if (fileName.endsWith(SQLITE_EXT)) {
            return fileImportImpl(uri, fileName)
        } else if (fileName.endsWith(CHARTS_EXT)) {
            val newFilename = fileName.removePrefix(CHARTS_EXT)+ SQLITE_EXT
            return fileImportImpl(uri, newFilename)
        }
        return false
    }

    private fun fileImportImpl(uri: Uri, fileName: String):Boolean {
        var counter = 0
        var receivedDataSize = 0L
        var isError = false
        var isReceived = true
        var data = ByteArray(chunkSize)
        val copyStartTime = System.currentTimeMillis()
        try {
            val bis:DataInputStream
            if (uri.scheme == "content") {
                bis = DataInputStream(app.contentResolver.openInputStream(uri))
            } else {
                bis = DataInputStream(FileInputStream(File(File(URI.create(uri.toString())).absolutePath)))
            }

            var read = 0
            while (read !=-1 ){
                var errorCount = 0
                if (isReceived) {
                    counter ++
                    receivedDataSize += read
                    read = bis.read (data)
                } else {
                    errorCount++
                    log.debug("errors: $errorCount")
                    if (errorCount > 100) {
                        isError = true
                        break
                    }
                }
                isReceived = app.osmandHelper.copyFile(CopyFileParams(fileName, receivedDataSize, data, copyStartTime, false))
            }
            bis.close()

            if (!isError) {
                app.osmandHelper.copyFile(CopyFileParams(fileName, 0, ByteArray(0), copyStartTime, true))
            } else {
                return false
            }
            return true
        } catch (ioe: IOException) {
            log.error(ioe.message, ioe)
            return false
        }
    }
}