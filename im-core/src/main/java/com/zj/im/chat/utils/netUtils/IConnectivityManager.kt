package com.zj.im.chat.utils.netUtils

import android.annotation.TargetApi
import android.app.Application
import android.content.Context
import android.content.IntentFilter
import android.net.*
import android.net.NetworkCapabilities.*
import android.os.Build
import com.zj.im.utils.log.logger.printInFile

object IConnectivityManager {

    private var stateChangeListener: ((NetWorkInfo) -> Unit)? = null
    private var connectivityManager: ConnectivityManager? = null
    private var netWorkBrodCast: NetWorkBrodCast? = null

    fun init(context: Application?, l: ((NetWorkInfo) -> Unit)?) {
        this.stateChangeListener = l
        clearRegister(context)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            isLowerNChange(context)
        } else {
            this.connectivityManager = context?.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager?
            connectivityManager?.registerNetworkCallback(request, netCallBack)
        }
    }

    private val request = NetworkRequest.Builder()
        .addCapability(NET_CAPABILITY_INTERNET)
        .build()

    private val netCallBack = object : ConnectivityManager.NetworkCallback() {

        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            stateChangeListener?.invoke(NetWorkInfo.CONNECTED)
        }

        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            val level = networkCapabilities.signalStrength
            printInFile("ConnectivityManager", "the signal changed to $level")
            super.onCapabilitiesChanged(network, networkCapabilities)
        }

        override fun onLost(network: Network) {
            stateChangeListener?.invoke(NetWorkInfo.DISCONNECTED)
            super.onLost(network)
        }

        override fun onUnavailable() {
            stateChangeListener?.invoke(NetWorkInfo.DISCONNECTED)
            super.onUnavailable()
        }
    }

    @Suppress("DEPRECATION")
    private fun isLowerNChange(context: Context?) {
        netWorkBrodCast = NetWorkBrodCast {
            stateChangeListener?.invoke(isNetWorkActive)
        }
        context?.registerReceiver(netWorkBrodCast, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
    }

    val isNetWorkActive: NetWorkInfo
        @TargetApi(Build.VERSION_CODES.M) get() {
            return try {
                if (isNetworkConnected()) NetWorkInfo.CONNECTED else NetWorkInfo.DISCONNECTED
            } catch (e: Exception) {
                e.printStackTrace()
                NetWorkInfo.DISCONNECTED
            }
        }

    private fun isNetworkConnected(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val networkCapabilities: NetworkCapabilities? =
                connectivityManager?.getNetworkCapabilities(connectivityManager?.activeNetwork)
            if (networkCapabilities != null) {
                networkCapabilities.hasCapability(NET_CAPABILITY_INTERNET) && networkCapabilities.hasCapability(
                    NET_CAPABILITY_VALIDATED
                )
            } else false
        } else {
            @Suppress("DEPRECATION") (connectivityManager?.activeNetworkInfo as NetworkInfo).let {
                it.isConnected && it.isAvailable
            }
        }
    }

    private fun clearRegister(context: Application?) {
        connectivityManager?.unregisterNetworkCallback(netCallBack)
        context?.unregisterReceiver(netWorkBrodCast)
    }

    fun shutDown(context: Application?) {
        clearRegister(context)
        stateChangeListener = null
        connectivityManager = null
    }
}