package com.zj.im.chat.core

import com.zj.im.chat.exceptions.ChatException
import com.zj.im.chat.hub.MessageHubClient
import com.zj.im.chat.hub.MessageHubServer

/**
 * @property getClient return your custom client for sdk {@see MessageHubClient}
 *
 * @property getServer return your custom server for sdk {@see MessageHubServer}
 *
 * @property onError handler the sdk errors with runtime
 *
 * @property prepare on SDK init prepare
 *
 * @property shutdown it called when SDK was shutdown
 *
 * @property onLayerChanged it called when SDK was changed form foreground / background
 * */

abstract class OnBuildOption<OUT : Any> {

    abstract fun getClient(): MessageHubClient<OUT>

    abstract fun getServer(): MessageHubServer

    abstract fun onError(e: ChatException)

    open fun checkNetWorkIsWorking(): Boolean {
        return true
    }

    open fun prepare() {}

    open fun shutdown() {}

    open fun onLayerChanged(inBackground: Boolean) {}
}
