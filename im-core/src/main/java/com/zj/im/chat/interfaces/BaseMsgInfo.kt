package com.zj.im.chat.interfaces

import android.net.NetworkInfo
import com.zj.im.chat.enums.SendMsgState
import com.zj.im.chat.enums.SocketState
import com.zj.im.chat.modle.AuthBuilder
import com.zj.im.chat.modle.SocketConnInfo
import com.zj.im.sender.SendObject
import com.zj.im.sender.SendingUp

/**
 * Created by ZJJ
 */
internal class BaseMsgInfo private constructor() {

    enum class MessageHandleType {
        NETWORK_STATE, RECEIVED_MSG, SOCKET_STATE, SEND_MSG, CONNECT_TO_SERVER, HEARTBEATS_SEND, AUTH_SEND, SEND_STATE_CHANGE, CLOSE_SOCKET, SEND_PROGRESS_CHANGED, AUTH_RESPONSE, HEARTBEATS_RESPONSE
    }

    var type: MessageHandleType? = null

    var data: Map<String, Any>? = null

    var params: Map<String, Any>? = null

    var connInfo: SocketConnInfo? = null

    var connStateChange: SocketState? = null

    var netWorkState: NetworkInfo.State = NetworkInfo.State.UNKNOWN

    var sendingState: SendMsgState? = null

    var isResend: Boolean = false

    var sendObject: SendObject? = null
        set(value) {
            value?.setSendingUpState(when {
                value.isOverrideSendingBefore() -> SendingUp.WAIT
                value.getSendingUpState() != SendingUp.NORMAL -> SendingUp.READY
                else -> SendingUp.NORMAL
            })
            field = value
        }

    var progress: Int = 0

    var authStatus: AuthBuilder.AuthStatus? = null

    var joinInTop = false

    /**
     *the pending id for per message，
     *
     * it used in the status notification ,example 'timeout' / 'sending status changed' / 'success' /...
     *
     * the default value is uuid
     * */
    var callId: String = ""


    companion object {
        fun heartBeats(params: Map<String, Any>?): BaseMsgInfo {
            val baseInfo = BaseMsgInfo()
            baseInfo.params = params
            baseInfo.type = MessageHandleType.HEARTBEATS_SEND
            return baseInfo
        }

        fun auth(callId: String, params: Map<String, Any>?): BaseMsgInfo {
            val baseInfo = BaseMsgInfo()
            baseInfo.params = params
            baseInfo.callId = callId
            baseInfo.type = MessageHandleType.AUTH_SEND
            return baseInfo
        }

        fun onProgressChange(progress: Int, callId: String): BaseMsgInfo {
            return BaseMsgInfo().apply {
                this.callId = callId
                this.progress = progress
                this.type = MessageHandleType.SEND_PROGRESS_CHANGED
            }
        }

        fun sendingStateChange(state: SendMsgState?, callId: String, params: Map<String, Any>?, isResend: Boolean): BaseMsgInfo {
            val baseInfo = BaseMsgInfo()
            baseInfo.sendingState = state
            baseInfo.callId = callId
            baseInfo.params = params
            baseInfo.isResend = isResend
            baseInfo.type = MessageHandleType.SEND_STATE_CHANGE
            return baseInfo
        }

        fun networkStateChanged(state: NetworkInfo.State): BaseMsgInfo {
            val baseInfo = BaseMsgInfo()
            baseInfo.netWorkState = state
            baseInfo.type = MessageHandleType.NETWORK_STATE
            return baseInfo
        }

        fun connectStateChange(connStateChange: SocketState, case: String = ""): BaseMsgInfo {
            val baseInfo = BaseMsgInfo()
            baseInfo.type = MessageHandleType.SOCKET_STATE
            baseInfo.connStateChange = connStateChange.apply {
                this.case = case
            }
            return baseInfo
        }

        fun closeSocket(): BaseMsgInfo {
            return BaseMsgInfo().apply {
                this.type = MessageHandleType.CLOSE_SOCKET
            }
        }

        fun connectToServer(connInfo: SocketConnInfo): BaseMsgInfo {
            val baseInfo = BaseMsgInfo()
            baseInfo.connInfo = connInfo
            baseInfo.type = MessageHandleType.CONNECT_TO_SERVER
            return baseInfo
        }

        fun sendMsg(sendObject: SendObject, joinInTop: Boolean = false): BaseMsgInfo {
            return BaseMsgInfo().apply {
                this.joinInTop = joinInTop
                this.sendObject = sendObject
                this.callId = sendObject.getCallId()
                this.sendingState = SendMsgState.SENDING
                this.type = MessageHandleType.SEND_MSG
            }
        }

        fun receiveMsg(data: Map<String, Any>?): BaseMsgInfo {
            val baseInfo = BaseMsgInfo()
            baseInfo.data = data
            baseInfo.type = MessageHandleType.RECEIVED_MSG
            return baseInfo
        }

        fun authResponse(authStatus: AuthBuilder.AuthStatus): BaseMsgInfo {
            return BaseMsgInfo().apply {
                this.authStatus = authStatus
                this.type = MessageHandleType.AUTH_RESPONSE
            }
        }

        fun heartBeatsResponse(): BaseMsgInfo {
            return BaseMsgInfo().apply {
                this.type = MessageHandleType.HEARTBEATS_RESPONSE
            }
        }
    }
}
