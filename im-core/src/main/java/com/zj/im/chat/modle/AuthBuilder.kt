package com.zj.im.chat.modle

/**
 * Created by ZJJ
 */

data class AuthBuilder(val params: Map<String, Any>, val authTime: Long, val onParsedAuthReceiver: (Map<String, Any>?) -> AuthStatus?) {
    enum class AuthStatus {
        SUCCESS, FAIL
    }
}
