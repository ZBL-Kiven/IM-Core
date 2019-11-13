@file:Suppress("unused")

package com.zj.im.chat.core

import com.zj.im.chat.event.MsgEventHub
import com.zj.im.chat.exceptions.ExceptionHandler
import com.zj.im.chat.interfaces.BaseMsgInfo
import com.zj.im.chat.interfaces.RunningObserver
import com.zj.im.chat.modle.AuthBuilder
import com.zj.im.chat.utils.EfficiencyUtils
import com.zj.im.main.ChatBase
import com.zj.im.sender.SendingPool
import com.zj.im.utils.CustomList
import com.zj.im.utils.cusListOf
import com.zj.im.utils.runSync

/**
 * created by ZJJ
 *
 * the message queue for sdk
 *
 * thread-safety list will pop their top  by their priority
 *
 * */
internal object DataStore : RunningObserver() {

    fun init() {
        clear()
    }

    override fun run(runningKey: String) {
        runSync {
            if (!ChatBase.isFinishing(runningKey)) {
                val info = pop()
                if (info != null) MsgEventHub.put(info)
            }
        }
    }

    //PRI = 2
    private val netWorkStateChanged = cusListOf<BaseMsgInfo>()
    //PRI = 5
    private val sendAuth = cusListOf<BaseMsgInfo>()
    //PRI = 8
    private val sendMsg = cusListOf<BaseMsgInfo>()
    //PRI = 4
    private val connectStateChanged = cusListOf<BaseMsgInfo>()
    //PRI = 6
    private val heartBeats = cusListOf<BaseMsgInfo>()
    //PRI = 3
    private val connectToServers = cusListOf<BaseMsgInfo>()
    //PRI = 1
    private val closeSocket = cusListOf<BaseMsgInfo>()
    //PRI = 7
    private val sendStateChanged = cusListOf<BaseMsgInfo>()
    //PRI = 9
    private val receivedMsg = cusListOf<BaseMsgInfo>()
    //PRI = 0
    private val simpleStatusFound = cusListOf<BaseMsgInfo>()
    //PRI = 10
    private val sendingProgress = cusListOf<BaseMsgInfo>()

    fun put(info: BaseMsgInfo) {
        EfficiencyUtils.checkEfficiency()
        MsgHandler.checkRunning()
        when (info.type) {
            BaseMsgInfo.MessageHandleType.AUTH_SEND -> {
                sendAuth.addOnly(info)
            }
            BaseMsgInfo.MessageHandleType.CONNECT_TO_SERVER -> {
                connectToServers.addOnly(info)
            }
            BaseMsgInfo.MessageHandleType.HEARTBEATS_SEND -> {
                heartBeats.addOnly(info)
            }
            BaseMsgInfo.MessageHandleType.SOCKET_STATE -> {
                connectStateChanged.add(info)
            }
            BaseMsgInfo.MessageHandleType.SEND_MSG -> {
                val index = if (info.joinInTop) 0 else -1
                sendMsg.addIf(info, index = index) { `in`, other -> `in`.callId != other.callId }
                sendMsg.sort { it.sendObject?.createdTs ?: 0.0 }
            }
            BaseMsgInfo.MessageHandleType.RECEIVED_MSG -> {
                if (filterHeartBeatsOrAuthResponseFormReceived(info))
                    receivedMsg.add(info)
            }
            BaseMsgInfo.MessageHandleType.SEND_STATE_CHANGE -> {
                sendStateChanged.add(info)
            }
            BaseMsgInfo.MessageHandleType.NETWORK_STATE -> {
                netWorkStateChanged.addOnly(info)
            }
            BaseMsgInfo.MessageHandleType.CLOSE_SOCKET -> {
                closeSocket.addOnly(info)
            }
            BaseMsgInfo.MessageHandleType.AUTH_RESPONSE -> {
                simpleStatusFound.add(info)
            }
            BaseMsgInfo.MessageHandleType.HEARTBEATS_RESPONSE -> {
                simpleStatusFound.add(info)
            }
            BaseMsgInfo.MessageHandleType.SEND_PROGRESS_CHANGED -> {
                sendingProgress.addOnly(info)
            }
        }
    }

    private fun pop(): BaseMsgInfo? {

        when {
            /**
             * when heartbeats / auth received
             * */
            simpleStatusFound.isNotEmpty() -> {
                return getFirst(simpleStatusFound) { it, lst ->
                    lst.remove(it)
                }
            }
            /**
             * when socket closing
             */
            closeSocket.isNotEmpty() -> {
                return getFirst(closeSocket) { _, lst ->
                    lst.clear()
                }
            }
            /**
             * when network status changed
             */
            netWorkStateChanged.isNotEmpty() -> {
                return getFirst(netWorkStateChanged) { it, lst ->
                    lst.remove(it)
                }
            }
            /**
             * when con / recon to server
             */
            connectToServers.isNotEmpty() -> {
                return getFirst(connectToServers) { _, lst ->
                    lst.clear()
                }
            }
            /**
             * when connection status changed
             */
            connectStateChanged.isNotEmpty() -> {
                return getFirst(connectStateChanged) { it, lst ->
                    lst.remove(it)
                }
            }
            /**
             * when auth sent
             */
            sendAuth.isNotEmpty() && canAuth() -> {
                return getFirst(sendAuth) { _, lst ->
                    lst.clear()
                }
            }
            /**
             * when heartbeats called
             */
            heartBeats.isNotEmpty() -> {
                return getFirst(heartBeats) { _, lst ->
                    lst.clear()
                }
            }
            /**
             * when some msg send status changed
             */
            sendStateChanged.isNotEmpty() -> {
                return getFirst(sendStateChanged) { it, lst ->
                    lst.remove(it)
                }
            }
            /**
             * when send a msg
             */
            isSending() && sendMsg.isNotEmpty() && !isStopHandleMsg -> {
                return getFirst(sendMsg) { it, lst ->
                    lst.remove(it)
                }
            }
            /**
             * when receive something
             */
            receivedMsg.isNotEmpty() && isReceiving() -> {
                return getFirst(receivedMsg) { it, lst ->
                    lst.remove(it)
                }
            }
            /**
             * when sending progress changed
             * */
            sendingProgress.isNotEmpty() -> {
                return getFirst(sendingProgress) { _, lst ->
                    lst.clear()
                }
            }
        }
        return null
    }

    private fun filterHeartBeatsOrAuthResponseFormReceived(info: BaseMsgInfo): Boolean {
        val data = info.data
        val interrupt = isHeartBeatsOrAuthResponse(data)
        val authResponse = interrupt.third
        if (!interrupt.first) return false
        if (interrupt.second) {
            put(BaseMsgInfo.heartBeatsResponse());return false
        }
        if (authResponse != null) {
            put(BaseMsgInfo.authResponse(authResponse));return false
        }
        return true
    }


    private fun <T> getFirst(lst: CustomList<T>, before: (T, CustomList<T>) -> Unit): T? {
        return lst.getFirst()?.apply {
            before.invoke(this, lst)
        }
    }

    private var isSending: () -> Boolean = { true }
    private var isReceiving: () -> Boolean = { true }
    private var isHeartBeatsOrAuthResponse: (data: Map<String, Any>?) -> Triple<Boolean, Boolean, AuthBuilder.AuthStatus?> = { Triple(first = true, second = false, third = null) }
    private var canAuth: () -> Boolean = { true }

    fun canSend(isSending: () -> Boolean) {
        DataStore.isSending = isSending
    }

    fun canReceive(isReceiving: () -> Boolean) {
        DataStore.isReceiving = isReceiving
    }

    fun isHeartBeatsOrAuthResponse(isHeartBeatsOrAuthResponse: (data: Map<String, Any>?) -> Triple<Boolean, Boolean, AuthBuilder.AuthStatus?>) {
        this.isHeartBeatsOrAuthResponse = isHeartBeatsOrAuthResponse
    }

    fun canAuth(auth: () -> Boolean) {
        canAuth = auth
    }

    fun queryInMsgQueue(predicate: (BaseMsgInfo) -> Boolean): Boolean {
        return sendMsg.contains(predicate)
    }

    fun deleteFormQueue(callId: String?) {
        callId?.let {
            sendMsg.removeIf { m ->
                m.callId == callId
            }
        }
    }

    override fun getTotal(): Int {
        return try {
            netWorkStateChanged.count + sendAuth.count + sendMsg.count + connectStateChanged.count + heartBeats.count + connectToServers.count + closeSocket.count + sendStateChanged.count + receivedMsg.count + simpleStatusFound.count + sendingProgress.count
        } catch (e: Exception) {
            ExceptionHandler.postError(e)
            0
        }
    }

    fun shutDown() {
        MsgHandler.shutdown()
        SendingPool.clear()
        clear()
    }

    fun clear() {
        sendAuth.clear()
        sendMsg.clear()
        connectStateChanged.clear()
        heartBeats.clear()
        connectToServers.clear()
        sendStateChanged.clear()
        receivedMsg.clear()
    }
}
