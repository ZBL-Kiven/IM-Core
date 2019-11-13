package com.zj.im.chat.enums

const val STATE_INIT = -1
const val STATE_ERROR = 3
const val STATE_SUCCESS = 2
const val STATE_NORMAL = 1
const val STATE_CONNECTING = 0

/**
 * created by ZJJ
 *
 *@param code 的定义
 *
 *  code STATE_INIT： 初始值，未在可处理的范围内，不被任何状态替换
 *
 *  code STATE_NORMAL : 可被成功或失败直接忽略的状态
 *
 *  code STATE_SUCCESS ： 成功状态的标识，在当前未通知（队列首位）的 code 为 STATE_ERROR 的情况下，覆盖 code STATE_NORMAL、STATE_ERROR，在队列内会被 code STATE_ERROR 覆盖
 *
 *  code STATE_ERROR ： 失败的标识，覆盖 code STATE_NORMAL,STATE_SUCCESS
 *
 * */

enum class SocketState(val code: Int, internal var case: String = "") {

    INIT(STATE_INIT), CONNECTED(STATE_NORMAL), RECONNECTION(STATE_CONNECTING), DISCONNECTED(STATE_ERROR), CONNECTION(STATE_CONNECTING), PING(STATE_SUCCESS), PONG(STATE_SUCCESS), SEND_AUTH(STATE_NORMAL), AUTH_SUCCESS(STATE_SUCCESS), CONNECTED_ERROR(STATE_ERROR), NETWORK_STATE_CHANGE(-1);

    fun isConnected(): Boolean {
        return when (this) {
            PING, PONG, AUTH_SUCCESS -> true
            else -> false
        }
    }
}