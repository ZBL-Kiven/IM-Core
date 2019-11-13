package com.zj.im.chat.interfaces

import com.zj.im.chat.enums.IMLifecycle

/**
 * Created by ZJJ
 */

interface LifecycleListener {
    fun status(name: String, type: IMLifecycle)
}
