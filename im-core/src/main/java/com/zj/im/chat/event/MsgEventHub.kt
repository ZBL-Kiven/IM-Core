package com.zj.im.chat.event

import android.net.NetworkInfo
import com.zj.im.chat.core.DataStore
import com.zj.im.chat.enums.SendMsgState
import com.zj.im.chat.enums.SocketState
import com.zj.im.chat.hub.MessageHubClient
import com.zj.im.chat.hub.MessageHubServer
import com.zj.im.chat.interfaces.BaseMsgInfo
import com.zj.im.chat.interfaces.ConnectCallBack
import com.zj.im.chat.interfaces.HeartBeatsCallBack
import com.zj.im.chat.interfaces.SendReplyCallBack
import com.zj.im.chat.modle.AuthBuilder
import com.zj.im.chat.modle.SocketConnInfo
import com.zj.im.main.ChatBase
import com.zj.im.sender.SendObject
import com.zj.im.sender.SendingPool
import com.zj.im.utils.Constance
import com.zj.im.utils.log.e

/**
 * Created by ZJJ
 *
 * any message must be handle form here
 */
internal object MsgEventHub {

    fun put(data: BaseMsgInfo) {
        when (data.type) {
            BaseMsgInfo.MessageHandleType.SEND_MSG -> sendMsg(data.sendObject)
            BaseMsgInfo.MessageHandleType.RECEIVED_MSG -> receivedMsg(data.data)
            BaseMsgInfo.MessageHandleType.CONNECT_TO_SERVER -> connToServer(data.connInfo)
            BaseMsgInfo.MessageHandleType.CLOSE_SOCKET -> closeSocket()
            BaseMsgInfo.MessageHandleType.SOCKET_STATE -> onSocketStateChange(data.connStateChange)
            BaseMsgInfo.MessageHandleType.HEARTBEATS_SEND -> heartbeats(data.params)
            BaseMsgInfo.MessageHandleType.AUTH_SEND -> auth(data.params, data.callId)
            BaseMsgInfo.MessageHandleType.SEND_STATE_CHANGE -> sendStateChange(data.sendingState, data.callId, data.params, data.isResend)
            BaseMsgInfo.MessageHandleType.NETWORK_STATE -> networkStateChanged(data.netWorkState)
            BaseMsgInfo.MessageHandleType.SEND_PROGRESS_CHANGED -> onSendingProgress(data.callId, data.progress)
            BaseMsgInfo.MessageHandleType.AUTH_RESPONSE -> authStatusChange(data.authStatus)
            BaseMsgInfo.MessageHandleType.HEARTBEATS_RESPONSE -> onHeartBeatsResponse()
        }
    }

    private fun closeSocket() {
        getServer("closeSocket")?.getService("closeSocket", true)?.closeSocket()
    }

    private fun sendMsg(sendObject: SendObject?) {
        if (sendObject != null) SendingPool.push(sendObject)
    }

    private fun auth(params: Map<String, Any>?, callId: String) {
        val sendObject = SendObject.create(callId).putAll(params ?: return)
        getServer("auth")?.sendToSocket(sendObject, object : SendReplyCallBack {
            override fun onReply(isSuccess: Boolean, sendObject: SendObject, e: Throwable?) {
                if (!isSuccess) {
                    DataStore.put(BaseMsgInfo.sendingStateChange(SendMsgState.FAIL, callId, params, sendObject.isResend()))
                }
            }
        })
    }

    private fun sendStateChange(state: SendMsgState?, callId: String?, param: Map<String, Any>?, isResend: Boolean) {
        getClient("sendStateChange")?.setSendingState(state, callId, param, isResend)
    }

    private fun networkStateChanged(netWorkState: NetworkInfo.State) {
        getClient("networkStateChanged")?.setNetworkState(netWorkState)
    }

    private fun receivedMsg(data: Map<String, Any>?) {
        getClient("receivedMsg")?.sendReceivedData(data)
    }

    private fun connToServer(connInfo: SocketConnInfo?) {
        getServer("connToServer")?.connect(connInfo?.address ?: "", connInfo?.port ?: 0, connInfo?.socketTimeOut, object : ConnectCallBack {
            override fun onConnection(isSuccess: Boolean, throwable: Throwable?) {
                val state = if (isSuccess) SocketState.CONNECTED else SocketState.CONNECTED_ERROR
                DataStore.put(BaseMsgInfo.connectStateChange(state, Constance.CONNECT_ERROR))
            }
        })
    }

    private fun heartbeats(params: Map<String, Any>?) {
        if (params != null) {
            getServer("heartbeats")?.onHeartBeatsRequest(params, object : HeartBeatsCallBack {
                override fun heartBeats(isOK: Boolean, throwable: Throwable?) {
                    if (isOK) getClient("heartbeats")?.nextHeartBeats()
                    else {
                        DataStore.put(BaseMsgInfo.connectStateChange(SocketState.CONNECTED_ERROR, "the heartbeats was failed to send to server"))
                    }
                }
            })
        } else {
            getClient("heartbeats")?.nextHeartBeats()
            e("MessageHubClient.startHeartBeats", "heart-beats was not work on this time with null params")
        }
    }

    private fun onSocketStateChange(state: SocketState?) {
        getClient("onSocketStateChange")?.changeSocketState(state ?: SocketState.INIT)
    }

    private fun onSendingProgress(callId: String, progress: Int) {
        getClient("onSendingProgress")?.onSendingProgress(callId, progress)
    }

    private fun onHeartBeatsResponse() {
        getClient("onHeartBeatsResponse")?.onHeartbeatsReceived()
    }

    private fun authStatusChange(state: AuthBuilder.AuthStatus?) {
        getClient("authStatusChange")?.authStateChange(state)
    }


    private fun getServer(where: String): MessageHubServer? {
        return ChatBase.options?.getServer(where)
    }

    private fun getClient(where: String): MessageHubClient<*>? {
        return ChatBase.options?.getClient(where)
    }
}
