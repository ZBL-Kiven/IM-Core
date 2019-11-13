package com.zj.im.listeners

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.zj.im.main.ChatBase.context

/**
 * Created by ZJJ
 */
object DropRecoverListener : Application.ActivityLifecycleCallbacks {

    private var interrupting: (() -> Boolean)? = null

    fun init(interrupting: () -> Boolean) {
        this.interrupting = interrupting
        context?.registerActivityLifecycleCallbacks(this)
    }

    override fun onActivityPaused(activity: Activity?) {

    }

    override fun onActivityResumed(activity: Activity?) {
        interrupting?.let {
            if (!it.invoke()) {
                destroy()
            }
        }
    }

    override fun onActivityStarted(activity: Activity?) {

    }

    override fun onActivityDestroyed(activity: Activity?) {

    }

    override fun onActivitySaveInstanceState(activity: Activity?, outState: Bundle?) {
    }

    override fun onActivityStopped(activity: Activity?) {
    }

    override fun onActivityCreated(activity: Activity?, savedInstanceState: Bundle?) {
    }

    fun destroy() {
        context?.unregisterActivityLifecycleCallbacks(this)
    }

}
