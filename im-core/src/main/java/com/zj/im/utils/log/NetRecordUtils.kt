package com.zj.im.utils.log

import com.zj.im.BuildConfig
import com.zj.im.utils.full
import com.zj.im.utils.log.logger.DataUtils
import com.zj.im.utils.log.logger.LogCollectionUtils
import java.util.concurrent.locks.ReentrantReadWriteLock

/**
 * IM TCP status collector
 * */
@Suppress("unused")
internal object NetRecordUtils : LogCollectionUtils.Config() {

    override fun overriddenFolderName(folderName: String): String {
        return "$folderName/UsageSituation"
    }

    private val path: String; get() = ""
    val name: String; get() = "situations"

    override val subPath: () -> String
        get() = { path }
    override val fileName: () -> String
        get() = { name }
    override val debugEnable: () -> Boolean
        get() = { BuildConfig.DEBUG }

    private val changedListeners = mutableMapOf<String, TCPNetRecordChangedListener>()

    @JvmStatic
    fun addRecordListener(name: String, tc: TCPNetRecordChangedListener) {
        changedListeners[name] = tc
    }

    @JvmStatic
    fun removeRecordListener(name: String) {
        changedListeners.remove(name)
    }

    private var accessAble = false

    private val rwl = ReentrantReadWriteLock()
    private val r = rwl.readLock()
    private val w = rwl.writeLock()
    private var netWorkRecordInfo: NetWorkRecordInfo? = null
        get() {
            if (!accessAble) return null
            if (field == null) {
                field = getNetRecordInfo() ?: NetWorkRecordInfo()
            }
            return field
        }

    override fun prepare() {
        accessAble = true
    }

    @JvmStatic
    fun recordDisconnectCount() {
        if (accessAble) {
            val disconnectCount = (netWorkRecordInfo?.disconnectCount ?: 0) + 1
            netWorkRecordInfo?.disconnectCount = disconnectCount
            record(netWorkRecordInfo)
        }
    }

    @JvmStatic
    fun recordLastModifySendData(lastModifySendData: Long) {
        if (accessAble) {
            netWorkRecordInfo?.apply {
                this.lastModifySendData = lastModifySendData
                this.receivedSize += lastModifySendData
                this.total = sentSize + receivedSize
                record(this)
            }
        }
    }

    @JvmStatic
    fun recordLastModifyReceiveData(lastModifyReceiveData: Long) {
        if (accessAble) {
            netWorkRecordInfo?.apply {
                this.lastModifyReceiveData = lastModifyReceiveData
                this.sentSize += lastModifyReceiveData
                this.total = sentSize + receivedSize
                record(this)
            }
        }
    }

    @JvmStatic
    fun recordSendCount() {
        if (accessAble) {
            val sentCount = (netWorkRecordInfo?.sentCount ?: 0) + 1
            netWorkRecordInfo?.sentCount = sentCount
            record(netWorkRecordInfo)
        }
    }

    @JvmStatic
    fun recordReceivedCount() {
        if (accessAble) {
            val receivedCount = (netWorkRecordInfo?.receivedCount ?: 0) + 1
            netWorkRecordInfo?.receivedCount = receivedCount
            record(netWorkRecordInfo)
        }
    }

    @JvmStatic
    fun getRecordInfo(): NetWorkRecordInfo? {
        return if (!accessAble) null else netWorkRecordInfo
    }

    private fun record(info: NetWorkRecordInfo?) {
        if (info == null) return
        if (info != netWorkRecordInfo) netWorkRecordInfo = info
        info.lastModifyTime = full()
        val recordString = DataUtils.toString(info)
        changedListeners.forEach { (_, v) ->
            v.onChanged(info)
        }
        write(recordString, false)
    }

    private fun getNetRecordInfo(): NetWorkRecordInfo? {
        return try {
            r.lock()
            val logFile = getLogFile(path, name)
            DataUtils.toModule(getLogText(logFile))
        } finally {
            r.unlock()
        }
    }
}
