package com.zj.im.sender

import com.zj.im.chat.core.DataStore
import com.zj.im.chat.exceptions.ExceptionHandler
import com.zj.im.chat.interfaces.BaseMsgInfo
import com.zj.im.chat.interfaces.RunningObserver
import com.zj.im.main.ChatBase
import com.zj.im.utils.cusListOf
import java.lang.Exception

/**
 * Created by ZJJ
 */

internal object SendingPool : RunningObserver() {

    fun init() {
        clear()
    }

    private var sending = false

    override fun run(runningKey: String) {
        if (!sending) {
            sending = true
            if (!ChatBase.isFinishing(runningKey)) {
                pop()?.let {
                    SendExecutors.send(it) {
                        sending = false
                    }
                    return
                }
            }
            sending = false
        }
    }

    private val sendMsgQueue = cusListOf<SendObject>()

    private val onProgressCover: (progress: Int, callId: String) -> Unit = { progress, callId ->
        DataStore.put(BaseMsgInfo.onProgressChange(progress, callId))
    }

    private val onSendBeforeEnd: (isContinue: Boolean, callId: String) -> Unit = { isContinue, callId ->
        val sendState = if (isContinue) SendingUp.READY else if (isStopHandleMsg) SendingUp.WAIT else SendingUp.CANCEL
        sendMsgQueue.getFirst { obj -> obj.getCallId() == callId }?.apply {
            this.setSendingUpState(sendState)
            if (isContinue) this.removeSendingBefore()
        }
    }

    fun push(sendObject: SendObject) {
        sendMsgQueue.add(sendObject)
        if (sendObject.isOverrideSendingBefore()) sendObject.getSendBefore()?.onSendBefore(onProgressCover, onSendBeforeEnd)
    }

    private fun pop(): SendObject? {
        if (sendMsgQueue.isEmpty()) return null
        if (isStopHandleMsg) {
            sendMsgQueue.mapTo(true) { BaseMsgInfo.sendMsg(it, true) }.forEach {
                DataStore.put(it)
            }
            sendMsgQueue.clear()
            return null
        }
        var firstInStay = sendMsgQueue.getFirst()
        if (firstInStay?.getSendingUpState() == SendingUp.WAIT) {
            firstInStay = sendMsgQueue.getFirst {
                it.getSendingUpState() == SendingUp.NORMAL
            }
        }
        firstInStay?.let {
            if (checkAndPop(it.getSendingUpState() == SendingUp.CANCEL)) {
                sendMsgQueue.remove(it)
                return it
            }
        }
        return null
    }

    private fun checkAndPop(isCancel: Boolean): Boolean {
        if (!isCancel) return true
        try {
            return ChatBase.options?.buildOption?.checkNetWorkIsWorking() ?: true
        } catch (ignored: Exception) {
            ExceptionHandler.postError(ignored)
        }
        return false
    }

    fun deleteFormQueue(callId: String?) {
        callId?.let {
            sendMsgQueue.removeIf { m ->
                m.getCallId() == callId
            }
        }
    }

    fun queryInSendingQueue(predicate: (SendObject) -> Boolean): Boolean {
        return sendMsgQueue.contains(predicate)
    }

    override fun getTotal(): Int {
        return try {
            sendMsgQueue.count
        } catch (e: Exception) {
            ExceptionHandler.postError(e)
            0
        }
    }

    fun clear() {
        sendMsgQueue.clear()
        sending = false
    }
}
