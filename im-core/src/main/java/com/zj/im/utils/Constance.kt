package com.zj.im.utils

import com.zj.im.BuildConfig

internal object Constance {

    const val PING_TIMEOUT = "the socket would reconnection because the ping was no response too many times!"

    const val CONNECTING_INTERRUPTED = "the socket would reconnection because the connected was overridden but it not call resume!"

    const val AUTH_INTERRUPTED = "the socket would reconnection because the authentication was overridden but it not call resume!"

    const val AUTH_TIMEOUT = "auth time out!"

    const val CONNECT_ERROR = "tcp connection error , unable to connect to server!"

    const val LOG_FILE_NAME_EMPTY_ERROR = "must set a log path with open the log collectors!"

    const val FOLDER_NAME = "${BuildConfig.APPLICATION_ID}_IM"

    const val MAX_RETAIN_TCP_LOG = 5 * 24 * 60 * 60 * 1000L

    internal var DEFAULT_TIMEOUT = 10000L
}
