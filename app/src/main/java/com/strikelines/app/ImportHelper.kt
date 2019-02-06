package com.strikelines.app

import android.annotation.SuppressLint
import android.net.Uri
import android.os.AsyncTask
import android.support.v7.app.AppCompatActivity
import com.strikelines.app.ui.MainActivity
import com.strikelines.app.utils.PlatformUtil
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
    val mainActivityRef by lazy {
        WeakReference<MainActivity>(activity)
    }


    companion object {
        const val SQLITE_EXT = ".sqlitedb"
        const val CHARTS_EXT = ".charts" //$NON-NLS-1$
    }

    override fun onPreExecute() {
        super.onPreExecute()
        var mainActivity: MainActivity? = mainActivityRef.get()
        mainActivity?.showLoader()
    }

    override fun doInBackground(vararg params: Void): Boolean? {
        return handleFileImport(uri, fileName)

    }

    override fun onPostExecute(result: Boolean?) {
        super.onPostExecute(result)
        var mainActivity: MainActivity? = mainActivityRef.get()
        if(result!=null && result) {
            mainActivity?.showSnackBar("Import of ${fileName} into OsmAnd SUCCESSFUL!", action = 2)
        } else {
            mainActivity?.showSnackBar("Error! Import of ${fileName} into OsmAnd FAILED!", action = 2)
        }
        mainActivity?.isCopingFile = false
        mainActivity?.dismissLoader()
        if(mainActivity?.isActivityVisible!!) {
            mainActivity.updateOsmandItemList()
        }
    }

    fun handleFileImport(uri: String, fileName:String):Boolean {
        if (fileName.endsWith(SQLITE_EXT)) {
            return handleSqliteCopy(uri, fileName)
        } else if (fileName.endsWith(CHARTS_EXT)) {
            val newFilename = fileName.removePrefix(CHARTS_EXT)+ SQLITE_EXT
            return handleSqliteCopy(uri, newFilename)
        }
        return false
    }

    private fun handleSqliteCopy(uri: String, fileName: String):Boolean {
        val path = File(URI.create(uri)).absolutePath
        var counter = 0


        try {
            var sentData = 0L
            var data = ByteArray(chunkSize)
            val fileToCopy = File(path)
            val fileSize = fileToCopy.length()
            val bis: InputStream = DataInputStream(FileInputStream(fileToCopy))
            var read = 0

            var response = true
            while (read != -1) {
                if (fileSize - sentData <= chunkSize) {
                    data = ByteArray((fileSize - sentData).toInt()+1)
                }
                if (response) {
                    read = bis.read(data, 0, data.size)
                    sentData += read
                    counter++
                }

                response = app.osmandHelper.appendDataToFile(data, fileName)
                //log.debug("data chunks count: $counter, response: $response, chunk size ${data.size}")

            }
            app.osmandHelper.appendDataToFile(ByteArray(0), fileName)
            bis.close()
            return true
        } catch (ioe: IOException) {
            log.error(ioe.message, ioe)
            return false
        }
    }
}