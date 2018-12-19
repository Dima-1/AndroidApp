package com.strikelines.app.utils

import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentManager
import android.text.format.DateFormat
import android.view.View
import android.view.WindowManager
import java.text.MessageFormat
import java.util.*

object AndroidUtils {

	private val FORMAT_GB = MessageFormat("{0, number,#.##} GB", Locale.US)
	private val FORMAT_MB = MessageFormat("{0, number,##.#} MB", Locale.US)
	private val FORMAT_KB = MessageFormat("{0, number,##.#} kB", Locale.US)

	fun resourceToUri(ctx: Context, resId: Int): Uri {
		val pack = ctx.resources.getResourcePackageName(resId)
		val type = ctx.resources.getResourceTypeName(resId)
		val entry = ctx.resources.getResourceEntryName(resId)
		return Uri.parse("${ContentResolver.SCHEME_ANDROID_RESOURCE}://$pack/$type/$entry")
	}

	fun isAppInstalled(ctx: Context, appPackage: String) = when {
		appPackage.isEmpty() -> false
		else -> try {
			ctx.packageManager.getPackageInfo(appPackage, 0)
			true
		} catch (e: PackageManager.NameNotFoundException) {
			false
		}
	}

	fun enterToTransparentFullScreen(activity: Activity) {
		if (Build.VERSION.SDK_INT >= 23) {
			val window = activity.window
			window.statusBarColor = Color.TRANSPARENT
			window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or
					View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
					View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
		}
	}

	fun enterToTranslucentFullScreen(activity: Activity) {
		if (Build.VERSION.SDK_INT >= 19) {
			activity.window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
		}
	}

	fun dismissAllDialogs(fm: FragmentManager) {
		for (fragment in fm.fragments) {
			if (fragment is DialogFragment) {
				fragment.dismissAllowingStateLoss()
			}
			dismissAllDialogs(fragment.childFragmentManager)
		}
	}

	fun formatDate(ctx: Context, time: Long): String {
		return DateFormat.getDateFormat(ctx).format(Date(time))
	}

	fun formatDateTime(ctx: Context, time: Long): String {
		val d = Date(time)
		return DateFormat.getDateFormat(ctx).format(d) +
				" " + DateFormat.getTimeFormat(ctx).format(d)
	}

	fun formatTime(ctx: Context, time: Long): String {
		return DateFormat.getTimeFormat(ctx).format(Date(time))
	}

	fun formatSize(sizeBytes: Long): String {
		val sizeKb = (sizeBytes + 512 shr 10).toInt()
		return if (sizeKb > 0) {
			if (sizeKb > 1 shl 20) {
				FORMAT_GB.format(arrayOf<Any>(sizeKb.toFloat() / (1 shl 20)))
			} else if (sizeBytes > 100 * (1 shl 10)) {
				FORMAT_MB.format(arrayOf<Any>(sizeBytes.toFloat() / (1 shl 20)))
			} else {
				FORMAT_KB.format(arrayOf<Any>(sizeBytes.toFloat() / (1 shl 10)))
			}
		} else ""
	}

	fun getIntentForBrowser(url:String) = Intent(Intent.ACTION_VIEW, Uri.parse(url))


}
