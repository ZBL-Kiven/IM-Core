package com.zj.im.sender

import android.accounts.NetworkErrorException
import com.zj.im.chat.enums.SendMsgState
import com.zj.im.chat.exceptions.ExceptionHandler
import com.zj.im.chat.core.DataStore
import com.zj.im.chat.interfaces.BaseMsgInfo
import com.zj.im.chat.interfaces.SendReplyCallBack
import com.zj.im.chat.utils.TimeOutUtils
import com.zj.im.main.ChatBase
import com.zj.im.utils.log.NetRecordUtils

/**
 * Created by ZJJ
 */

internal object SendExecutors {

    fun send(sendObject: SendObject, done: () -> Unit) {
        try {
            when {
                sendObject.getSendingUpState() == SendingUp.CANCEL -> {
                    try {
                        sendingFail(sendObject, NetworkErrorException(""))
                    } finally {
                        done()
                    }
                }
                else -> {
                    NetRecordUtils.recordSendCount()
                    TimeOutUtils.putASentMessage(sendObject.getCallId(), sendObject.getParams(), sendObject.getTimeOut(), sendObject.isResend())
                    ChatBase.options?.getServer("SendExecutors.send")?.sendToSocket(sendObject, object : SendReplyCallBack {
                        override fun onReply(isSuccess: Boolean, sendObject: SendObject, e: Throwable?) {
                            try {
                                if (!isSuccess) {
                                    if (!(ChatBase.isNetWorkAccess && ChatBase.isTcpConnected)) {
                                        TimeOutUtils.remove(sendObject.getCallId())
                                        DataStore.put(BaseMsgInfo.sendMsg(sendObject))
                                        return
                                    }
                                    sendingFail(sendObject, e)
                                }
                            } finally {
                                done()
                            }
                        }
                    })
                }
            }
        } catch (e: Exception) {
            done()
            ExceptionHandler.postError(e)
        }
    }

    private fun sendingFail(sendObject: SendObject, e: Throwable?) {
        ExceptionHandler.postError(e)
        TimeOutUtils.remove(sendObject.getCallId())
        DataStore.put(BaseMsgInfo.sendingStateChange(SendMsgState.FAIL, sendObject.getCallId(), sendObject.getParams(), sendObject.isResend()))
    }
}
