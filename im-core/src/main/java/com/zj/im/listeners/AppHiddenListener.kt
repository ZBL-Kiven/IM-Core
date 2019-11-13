package com.zj.im.listeners

import android.app.Application
import android.content.ComponentCallbacks2
import android.content.ComponentCallbacks2.*
import android.content.res.Configuration

/**
 * Created by ZJJ
 */

object AppHiddenListener : ComponentCallbacks2 {

    private var appHiddenListener: (() -> Unit)? = null

    fun init(context: Application?, appHiddenListener: () -> Unit) {
        this.appHiddenListener = appHiddenListener
        context?.registerComponentCallbacks(this)
    }

    override fun onLowMemory() {
        System.gc()
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {

    }

    override fun onTrimMemory(level: Int) {
        if (level == TRIM_MEMORY_UI_HIDDEN) appHiddenListener?.invoke()
    }

    fun shutDown(context: Application?) {
        context?.unregisterComponentCallbacks(this)
    }
}