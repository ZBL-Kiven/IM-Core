package com.zj.im.chat.modle

/**
 * Created by ZJJ
 */

data class HeartbeatsBuilder(val params: Map<String, Any>,val heartbeatsTime: Int, val onParsedHeartbeatsReceiver: (Map<String, Any>?) -> Boolean)
