package com.zj.im.net.socket

import com.zj.im.chat.exceptions.ExceptionHandler
import com.zj.im.chat.core.DataStore
import com.zj.im.chat.interfaces.RunningObserver
import com.zj.im.chat.interfaces.BaseMsgInfo
import com.zj.im.main.ChatBase
import com.zj.im.net.helper.TcpMessageUtility
import java.io.DataInputStream
import java.io.IOException
import java.io.InputStream
import java.net.Socket
import java.nio.ByteBuffer

/**
 * Created by ZJJ
 */

internal object ReadSocketThread : RunningObserver() {

    var getSocket: (() -> Socket?)? = null

    override fun run(runningKey: String) {
        if (ChatBase.isFinishing(runningKey)) return
        try {
            while (true) {
                val socket = getSocket?.invoke() ?: return
                if (socket.isClosed) return
                val `is` = socket.getInputStream()
                if (`is`.available() <= 0) return
                parseByte(`is`)
                try {
                    Thread.sleep(16)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }
        } catch (e: IOException) {
            ExceptionHandler.postError(e)
        } catch (e: OutOfMemoryError) {
            ExceptionHandler.postError("the large data over than max memory size, case : ${e.message}")
        } catch (e: Exception) {
            ExceptionHandler.postError(e)
        }
    }

    private fun parseByte(`is`: InputStream) {
        val header = ByteArray(4)
        val inputStream = DataInputStream(`is`)
        inputStream.readFully(header)
        val anInt = ByteBuffer.wrap(header).int
        val bufferBody = ByteArray(anInt)
        val dis = DataInputStream(`is`)
        dis.readFully(bufferBody)
        val unpackMap = TcpMessageUtility.unpackMsg(bufferBody)
        putReceivedDataFormServer(unpackMap)
    }

    private fun putReceivedDataFormServer(data: Map<String, Any>?) {
        DataStore.put(BaseMsgInfo.receiveMsg(data))
    }
}
