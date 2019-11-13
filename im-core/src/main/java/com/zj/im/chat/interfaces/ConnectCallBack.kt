package com.zj.im.chat.interfaces

/**
 * Created by ZJJ
 */

interface ConnectCallBack {
    fun onConnection(isSuccess: Boolean, throwable: Throwable?)
}
