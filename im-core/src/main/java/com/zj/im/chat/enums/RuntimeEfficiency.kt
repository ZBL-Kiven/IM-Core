package com.zj.im.chat.enums

/**
 * created by ZJJ
 *
 * @param interval  how long to handle once in queue
 *
 **/

enum class RuntimeEfficiency(val interval: Long) {
    SLEEP(512), LOW(256), MEDIUM(64), HIGH(32), OVERCLOCK(16)
}
