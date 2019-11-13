package com.zj.im.chat.interfaces

import com.zj.im.main.ChatBase

/**
 * Created by ZJJ
 */

internal abstract class RunningObserver {

    protected abstract fun run(runningKey: String)

    val isStopHandleMsg: Boolean; get() = !(ChatBase.isNetWorkAccess && ChatBase.isTcpConnected)

    open fun getTotal(): Int {
        return 0
    }

    private var lock: Boolean = false
    private var isRunning = false

    fun runningInBlock(runningKey: String) {
        if (lock || isRunning) return
        try {
            lock = true
            isRunning = true
            run(runningKey)
        } finally {
            isRunning = false
            lock = false
        }
    }
}
