package com.zj.im.chat.hub

import com.zj.im.main.ChatBase

/**
 * created by ZJJ
 *
 * the base of client
 * */

open class BaseMessageHub {

    private var runningKey: String = ""

    internal fun initRunningKey(runningKey: String) {
        this.runningKey = runningKey
    }

    protected val isShutdown: Boolean; get() = ChatBase.isFinishing(runningKey)
}
