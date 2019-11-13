package com.zj.im.net.socket

import android.accounts.NetworkErrorException
import android.app.*
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import com.zj.im.chat.exceptions.ExceptionHandler
import com.zj.im.chat.exceptions.NecessaryAttributeEmptyException
import com.zj.im.chat.hub.MessageHubServer
import com.zj.im.chat.interfaces.ConnectCallBack
import com.zj.im.chat.interfaces.HeartBeatsCallBack
import com.zj.im.chat.interfaces.SendReplyCallBack
import com.zj.im.main.ChatBase
import com.zj.im.net.tasks.ConnectionTask
import com.zj.im.net.tasks.SendMessageTask
import com.zj.im.sender.SendObject
import java.lang.NullPointerException
import java.lang.ref.WeakReference
import java.net.Socket
import java.util.concurrent.Executors
import java.util.concurrent.Future

/**
 * Created by ZJJ
 */

open class BaseSocketService : Service() {

    data class SocketBinder(val service: BaseSocketService) : Binder()

    override fun onBind(intent: Intent?): IBinder? {
        return SocketBinder(this@BaseSocketService)
    }

    private var socket: Socket? = null

    //the write thread pool
    private val executorService = Executors.newSingleThreadExecutor()

    protected open var isShutdown = false

    open fun initServer(getClient: () -> MessageHubServer) {

    }

    private val setSocketToReadThread: () -> Socket? = {
        socket ?: { ChatBase.postError(NecessaryAttributeEmptyException("form read thread , the socket was null"));null }.invoke()
    }

    /**
     * request connect to socket
     */
    open fun connect(address: String?, port: Int?, socketTimeOut: Int?, callBack: ConnectCallBack?) {
        closeSocket()
        synchronized(executorService) {
            var throwable: Throwable?
            try {
                socket = Socket()
                val connectFuture = executorService.submit(ConnectionTask(socket, address, port, socketTimeOut ?: 3000))
                throwable = connectFuture.get()
                if (throwable == null && socket != null && socket?.isConnected == true) {
                    ReadSocketThread.getSocket = setSocketToReadThread
                } else
                    throwable = NetworkErrorException("Socket is already closed")
            } catch (e: Exception) {
                throwable = e
            }
            callBack?.onConnection(throwable == null, throwable)
        }
    }

    /**
     * request heartbeats
     *
     * @param params the params
     */
    open fun onHeartBeatsRequest(params: Map<String, Any>?, callBack: HeartBeatsCallBack?) {
        synchronized(executorService) {
            val throwable = send(params)
            callBack?.heartBeats(throwable == null, throwable)
        }
    }

    /**
     * request to send a msg to socket
     *
     * @param sendObject   the params
     * @param callBack the callback with sent
     */
    open fun sendToSocket(sendObject: SendObject, callBack: SendReplyCallBack?) {
        synchronized(this) {
            val throwable = send(sendObject.getParams())
            callBack?.onReply(throwable == null, sendObject, throwable)
        }
    }

    private fun send(params: Map<String, Any>?): Throwable? {
        var throwable: Throwable? = null
        try {
            if (socket == null) throwable = NullPointerException("Socket is null or closed")
            socket?.let {
                if (!it.isClosed) {
                    val result: Future<Throwable>? = executorService.submit(SendMessageTask(WeakReference(socket), params))
                    throwable = result?.get()
                } else
                    throwable = NetworkErrorException("Socket is already closed")
            }
        } catch (e: Exception) {
            throwable = e
        }
        return throwable
    }

    fun closeSocket() {
        try {
            socket?.let {
                if (!it.isClosed) it.close()
            }
        } catch (e: Exception) {
            ExceptionHandler.postError(e)
        }
    }

    fun shutdown() {
        isShutdown = true
        closeSocket()
        try {
            stopForeground(true)
        } catch (e: Exception) {
            ExceptionHandler.postError(e)
        }
        try {
            stopSelf()
        } catch (e: Exception) {
            ExceptionHandler.postError(e)
        }
    }

    override fun onDestroy() {
        closeSocket()
        super.onDestroy()
    }
}