@file:Suppress("unused")

package com.zj.im.persistence

import com.zj.im.chat.exceptions.ExceptionHandler

/**
 * Created by ZJJ
 */

abstract class Saver<CLS> {

    private var helperCls: CLS? = null

    private fun start() {
        if (helperCls == null) helperCls = createHelperCls()
    }

    protected fun getHelperCls(): CLS? {
        return helperCls
    }

    private fun createHelperCls(): CLS? {
        return onCreate()
    }

    internal fun run() {
        try {
            start()
            onCall(helperCls)
        } catch (e: Exception) {
            ExceptionHandler.postError("on Saver.call,case: ${e.message} ")
        } finally {
            onDestroyed(helperCls)
        }
    }

    protected abstract fun onCreate(): CLS?
    protected abstract fun onDestroyed(helperCls: CLS?)
    protected abstract fun onCall(helperCls: CLS?)
}