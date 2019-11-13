package com.zj.im.net.tasks

import java.io.IOException
import java.lang.NullPointerException
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.Callable

/**
 * Created by ZJJ
 */

class ConnectionTask(private val socket: Socket?, private val address: String?, private val port: Int?, private val timeOut: Int) : Callable<Throwable> {

    override fun call(): Throwable? {
        return try {
            if (socket == null || address.isNullOrEmpty() || port ?: 0 <= 0) {
                NullPointerException("${socket?.toString() ?: address.toString()} is null object")
            } else {
                socket.connect(InetSocketAddress(address, port ?: 0), timeOut)
                null
            }
        } catch (e: IOException) {
            e
        }
    }
}
