package com.zj.im.sender

import com.zj.im.main.ChatBase
import com.zj.im.utils.Constance.DEFAULT_TIMEOUT
import com.zj.im.utils.MutableParamsMap
import com.zj.im.utils.cusParamsMapOf
import com.zj.im.utils.getIncrementKey
import com.zj.im.utils.getIncrementNumber

/**
 * Created by ZJJ
 */

enum class SendingUp {
    NORMAL, READY, WAIT, CANCEL
}

@Suppress("unused")
class SendObject private constructor(private val callId: String) {

    companion object {

        /**
         * @param callId must be an unique and increment key,
         * you can follow up of your sending message all of the time.
         * */
        fun create(callId: String = getIncrementKey()): SendObject {
            return SendObject(callId)
        }
    }

    val createdTs = getIncrementNumber()
    private var timeOut = DEFAULT_TIMEOUT
    private var isResend = false
    private val param = cusParamsMapOf()
    private var onSendBefore: OnSendBefore? = null
    private var sendingUp: SendingUp = SendingUp.NORMAL

    fun put(name: String, value: Any?): SendObject {
        if (value != null) param.put(name, value)
        return this
    }

    fun <T : Any> putIf(name: String, value: T?, condition: (T) -> Boolean): SendObject {
        if (condition(value ?: return this)) param.put(name, value)
        return this
    }

    fun putAll(map: Map<String, Any>): SendObject {
        param.putAll(map)
        return this
    }

    fun putSubMap(name: String, map: Map<String, Any>): SendObject {
        param.putSubMap(name, map)
        return this
    }

    fun transaction(vararg names: String, block: (MutableParamsMap) -> Unit) {
        param.transactionSubParams(names, block)
    }

    fun timeOut(timeOut: Long): SendObject {
        this.timeOut = timeOut
        return this
    }

    fun build(): Call {
        return Call(this)
    }

    data class Call(private val sendObject: SendObject) {

        fun resend(onSendBefore: OnSendBefore? = null) {
            sendObject.onSendBefore = onSendBefore
            sendObject.isResend = true
            ChatBase.options?.sendToSocket(sendObject)
        }

        fun send(onSendBefore: OnSendBefore? = null) {
            sendObject.onSendBefore = onSendBefore
            ChatBase.options?.sendToSocket(sendObject)
        }
    }

    internal fun getParams(): Map<String, Any> {
        return param.get()
    }

    internal fun getCallId(): String {
        return callId
    }

    internal fun isOverrideSendingBefore(): Boolean {
        return onSendBefore != null
    }

    internal fun isResend(): Boolean {
        return isResend
    }

    internal fun getSendBefore(): OnSendBefore? {
        return onSendBefore
    }

    internal fun getTimeOut(): Long {
        return timeOut
    }

    internal fun getSendingUpState(): SendingUp {
        return sendingUp
    }

    internal fun setSendingUpState(s: SendingUp) {
        this.sendingUp = s
    }

    internal fun removeSendingBefore() {
        this.onSendBefore = null
    }
}
