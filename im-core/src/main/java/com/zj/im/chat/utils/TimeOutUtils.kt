package com.zj.im.chat.utils

import com.zj.im.chat.enums.SendMsgState
import com.zj.im.chat.core.DataStore
import com.zj.im.chat.interfaces.BaseMsgInfo
import com.zj.im.main.ChatBase
import com.zj.im.utils.runSync
import java.util.*

/**
 * created by ZJJ
 *
 * the Timeout utils ,Nonnull param callId
 *
 * */

internal object TimeOutUtils {

    private val sentMessages = hashMapOf<String, SentMsgInfo>()

    private var timer: Timer? = null

    fun init() {
        if (timer == null) {
            timer = Timer()
            timer?.schedule(DataTask(), 0, 1000)
        }
    }

    fun remove(callId: String?) {
        if (callId.isNullOrEmpty()) return
        sentMessages.runSync {
            if (it.containsKey(callId)) {
                it.remove(callId)
            }
        }
    }

    fun putASentMessage(callId: String, params: Map<String, Any>, timeOut: Long, isResend: Boolean, isIgnoreConnecting: Boolean = false) {
        sentMessages.runSync { it[callId] = SentMsgInfo(params, callId, timeOut, isResend, isIgnoreConnecting) }
    }

    private class DataTask : TimerTask() {
        override fun run() {
            sentMessages.runSync {
                val rev = arrayListOf<String>()
                it.forEach { (k, v) ->
                    if (v.isIgnoreConnecting || (ChatBase.options?.isRunning() == true && (ChatBase.isNetWorkAccess && ChatBase.isTcpConnected))) {
                        if (System.currentTimeMillis() - v.putTime >= v.timeOut) {
                            rev.add(k)
                        }
                    } else v.putTime = System.currentTimeMillis()
                }
                if (rev.isNotEmpty()) rev.forEach { t ->
                    it.remove(t)?.let { value ->
                        DataStore.put(BaseMsgInfo.sendingStateChange(SendMsgState.TIME_OUT, value.callId, value.params, value.isResend))
                    }
                }
                rev.clear()
            }
        }
    }

    private class SentMsgInfo(val params: Map<String, Any>, val callId: String, val timeOut: Long, val isResend: Boolean, val isIgnoreConnecting: Boolean, var putTime: Long = System.currentTimeMillis())
}
