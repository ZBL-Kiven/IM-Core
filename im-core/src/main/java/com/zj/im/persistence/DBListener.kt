package com.zj.im.persistence

import com.zj.im.main.ChatBase
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Created by ZJJ
 */

abstract class DBListener<T, SAVER : Saver<*>?> {

    private var receiveExecutors: ExecutorService? = null
    private var postExecutors: ExecutorService? = null

    internal fun postReceivedData(getData: () -> T?, onFinish: () -> Unit) {
        receiveExecutors?.let {
            if (it.isShutdown) return
            it.execute {
                getData.invoke()?.let { data ->
                    onReceived(data)?.run()
                    onFinish()
                } ?: {
                    onFinish()
                }.invoke()
            }
        } ?: onFinish()
    }

    open fun init(): DBListener<T, SAVER> {
        if (receiveExecutors == null || receiveExecutors?.isShutdown == true) receiveExecutors = Executors.newSingleThreadExecutor()
        if (postExecutors == null || postExecutors?.isShutdown == true) postExecutors = Executors.newSingleThreadExecutor()
        return this
    }

    protected abstract fun onReceived(t: T): SAVER?

    fun postTo(t: List<T>) {
        if (postExecutors?.isShutdown == false) postExecutors?.execute {
            t.forEach { ChatBase.options?.onMsgReceive(it) }
        }
    }

    fun close() {
        receiveExecutors?.shutdown()
        postExecutors?.shutdown()
    }
}
