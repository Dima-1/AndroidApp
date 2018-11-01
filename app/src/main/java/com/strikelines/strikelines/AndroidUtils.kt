package com.strikelines.strikelines

import android.content.Context
import android.content.pm.PackageManager

object AndroidUtils {

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
