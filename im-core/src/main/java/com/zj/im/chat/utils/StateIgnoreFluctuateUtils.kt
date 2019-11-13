package com.zj.im.chat.utils

import android.net.NetworkInfo
import android.os.Handler
import android.os.Looper
import android.os.Message
import com.zj.im.chat.enums.*
import com.zj.im.utils.cast

/**
 * created by ZJJ
 *
 *  the code ，just invalidate in fluctuate holder, used by:
 *
 *  STATE_SUSPENDED : weak network，overridden by SUCCESS or ERROR state
 *
 *  STATE_CONNECTING :is connecting，overridden by SUCCESS or ERROR state
 *
 *  STATE_SUCCESS ：connected，it'll overridden STATE_NORMAL、STATE_ERROR in current if it not notify the stack top and the code was  STATE_ERROR.and it would overridden by STATE_ERROR
 *
 *  STATE_ERROR ：connect fail，overridden the code STATE_NORMAL,STATE_SUCCESS
 * */

const val IM_STATE_ERROR = 403
const val IM_STATE_SUCCESS = 203
const val IM_STATE_NONE = 503

enum class NetWorkState(val code: Int) {
    NONE(IM_STATE_NONE), CONNECTED(IM_STATE_SUCCESS), DISCONNECTED(IM_STATE_ERROR);
}

internal object StateIgnoreFluctuateUtils {

    private var socketStateObserver: MutableMap<String, Pair<Boolean, ((SocketState) -> Unit)?>>? = null
        get() {
            if (field == null) field = mutableMapOf()
            return field
        }
    private var netWorkStateObserver: MutableMap<String, Pair<Boolean, ((NetWorkState) -> Unit)?>>? = null
        get() {
            if (field == null) field = mutableMapOf()
            return field
        }


    fun registerSocketStateChangeListener(name: String, ignoreFluctuate: Boolean, observer: (SocketState) -> Unit) {
        observer(SocketState.INIT)
        this.socketStateObserver?.put(name, Pair(ignoreFluctuate, observer))
    }

    fun removeSocketStateChangeListener(name: String) {
        this.socketStateObserver?.remove(name)
    }

    fun registerNetWorkStateChangeListener(name: String, ignoreFluctuate: Boolean, observer: (NetWorkState) -> Unit) {
        observer(curNetWorkState)
        this.netWorkStateObserver?.put(name, Pair(ignoreFluctuate, observer))
    }

    fun removeNetWorkStateChangeListener(name: String) {
        this.netWorkStateObserver?.remove(name)
    }

    private var curNetWorkState: NetWorkState = NetWorkState.NONE

    private var curSocketState: SocketState = SocketState.INIT

    /** on network status changed */
    fun sendNetWorkStatus(state: NetworkInfo.State) {
        curNetWorkState = when (state) {

            NetworkInfo.State.DISCONNECTED -> {
                NetWorkState.DISCONNECTED
            }

            NetworkInfo.State.CONNECTED -> {
                NetWorkState.CONNECTED
            }
            else -> {
                NetWorkState.NONE
            }
        }
        netWorkStateObserver?.forEach { (k, v) ->
            if (v.first) {
                OnNetWorkStatusChange(k, curNetWorkState)
            } else {
                v.second?.invoke(curNetWorkState)
            }
        }
    }

    /** on tcp status changed */
    fun sendSocketStatus(state: SocketState) {
        curSocketState = state
        socketStateObserver?.forEach { (t, u) ->
            if (u.first) {
                OnSocketStatusChange(t, state)
            } else {
                u.second?.invoke(state)
            }
        }
    }

    class OnSocketStatusChange(key: String, socketState: SocketState) {
        val value = socketStateObserver?.get(key)?.second

        private fun postOnRunnable(delayTime: Long, state: SocketState) {
            postOn(state.code, delayTime) { value?.invoke(state) }
        }

        init {
            when ((if (curNetWorkState == NetWorkState.DISCONNECTED) SocketState.INIT else socketState).code) {
                STATE_INIT -> {
                    postOnRunnable(0, SocketState.INIT)
                }
                STATE_NORMAL -> {
                    handler.removeMessages(STATE_NORMAL)
                    postOnRunnable(500, socketState)
                }
                STATE_ERROR -> {
                    handler.removeMessages(STATE_NORMAL)
                    handler.removeMessages(STATE_SUCCESS)
                    handler.removeMessages(STATE_CONNECTING)
                    postOnRunnable(1000, socketState)
                }
                STATE_SUCCESS -> {
                    handler.removeMessages(STATE_NORMAL)
                    handler.removeMessages(STATE_ERROR)
                    handler.removeMessages(STATE_CONNECTING)
                    postOnRunnable(0, socketState)
                }
                STATE_CONNECTING -> {
                    handler.removeMessages(STATE_ERROR)
                    handler.removeMessages(STATE_SUCCESS)
                    postOnRunnable(1000, socketState)
                }
            }
        }
    }

    class OnNetWorkStatusChange(key: String, private val netWorkState: NetWorkState) {

        val value = netWorkStateObserver?.get(key)?.second

        private fun postOnRunnable(delayTime: Long) {
            postOn(netWorkState.code, delayTime) { value?.invoke(netWorkState) }
        }

        init {
            when (netWorkState) {
                NetWorkState.NONE -> {
                    handler.removeMessages(IM_STATE_ERROR)
                    handler.removeMessages(IM_STATE_SUCCESS)
                    postOnRunnable(0)
                }
                NetWorkState.DISCONNECTED -> {
                    handler.removeMessages(IM_STATE_SUCCESS)
                    postOnRunnable(3000)
                }
                NetWorkState.CONNECTED -> {
                    handler.removeMessages(IM_STATE_ERROR)
                    postOnRunnable(0)
                }
            }
        }
    }

    private fun postOn(what: Int, delayTime: Long, run: () -> Unit) {
        handler.sendMessageDelayed(Message.obtain()?.apply {
            this.what = what
            this.obj = Runnable(run)
        }, delayTime)
    }

    private val handler = Handler(Looper.getMainLooper()) {
        cast<Any?, Runnable?>(it.obj)?.run()
        return@Handler false
    }

    /**
     * 获取当前Socket连接的状态
     */
    fun getCurSocketState(): SocketState {
        return curSocketState
    }

    /**
     * 获取当前网络状态
     */
    fun getCurNetWorkState(): NetWorkState {
        return curNetWorkState
    }
}
