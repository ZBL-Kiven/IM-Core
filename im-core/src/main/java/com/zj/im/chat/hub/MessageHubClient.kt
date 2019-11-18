package com.zj.im.chat.hub

import com.zj.im.chat.utils.netUtils.NetWorkInfo
import android.os.Handler
import android.os.Looper
import com.zj.im.chat.modle.IMLifecycle
import com.zj.im.chat.enums.SendMsgState
import com.zj.im.chat.enums.SocketState
import com.zj.im.chat.exceptions.AuthFailException
import com.zj.im.chat.exceptions.ChatException
import com.zj.im.chat.core.DataStore
import com.zj.im.chat.enums.LifeType
import com.zj.im.chat.utils.MainLooper
import com.zj.im.chat.interfaces.BaseMsgInfo
import com.zj.im.chat.interfaces.IMLifecycleListener
import com.zj.im.chat.interfaces.AnalyzingData
import com.zj.im.chat.modle.AuthBuilder
import com.zj.im.chat.modle.HeartbeatsBuilder
import com.zj.im.chat.modle.SocketConnInfo
import com.zj.im.chat.utils.TimeOutUtils
import com.zj.im.main.ChatBase
import com.zj.im.persistence.DBListener
import com.zj.im.sender.SendObject
import com.zj.im.utils.Constance
import com.zj.im.utils.log.NetRecordUtils
import com.zj.im.utils.log.logger.printInFile
import com.zj.im.utils.nio
import kotlin.math.max

/**
 * Created by ZJJ
 *
 * the bridge of client, override and custom your client hub.
 *
 * it may reconnection if change the system clock to earlier.
 *
 */
abstract class MessageHubClient<T> : BaseMessageHub() {

    private var lifecycleType: IMLifecycle? = null
        set(value) {
            printInFile("on lifecycle changed ---- ", "${value?.type?.name}")
            if (field == value) return
            field = value
            listener?.onLifecycle(field ?: return)
        }

    private var listener: IMLifecycleListener? = null
    open var isAuth = false
    open var isSending = false
    open var isReceiving = false
    open var authentication = false
    open var connection = false
    open var isPause = false
    open var isReconnection = false

    private val heartBeatsTime: Int; get() = heartbeatsBuilder?.heartbeatsTime ?: DEFAULT_HEARTBEATS_TIME
    private val authTime: Long; get() = authBuilder?.authTime ?: DEFAULT_AUTH_TIME

    private val delaySendHandler = Handler(Looper.getMainLooper())

    private var pongTime = 0L
    private var pingTime = 0L
    private var pingHasNotResponseCount = 0

    private var curSocketState: SocketState = SocketState.INIT
        set(value) {
            field = value
            isReconnection = (field == SocketState.CONNECTED_ERROR)
            when (field) {
                SocketState.PONG -> {
                    pingHasNotResponseCount = 0
                    pongTime = System.currentTimeMillis()
                }
                SocketState.PING -> {
                    val curTime = System.currentTimeMillis()
                    val outOfTime = heartBeatsTime * 3f
                    val lastPingTime = curTime - pingTime - heartBeatsTime
                    if (pingTime > 0 && pongTime <= 0) {
                        pingHasNotResponseCount++
                    }
                    if (pingHasNotResponseCount > 3 || (pongTime > 0L && curTime - (pongTime + lastPingTime) > outOfTime)) {
                        pongTime = 0L
                        pingTime = 0L
                        pingHasNotResponseCount = 0
                        setCurState(SocketState.CONNECTED_ERROR, Constance.PING_TIMEOUT)
                    }
                    pingTime = System.currentTimeMillis()
                }
                SocketState.AUTH_SUCCESS -> {
                    pongTime = 0L;isSending = false;authentication =
                        false;TimeOutUtils.remove(AUTH_TIME_OUT_CALL_ID);isAuth = true;startHeartBeats();onPause(
                        OVERRIDE_AUTH_CODE
                    ); onAuthSuccess {
                        onResume(OVERRIDE_AUTH_CODE); if (!it) setCurState(
                        SocketState.CONNECTED_ERROR,
                        Constance.AUTH_INTERRUPTED
                    )
                    }
                }
                SocketState.SEND_AUTH -> isAuth = false
                SocketState.CONNECTED -> {
                    connection = false;onPause(OVERRIDE_CONNECTED_CODE);sendAuth();onConnected {
                        if (!it) setCurState(
                            SocketState.CONNECTED_ERROR,
                            Constance.CONNECTING_INTERRUPTED
                        );onResume(OVERRIDE_CONNECTED_CODE)
                    }
                }
                SocketState.NETWORK_STATE_CHANGE, SocketState.DISCONNECTED, SocketState.CONNECTED_ERROR -> conn(
                    DEFAULT_RECONNECT_TIME
                )
                else -> {
                }
            }
            ChatBase.onSocketStatusChanged(onSocketStateChanged(curSocketState))
            when (value) {
                SocketState.PING -> printInFile(
                    "on socket status change ----- ",
                    "--- $value -- ${nio(pingTime)}"
                )
                SocketState.PONG -> printInFile(
                    "on socket status change ----- ",
                    "--- $value -- ${nio(pongTime)}"
                )
                SocketState.CONNECTED_ERROR -> printInFile(
                    "on socket status change ----- ",
                    "$value  ==> reconnection with error : ${value.case}"
                )
                SocketState.AUTH_SUCCESS -> NetRecordUtils.recordDisconnectCount()
                else -> printInFile("on socket status change ----- ", "$value")
            }
        }
        get() {
            synchronized(field) {
                return field
            }
        }

    /**
     * get heartbeats params , wrong call if null
     */
    protected abstract val heartbeatsBuilder: HeartbeatsBuilder?

    /**
     * get authentication params, skips if it returned null
     */
    protected abstract val authBuilder: AuthBuilder?

    init {
        lifecycleType = IMLifecycle(LifeType.START, -1)
        conn(0)
    }

    protected abstract fun getConnInfo(conn: (Boolean, SocketConnInfo?) -> Unit)

    /**
     * parse the map data，in work thread ，don`t touch the UI
     * */
    protected abstract fun parseData(data: AnalyzingData): T?

    /**
     * @return your custom dataBase handler ,when the data parsed in the work thread
     */
    protected abstract fun onUpdateDataBase(): DBListener<T, *>

    /**
     *override after connected and intercept the nex step with :isContinue
     * */
    private var onConnected: ((isContinue: (Boolean) -> Unit) -> Unit) = { it(true) }

    /**
     * override after auth and intercept the nex step with :isContinue
     * */
    private var onAuthSuccess: ((isContinue: (Boolean) -> Unit) -> Unit) = { it(true) }

    private fun conn(delayTime: Long) {
        isAuth = false; authentication = false;connection = false
        reconnectRunnable?.let {
            delaySendHandler.removeCallbacks(it)
            delaySendHandler.postDelayed(it, delayTime)
        }

        DataStore.put(BaseMsgInfo.closeSocket())
    }

    private fun sendAuth() {
        setCurState(SocketState.SEND_AUTH)
        if (authBuilder == null || authBuilder?.params?.isNullOrEmpty() == true) {
            setCurState(SocketState.AUTH_SUCCESS)
            return
        } else {
            authentication = true
            TimeOutUtils.putASentMessage(
                AUTH_TIME_OUT_CALL_ID,
                hashMapOf(),
                max(DEFAULT_AUTH_TIME, authTime),
                false,
                isIgnoreConnecting = true
            )
            DataStore.put(BaseMsgInfo.auth(AUTH_TIME_OUT_CALL_ID, authBuilder?.params))
        }
    }

    /**
     * the heartbeats #[pongTime] was caught in there
     * */
    internal fun sendReceivedData(response: Map<String, Any>?) {
        pingHasNotResponseCount = 0
        pongTime = System.currentTimeMillis()
        isReceiving = true
        NetRecordUtils.recordReceivedCount()
        parseAndPost(AnalyzingData(response)) { isReceiving = false }
    }

    internal fun setSendingState(state: SendMsgState?, callId: String?, param: Map<String, Any>?, isResend: Boolean) {
        isSending = true
        if (callId == AUTH_TIME_OUT_CALL_ID) {
            setCurState(SocketState.CONNECTED_ERROR, Constance.AUTH_TIMEOUT)
            isSending = false
        } else {
            if (state == SendMsgState.SUCCESS || state == SendMsgState.FAIL) TimeOutUtils.remove(callId)
            parseAndPost(AnalyzingData(state, callId, param, isResend)) { isSending = false }
        }
    }

    private fun parseAndPost(intercept: AnalyzingData, onFinish: () -> Unit) {
        if (!isShutdown) onUpdateDataBase().init().postReceivedData({ parseData(intercept) }, {
            onFinish()
        })
    }

    internal fun onHeartbeatsReceived() {
        setCurState(SocketState.PONG)
    }

    internal fun authStateChange(state: AuthBuilder.AuthStatus?) {
        when (state) {
            AuthBuilder.AuthStatus.SUCCESS -> setCurState(SocketState.AUTH_SUCCESS)
            AuthBuilder.AuthStatus.FAIL -> postError(AuthFailException("Auth Fail , check your auth params with \": ${authBuilder?.params}\" ? "))
        }
    }

    /**
     * send message to socket
     * @param sendObject running in work thread , on message actual send before
     * */
    open fun sendToSocket(sendObject: SendObject) {
        val params = HashMap(sendObject.getParams())
        val callId = sendObject.getCallId()
        val isResend = sendObject.isResend()
        DataStore.put(BaseMsgInfo.sendMsg(sendObject))
        DataStore.put(BaseMsgInfo.sendingStateChange(SendMsgState.SENDING, callId, params, isResend))
    }

    private fun setCurState(socketState: SocketState, case: String = "") {
        socketState.case = case
        DataStore.put(BaseMsgInfo.connectStateChange(socketState, case))
    }

    /**
     * when socket state change , only called by eventHub and the network status changed
     **/
    internal fun changeSocketState(socketState: SocketState) {
        if (curSocketState != SocketState.SEND_AUTH && socketState == SocketState.AUTH_SUCCESS) return
        val pass = when (socketState) {
            SocketState.PING, SocketState.PONG, SocketState.DISCONNECTED, SocketState.CONNECTED_ERROR, SocketState.NETWORK_STATE_CHANGE -> true;else -> false
        }
        if (curSocketState != socketState || pass) {
            MainLooper.post {
                curSocketState = socketState
            }
        }
    }

    internal fun setNetworkState(netWorkState: NetWorkInfo) {
        MainLooper.post {
            ChatBase.options?.onNetWorkStateChanged(netWorkState)
            when (netWorkState) {
                NetWorkInfo.DISCONNECTED, NetWorkInfo.CONNECTED -> {
                    if ((netWorkState == NetWorkInfo.CONNECTED && canConnect() && !curSocketState.isConnected()) || netWorkState == NetWorkInfo.DISCONNECTED)
                        setCurState(SocketState.NETWORK_STATE_CHANGE, "on network changed")
                }
                else -> {
                }
            }
        }
    }

    /**
     * startHeartBeats
     */
    private fun startHeartBeats() {
        if (isAuth) {
            setCurState(SocketState.PING)
            DataStore.put(BaseMsgInfo.heartBeats(heartbeatsBuilder?.params))
        } else {
            nextHeartBeats()
        }
    }

    internal fun nextHeartBeats() {
        heartBeatsRunnable?.let {
            delaySendHandler.removeCallbacks(it)
            delaySendHandler.postDelayed(it, heartBeatsTime * 1L)
        }
    }

    /**
     * override it when socket state changed
     * */
    internal fun onSendingProgress(callId: String, progress: Int) {
        MainLooper.post {
            if (onSendingProgressUpdating(callId, progress)) {
                ChatBase.options?.onSendingProgressUpdate(progress, callId)
            }
        }
    }

    private var heartBeatsRunnable: Runnable? = null
        get() {
            if (field == null) field = Runnable {
                startHeartBeats()
            }
            return field
        }

    private var reconnectRunnable: Runnable? = null
        get() {
            if (field == null) field = Runnable {
                if (canConnect()) {
                    connection = true
                    try {
                        setCurState(if (isReconnection) SocketState.RECONNECTION else SocketState.CONNECTION)
                        getConnInfo { isOk, conn ->
                            if (!isOk || conn == null) {
                                connection = false
                                setCurState(SocketState.CONNECTED_ERROR, "the connection info not supported by null")
                            } else {
                                DataStore.put(BaseMsgInfo.connectToServer(conn))
                            }
                        }
                    } catch (e: Exception) {
                        connection = false
                        setCurState(SocketState.CONNECTED_ERROR, "connection error ! case : ${e.message}")
                    }
                }
            }
            return field
        }

    internal fun filterHeartBeatsOrAuthResponse(data: Map<String, Any>?): Triple<Boolean, Boolean, AuthBuilder.AuthStatus?> {
        return Triple(!isShutdown, isHeartbeatsBuilder(data), authBuilderStatus(data))
    }

    open fun canReceived(): Boolean {
        return !isPause && !isShutdown && !isReceiving && !connection
    }

    open fun canSend(): Boolean {
        return isAuth && !isShutdown && !isSending && !authentication && !connection
    }

    open fun canAuth(): Boolean {
        return !isShutdown && !isAuth && !connection
    }

    open fun canConnect(): Boolean {
        return !isShutdown && !connection
    }

    fun onPause(code: Int) {
        printInFile("on pause called ", "${overrideType(code)} --- onPause")
        isPause = true;lifecycleType =
            IMLifecycle(LifeType.PAUSE, code)
    }

    fun onResume(code: Int) {
        printInFile("on resume called ", "${overrideType(code)} --- onResume")
        isPause = false;lifecycleType =
            IMLifecycle(LifeType.RESUME, code)
    }

    internal fun overrideOnConnected(obj: (isContinue: (Boolean) -> Unit) -> Unit) {
        this.onConnected = obj
    }

    internal fun overrideOnAuthSuccess(obj: (isContinue: (Boolean) -> Unit) -> Unit) {
        this.onAuthSuccess = obj
    }

    internal fun shutDown() {
        onShutDown()
        lifecycleType = IMLifecycle(LifeType.STOP, -1)
        pongTime = 0L
        DataStore.shutDown()
    }

    private fun isHeartbeatsBuilder(response: Map<String, Any>?) =
        heartbeatsBuilder?.onParsedHeartbeatsReceiver?.invoke(response) == true

    private fun authBuilderStatus(response: Map<String, Any>?) = authBuilder?.onParsedAuthReceiver?.invoke(response)

    /**
     * override it when socket state changed
     * */
    open fun onSocketStateChanged(socketState: SocketState): SocketState {
        return socketState
    }

    /**
     * override it when sending progress updating
     * */
    open fun onSendingProgressUpdating(callId: String, progress: Int): Boolean {
        return false
    }


    protected open fun onShutDown() {}

    protected fun postError(e: ChatException) {
        ChatBase.postError(e)
    }

    internal fun setLifecycleListener(listener: IMLifecycleListener) {
        this.listener = listener
    }

    private fun overrideType(code: Int): String {
        return when (code) {
            OVERRIDE_AUTH_CODE -> "OVERRIDE_AUTH"
            OVERRIDE_CONNECTED_CODE -> "OVERRIDE_CONNECTED"
            else -> "UN_KNOW"
        }
    }

    companion object {
        private const val DEFAULT_RECONNECT_TIME = 3000L
        private const val AUTH_TIME_OUT_CALL_ID = "AC02-qd38-Ik94-nMK6-L7yT"
        const val OVERRIDE_AUTH_CODE = 0
        const val OVERRIDE_CONNECTED_CODE = 1
        const val DEFAULT_HEARTBEATS_TIME = 10000
        const val DEFAULT_AUTH_TIME = 10000L
    }
}
