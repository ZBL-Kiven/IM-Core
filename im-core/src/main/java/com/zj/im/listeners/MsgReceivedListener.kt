package com.zj.im.listeners

import android.os.Handler
import android.os.Message
import com.zj.im.persistence.Query
import com.zj.im.utils.cast
import com.zj.im.utils.cusListOf

/**
 * Created by ZJJ
 */

abstract class MsgReceivedListener<OUT : Any, IN : Any>(val query: Query<OUT>) : BaseReceiveListener<OUT, IN>() {

    companion object {
        const val HANDLER_PROGRESS = 0x10
        const val HANDLER_NEW_RECEIVED = 0x11
        const val HANDLER_ASYNC_DATA = 0x22
        const val HANDLER_ASYNC_COMPLETE = 0x33
        const val HANDLER_LOCAL_GOT = 0x44
    }

    private val handler: ReceiveHandler<OUT, IN>

    init {
        handler = ReceiveHandler({ this@MsgReceivedListener }.invoke())
        getLocal()
    }

    private fun getLocal() {
        if (query.needLocal()) {
            sendMsg(HANDLER_ASYNC_DATA)
        } else {
            sendMsg(HANDLER_ASYNC_COMPLETE)
        }
    }

    internal fun refreshLocalData() {
        getLocal()
    }

    internal fun sendTo(data: IN) {
        filter(query, data)?.let { sendMsg(HANDLER_NEW_RECEIVED, data) }
    }

    internal fun onProgress(percent: Int, callId: String) {
        handler.sendMessage(Message.obtain().apply {
            this.what = HANDLER_PROGRESS
            this.arg1 = percent
            this.obj = callId
        })
    }


    private fun sendMsg(what: Int, data: Any? = null) {
        handler.sendMessage(Message.obtain().apply {
            this.what = what
            this.obj = data
        })
    }

    private class ReceiveHandler<OUT : Any, IN : Any>(val base: BaseReceiveListener<OUT, IN>) : Handler() {

        private var isAsyncRun = true
        private val asyncData = cusListOf<Pair<IN, OUT>>()

        override fun handleMessage(msg: Message?) {
            when (msg?.what) {
                HANDLER_ASYNC_DATA -> {
                    isAsyncRun = true
                    base.getLocalData { lst ->
                        sendMessage(Message.obtain().apply {
                            this.what = HANDLER_LOCAL_GOT
                            this.obj = lst
                        })
                    }
                }
                HANDLER_ASYNC_COMPLETE -> {
                    asyncData.forEach {
                        base.beforeReceive(it.first, it.second)
                    }
                    asyncData.clear()
                    isAsyncRun = false
                }
                HANDLER_NEW_RECEIVED -> {
                    val data: IN = cast(msg.obj) ?: return
                    val outData: OUT = cast(base.getOutData(data)) ?: return
                    asyncData.add(Pair(data, outData))
                    if (!isAsyncRun) {
                        sendEmptyMessage(HANDLER_ASYNC_COMPLETE)
                    }
                }
                HANDLER_LOCAL_GOT -> {
                    cast<Any, List<OUT>>(msg.obj)?.let { lst ->
                        val locals = lst.filter { out ->
                            base.filterLocalData(out)?.let { o ->
                                asyncData.getFirst { base.isEquals(it.second, o) }?.let { asyncData.remove(it) };true
                            } ?: false
                        }
                        base.localData(locals) { sendEmptyMessage(HANDLER_ASYNC_COMPLETE) }
                    }
                }

                HANDLER_PROGRESS -> {
                    val callId = cast<Any, String>(msg.obj) ?: return
                    val percent = msg.arg1
                    base.onProgressChange(percent, callId)
                }
            }
        }
    }
}