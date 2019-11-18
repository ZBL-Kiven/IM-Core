package com.zj.im.chat.modle

import com.zj.im.chat.enums.LifeType

/**
 * created by ZJJ
 *
 * the current status in sdk
 *
 * @param type see {@link #LifeType}
 *
 * @param what when the specially lifecycle called as onPause / onResume
 *
 * this parameter lets to parse the kind of description
 *
 **/

data class IMLifecycle(val type: LifeType, val what: Int)

