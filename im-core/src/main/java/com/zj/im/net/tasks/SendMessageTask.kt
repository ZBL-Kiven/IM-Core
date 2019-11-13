package com.zj.im.net.tasks

import com.zj.im.net.helper.TcpMessageUtility
import java.io.IOException
import java.lang.ref.WeakReference
import java.net.Socket
import java.util.concurrent.Callable

/**
 * Created by ZJJ
 */

class SendMessageTask(private val socket: WeakReference<Socket?>, val params: Map<String, Any>?) : Callable<Throwable> {

    override fun call(): Throwable? {
        return try {
            val bytes = TcpMessageUtility.packMap(params)
            val outputStream = socket.get()?.getOutputStream()
            outputStream?.write(bytes)
            outputStream?.flush()
            null
        } catch (e: IOException) {
            e
        }
    }
}
