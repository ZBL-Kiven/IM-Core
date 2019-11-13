@file:Suppress("unused")

package com.zj.im.chat.core

import android.app.Application
import android.app.Notification
import com.zj.im.chat.enums.RuntimeEfficiency
import com.zj.im.utils.Constance

/**
 * created by ZJJ
 *
 * build as a proxy to init SDK, {@see }
 * */

class OptionProxy internal constructor(private val context: Application) {

    private var notification: Notification? = null
    private var sessionId: Int = 0
    private var logsCollectionAble: () -> Boolean = { false }
    private var logsMaxRetain: Long = Constance.MAX_RETAIN_TCP_LOG
    private var logsFileName: String = Constance.FOLDER_NAME
    private var runtimeEfficiency = RuntimeEfficiency.HIGH

    fun setNotify(notification: Notification?): OptionProxy {
        this.notification = notification
        return this
    }

    fun setSessionId(sessionId: Int): OptionProxy {
        this.sessionId = sessionId
        return this
    }

    fun setLevel(efficiency: RuntimeEfficiency): OptionProxy {
        this.runtimeEfficiency = efficiency
        return this
    }

    fun setLogsMaxRetain(maxRetain: Long): OptionProxy {
        this.logsMaxRetain = maxRetain
        return this
    }

    fun logsCollectionAble(logsCollectionAble: () -> Boolean): OptionProxy {
        this.logsCollectionAble = logsCollectionAble
        return this
    }

    fun logsFileName(logsFileName: String): OptionProxy {
        this.logsFileName = logsFileName
        return this
    }

    fun <OUT : Any> build(buildOption: OnBuildOption<OUT>): BaseOption<OUT> {
        return BaseOption(context, notification, sessionId, runtimeEfficiency, logsCollectionAble, logsFileName, logsMaxRetain, buildOption)
    }
}
