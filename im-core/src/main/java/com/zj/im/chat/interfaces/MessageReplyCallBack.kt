package com.zj.im.chat.interfaces

import com.zj.im.sender.SendObject

/**
 * Created by ZJJ
 */

interface SendReplyCallBack {
    fun onReply(isSuccess: Boolean, sendObject: SendObject, e: Throwable?)
}
