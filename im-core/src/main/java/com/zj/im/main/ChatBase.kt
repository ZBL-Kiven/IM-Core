@file:Suppress("unused")

package com.zj.im.main

import android.app.Application
import com.zj.im.chat.enums.SocketState
import com.zj.im.chat.exceptions.*
import com.zj.im.chat.core.DataStore
import com.zj.im.chat.interfaces.BaseMsgInfo
import com.zj.im.chat.core.BaseOption
import com.zj.im.chat.utils.TimeOutUtils
import com.zj.im.chat.utils.netUtils.IConnectivityManager
import com.zj.im.chat.utils.netUtils.NetWorkInfo
import com.zj.im.listeners.AppHiddenListener
import com.zj.im.listeners.DropRecoverListener
import com.zj.im.sender.SendingPool
import com.zj.im.utils.ToastUtils
import com.zj.im.utils.getIncrementKey
import com.zj.im.utils.log.logger.FileUtils
import com.zj.im.utils.log.logger.FileUtils.Companion.compressToZip
import com.zj.im.utils.log.NetRecordUtils
import com.zj.im.utils.log.logger.logUtils
import com.zj.im.utils.log.logger.printInFile
import java.io.File
import java.lang.IllegalArgumentException

/**
 * Created by ZJJ
 *
 * the internal base hub for SDK
 */

internal object ChatBase {

    var context: Application? = null
    private var isInit = false
    private var onErrorCallBack: ((e: ChatException) -> Unit)? = null
    internal var options: BaseOption<*>? = null
    var isRunningInBackground = false
    var isNetWorkAccess = false
    var isTcpConnected = false
    private var diskPathName: String = ""
    private var curRunningKey: String = ""
    private var runningKey: String = ""

    fun <OUT : Any> init(options: BaseOption<OUT>) {
        if (isInit) {
            printInFile("ChatBase.IM", "the SDK already init")
            options.buildOption.prepare()
            return
        }
        runningKey = getIncrementKey()
        curRunningKey = runningKey
        printInFile("ChatBase.IM", " the SDK init with $runningKey")
        this.context = options.context
        this.options = options
        onErrorCallBack = { options.buildOption.onError(it) }
        initBase(options.logsCollectionAble, options.logsFileName, options.logsMaxRetain, context)
        options.init(runningKey)
        initUtils()
        isInit = true
        options.buildOption.prepare()
    }

    private fun initBase(
        logsCollectionAble: () -> Boolean,
        logsFileName: String,
        logsMaxRetain: Long,
        context: Application?
    ) {
        diskPathName = logsFileName
        logUtils.init(context, logsFileName, logsCollectionAble, logsMaxRetain)
        NetRecordUtils.init(context, logsFileName, logsCollectionAble, logsMaxRetain)
        ToastUtils.init(context)
        TimeOutUtils.init()
    }

    private fun initUtils() {
        IConnectivityManager.init(context) {
            netWorkStateChanged(it)
        }
        DropRecoverListener.init {
            onLayerChanged(false);isInit && (options?.isInterrupt() ?: true)
        }
        AppHiddenListener.init(context) { onLayerChanged(true) }
    }

    fun checkInit(name: String) {
        if (!isInit) {
            DataStore.clear()
            SendingPool.clear()
            printInFile(
                "ChatBase.IM.checkInit",
                " when $name ,the IM SDK is not init by IMInterface.class or extends class?"
            )
        }
    }

    fun postError(e: ChatException) {
        when (e) {
            is LooperInterruptedException -> {
                if (!isFinishing(curRunningKey)) options?.initMsgHandler(curRunningKey)
                else printInFile(
                    "ChatBase.IM.LooperInterrupted",
                    " the MsgLooper was stopped by SDK shutDown"
                )
            }
            is AuthFailException -> {
                correctConnectionState(SocketState.CONNECTED_ERROR, e.case)
            }
            is NecessaryAttributeEmptyException -> {
                if (!isFinishing(curRunningKey)) {
                    ExceptionHandler.postError(e)
                    onErrorCallBack?.invoke(e)
                }
            }
            else -> {
                ExceptionHandler.postError(e)
                checkInit("on postError")
                onErrorCallBack?.invoke(e)
            }
        }
    }

    fun deleteFormQueue(callId: String?) {
        DataStore.deleteFormQueue(callId)
        SendingPool.deleteFormQueue(callId)
    }

    fun queryInQueue(callId: String?): Boolean {
        return !callId.isNullOrEmpty() && DataStore.queryInMsgQueue { it.callId == callId } || SendingPool.queryInSendingQueue { it.getCallId() == callId }
    }

    fun correctConnectionState(state: SocketState, case: String) {
        DataStore.put(BaseMsgInfo.connectStateChange(state, case))
    }

    private fun onLayerChanged(isHidden: Boolean) {
        if (isHidden != isRunningInBackground) {
            netWorkStateChanged(IConnectivityManager.isNetWorkActive)
            options?.onLayerChanged(isHidden)
        }
    }

    fun onSocketStatusChanged(socketState: SocketState) {
        this.isTcpConnected = socketState.isConnected()
        options?.onSocketConnStateChange(socketState)
    }

    private fun netWorkStateChanged(state: NetWorkInfo) {
        isNetWorkAccess = state == NetWorkInfo.CONNECTED
        if (!isNetWorkAccess) isTcpConnected = false
        DataStore.put(BaseMsgInfo.networkStateChanged(state))
    }

    fun show(s: String) {
        ToastUtils.show(s)
    }

    fun isFinishing(runningKey: String?): Boolean {
        return runningKey != this.runningKey
    }

    fun shutDown() {
        printInFile("ChatBase.IM", " the SDK has begin shutdown with $runningKey")
        runningKey = ""
        options?.shutDown()
        IConnectivityManager.shutDown(context)
        DropRecoverListener.destroy()
        AppHiddenListener.shutDown(context)
        isInit = false
        printInFile("ChatBase.IM", " the SDK was shutdown")
    }

    fun getLogsFolder(zipFolderName: String, zipFileName: String): String {
        if (zipFolderName.contains(".")) throw IllegalArgumentException("case: zipFolderName error : zip folder name can not contain with '.'")
        if (zipFolderName.contains(diskPathName)) throw IllegalArgumentException("case: zipFolderName error : zip folder can not create in log file")
        val path = FileUtils.getHomePath(diskPathName)
        if (path.isNotEmpty()) {
            val homeFile = File(path)
            if (homeFile.isDirectory) {
                val zipPath = FileUtils.getHomePath(zipFolderName)
                val zipName = "$zipFileName.zip"
                compressToZip(path, zipPath, zipName)
                val zipFile = File(zipPath, zipName)
                if (zipFile.exists() && zipFile.isFile)
                    return zipFile.path
            }
        }
        return ""
    }
}