package com.strikelines.app.utils

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.AsyncTask
import android.support.annotation.ColorInt
import android.support.annotation.ColorRes
import android.support.annotation.DrawableRes
import android.support.v4.content.ContextCompat
import android.support.v4.graphics.drawable.DrawableCompat
import android.view.Surface
import android.view.WindowManager
import android.widget.ImageView
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*
import android.widget.LinearLayout
import android.graphics.drawable.BitmapDrawable
import android.opengl.ETC1.getHeight
import android.opengl.ETC1.getWidth
import android.graphics.Bitmap
import android.provider.MediaStore.Images.Media.getBitmap



const val GRAYSCALE_PHOTOS_DIR = "grayscale_photos/"

const val GRAYSCALE_PHOTOS_EXT = ".jpeg"

class UiUtils(private val app: Context) {
    private val drawableCache = LinkedHashMap<Long, Drawable>()
    private val circleBitmapCache = LinkedHashMap<String, Bitmap>()

    private val isLightContent: Boolean
        get() = true



    private fun getDrawable(@DrawableRes resId: Int, @ColorRes clrId: Int): Drawable? {
        val hash = (resId.toLong() shl 31) + clrId
        var d: Drawable? = drawableCache[hash]
        if (d == null) {
            d = ContextCompat.getDrawable(app, resId)
            if (d != null) {
                d = DrawableCompat.wrap(d)
                d!!.mutate()
                if (clrId != 0) {
                    DrawableCompat.setTint(d, ContextCompat.getColor(app, clrId))
                }
                drawableCache[hash] = d
            }
        }
        return d
    }

    private fun getPaintedDrawable(@DrawableRes resId: Int, @ColorInt color: Int): Drawable? {
        val hash = (resId.toLong() shl 31) + color
        var d: Drawable? = drawableCache[hash]
        if (d == null) {
            d = ContextCompat.getDrawable(app, resId)
            if (d != null) {
                d = DrawableCompat.wrap(d)
                d!!.mutate()
                DrawableCompat.setTint(d, color)
                drawableCache[hash] = d
            }
        }
        return d
    }

    fun getPaintedIcon(@DrawableRes id: Int, @ColorInt color: Int): Drawable? {
        return getPaintedDrawable(id, color)
    }

    fun getIcon(@DrawableRes id: Int, @ColorRes colorId: Int): Drawable? {
        return getDrawable(id, colorId)
    }


    private fun getScreenOrientation(): Int {
        // screenOrientation correction must not be applied for devices without compass
        val sensorManager = app.getSystemService(Context.SENSOR_SERVICE) as SensorManager?
        if (sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) == null) {
            return 0
        }

        val windowManager = app.getSystemService(Context.WINDOW_SERVICE) as WindowManager?
        val rotation = windowManager?.defaultDisplay?.rotation ?: return 0

        return when (rotation) {
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> 0
        }
    }

}