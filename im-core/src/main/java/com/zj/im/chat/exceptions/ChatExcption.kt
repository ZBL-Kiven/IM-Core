package com.zj.im.chat.exceptions

/**
 * created by ZJJ
 *
 * base of chat exception
 * */
abstract class ChatException(case: String?, private val body: Any? = null) : Throwable(case) {

    @Suppress("unused")
    fun getBodyData(): Any? {
        return body
    }
}
