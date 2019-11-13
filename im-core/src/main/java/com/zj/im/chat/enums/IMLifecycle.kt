package com.zj.im.chat.enums

/**
 * created by ZJJ
 *
 * the current status in sdk
 **/

class IMLifecycle(val type: LifeType, val what: Int) {
    enum class LifeType { START, PAUSE, RESUME, STOP }
}

