package com.strikelines.app

import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri

object AndroidUtils {

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
}
