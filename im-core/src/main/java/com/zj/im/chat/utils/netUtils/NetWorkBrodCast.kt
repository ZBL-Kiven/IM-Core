package com.zj.im.chat.utils.netUtils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NetWorkBrodCast(private val connectedChange: () -> Unit) : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        connectedChange.invoke()
    }
}