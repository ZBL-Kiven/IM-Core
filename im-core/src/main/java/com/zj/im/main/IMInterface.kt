@file:Suppress("unused")

package com.zj.im.main

import com.zj.im.chat.utils.netUtils.NetWorkInfo
import com.zj.im.chat.enums.SocketState
import com.zj.im.chat.exceptions.ChatException
import com.zj.im.chat.exceptions.ExceptionHandler
import com.zj.im.chat.interfaces.LifecycleListener
import com.zj.im.chat.utils.NetWorkState
import com.zj.im.chat.utils.StateIgnoreFluctuateUtils
import com.zj.im.chat.core.BaseOption
import com.zj.im.chat.enums.RuntimeEfficiency
import com.zj.im.listeners.MsgReceivedListener
import com.zj.im.utils.log.NetRecordUtils
import com.zj.im.utils.log.TCPNetRecordChangedListener
import com.zj.im.utils.runSync

/**
 * created by ZJJ
 *
 * extend this and call init before use ,or it will be crash without init!!
 *
 * the entry of chatModule ,call register/unRegister listeners to observer/cancel the msg received
 *
 * you can call pause/resume to modify the messagePool`s running state.
 * */

abstract class IMInterface<OUT : Any> {

    val isRunningInBackground: Boolean
        get() = ChatBase.isRunningInBackground

    protected fun initChat(option: BaseOption<OUT>) {
        option.setMsgInterface(this)
        ChatBase.init(option)
    }

    protected fun postChangeData(out: OUT) {
        msgReceive(out)
    }

    /**
     * notify subscriber to refresh data when something received
     */
    internal fun msgReceive(data: OUT) {
        msgListeners.runSync {
            it.forEach { (_, v) ->
                if (v.canReceive) {
                    v.sendTo(data)
                }
            }
        }
    }

    /**
     * notify subscriber to refresh local data when changed
     */
    protected fun refreshLocalData() {
        msgListeners.runSync {
            it.forEach { (_, v) ->
                if (v.canReceive) v.refreshLocalData()
            }
        }
    }

    protected fun <CLS> refreshTypeData(cls: Class<CLS>) {
        msgListeners.runSync {
            it.forEach { (_, v) ->
                if (v.canReceive && v.query.getQueryClass()?.simpleName == cls.simpleName) v.refreshLocalData()
            }
        }
    }

    /**
     * the socket status changed
     */
    internal fun socketStatusChange(state: SocketState) {
        try {
            StateIgnoreFluctuateUtils.sendSocketStatus(state)
        } catch (e: Exception) {
            ExceptionHandler.postError(e)
        }
    }

    /**
     * the network status changed
     * */
    fun netWorkStateChanged(netWorkState: NetWorkInfo) {
        try {
            StateIgnoreFluctuateUtils.sendNetWorkStatus(netWorkState)
        } catch (e: Exception) {
            ExceptionHandler.postError(e)
        }
    }

    /**
     * query is msg exists in msg queue
     * */
    fun queryInSending(callId: String?): Boolean {
        return ChatBase.queryInQueue(callId)
    }

    /**
     * remove msg if it exists in msg queue
     * */
    fun cancelSendingMsg(callId: String?) {
        ChatBase.deleteFormQueue(callId)
    }

    internal fun progressUpdate(percent: Int, callId: String) {
        msgListeners.runSync {
            it.forEach { (_, v) ->
                if (v.canReceive) v.onProgress(percent, callId)
            }
        }
    }

    private var msgListeners: MutableMap<String, MsgReceivedListener<*, OUT>>? = null
        get() {
            if (field == null) field = mutableMapOf()
            return field
        }

    fun getCurSocketState(): SocketState {
        return StateIgnoreFluctuateUtils.getCurSocketState()
    }

    fun getCurNetWorkState(): NetWorkState {
        return StateIgnoreFluctuateUtils.getCurNetWorkState()
    }

    fun registerSocketStateChangeListener(name: String, ignoreFluctuate: Boolean, observer: (SocketState) -> Unit) {
        StateIgnoreFluctuateUtils.registerSocketStateChangeListener(name, ignoreFluctuate, observer)
    }

    fun removeSocketStateChangeListener(name: String) {
        StateIgnoreFluctuateUtils.removeSocketStateChangeListener(name)
    }

    fun registerNetWorkStateChangeListener(name: String, ignoreFluctuate: Boolean, observer: (NetWorkState) -> Unit) {
        StateIgnoreFluctuateUtils.registerNetWorkStateChangeListener(name, ignoreFluctuate, observer)
    }

    fun removeNetWorkStateChangeListener(name: String) {
        StateIgnoreFluctuateUtils.removeNetWorkStateChangeListener(name)
    }

    fun registerLifecycleListener(name: String, listener: LifecycleListener) {
        ChatBase.options?.registerLifecycleListener(name, listener)
    }

    fun unRegisterLifecycleListener(name: String) {
        ChatBase.options?.unRegisterLifecycleListener(name)
    }

    fun registerRecordListener(name: String, tc: TCPNetRecordChangedListener) {
        NetRecordUtils.addRecordListener(name, tc)
    }

    fun unRegisterRecordListener(name: String) {
        NetRecordUtils.removeRecordListener(name)
    }

    fun <T : MsgReceivedListener<*, OUT>> registerMsgListener(name: String, listener: T) {
        msgListeners.runSync {
            it[name] = listener
        }
    }

    fun unRegisterMsgListener(name: String) {
        msgListeners.runSync {
            it.remove(name)
        }
    }

    protected fun setFrequency(efficiency: RuntimeEfficiency) {
        ChatBase.options?.setFrequency(efficiency)
    }

    fun pause(code: Int) {
        ChatBase.options?.pause(code)
    }

    fun resume(code: Int) {
        ChatBase.options?.resume(code)
    }

    fun shutDown() {
        ChatBase.shutDown()
    }

    fun postError(e: ChatException) {
        ChatBase.postError(e)
    }

    fun overrideOnConnected(obj: (isContinue: (Boolean) -> Unit) -> Unit) {
        ChatBase.options?.overrideOnConnected(obj)
    }

    fun overrideOnAuthSuccess(obj: (isContinue: (Boolean) -> Unit) -> Unit) {
        ChatBase.options?.overrideOnAuthSuccess(obj)
    }

    fun cancelTimeOut(callId: String) {
        ChatBase.options?.cancelTimeOut(callId)
    }

    fun reconnection(case: String) {
        ChatBase.correctConnectionState(SocketState.CONNECTED_ERROR, case)
    }

    fun showToast(s: String) {
        ChatBase.show(s)
    }

    fun getLogsFolderPath(zipFolderName: String, zipFileName: String): String {
        return ChatBase.getLogsFolder(zipFolderName, zipFileName)
    }
}
