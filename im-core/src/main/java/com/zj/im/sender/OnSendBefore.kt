package com.zj.im.sender

/**
 * Created by ZJJ
 */

interface OnSendBefore {
    fun onSendBefore(onProgressCover: (progress: Int, callId: String) -> Unit, todo: (isContinue: Boolean, callId: String) -> Unit)
}