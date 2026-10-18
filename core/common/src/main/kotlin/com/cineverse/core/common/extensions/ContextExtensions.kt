package com.cineverse.core.common.extensions

import android.app.UiModeManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration

/** True on Android TV, Google TV and Fire TV. */
fun Context.isTelevision(): Boolean {
    val uiModeManager = getSystemService(UiModeManager::class.java)
    return uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
}

/** The intent that opens this app's launch activity: the Leanback launcher activity on TV. */
fun Context.appLaunchIntent(): Intent? =
    if (isTelevision()) {
        packageManager.getLeanbackLaunchIntentForPackage(packageName)
    } else {
        packageManager.getLaunchIntentForPackage(packageName)
    }
