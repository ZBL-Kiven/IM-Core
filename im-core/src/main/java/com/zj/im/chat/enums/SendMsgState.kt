package com.zj.im.chat.enums

/**
 * created by ZJJ
 *
 * the msg sending state
 * */

@Suppress("unused")
enum class SendMsgState(val type: String) {
    NONE("NONE"), SUCCESS("SUCCESS"), FAIL("FAIL"), SENDING("SENDING"), TIME_OUT("TIME_OUT"), ON_SEND_BEFORE_END("ON_SEND_BEFORE_END");

    companion object {
        fun parseStateByType(type: String?): SendMsgState? {
            var state: SendMsgState? = null
            if (!type.isNullOrEmpty())
                values().forEach {
                    if (it.type == type) {
                        state = it
                        return@forEach
                    }
                }
            return state
        }
    }
}