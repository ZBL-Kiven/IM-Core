package com.zj.im.utils.log

import com.zj.im.utils.full

data class NetWorkRecordInfo(val startedTs: String = full()) {

    val name: String = "Last 5 days flows record"
    var lastModifyTime: String = full()
    var lastModifySendData: Long = 0L
    var lastModifyReceiveData: Long = 0L
    var disconnectCount: Long = 0L
    var sentSize: Long = 0L
    var receivedSize: Long = 0L
    var sentCount: Long = 0L
    var receivedCount: Long = 0L
    var total: Long = 0L

    @Suppress("unused")
    fun getTcpDataSize(): Long {
        return sentSize + receivedSize
    }
}
