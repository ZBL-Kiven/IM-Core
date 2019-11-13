package com.zj.im.net.helper

import android.annotation.TargetApi
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.*
import android.os.Build
import android.os.Build.VERSION_CODES.M
import android.os.Build.VERSION_CODES.N
import com.zj.im.main.ChatBase.context

/**
 * Created by ZJJ
 */

object NetWorkStateListener {
    private var stateChange: ((NetworkInfo.State) -> Unit)? = null
    private var connectivityManager: ConnectivityManager? = null
    private var netWorkBrodCast: NetWorkBrodCast? = null

    fun init(stateChange: ((NetworkInfo.State) -> Unit)?) {
        this.stateChange = stateChange
        connectivityManager = context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
        if (connectivityManager == null) {
            stateChange?.invoke(NetworkInfo.State.UNKNOWN)
        }
        val version = Build.VERSION.SDK_INT
        when {
            version < M -> {
                isLowerNChange(context)
            }
            version >= M -> {
                isUpperNChange()
            }
        }
        this.stateChange?.invoke(getCurNetworkStatus())
    }

    @Suppress("DEPRECATION")
    private fun isLowerNChange(context: Context?) {
        netWorkBrodCast = NetWorkBrodCast { connectedChange() }
        context?.registerReceiver(netWorkBrodCast, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
    }


    @TargetApi(N)
    private fun isUpperNChange() {
        connectivityManager?.registerNetworkCallback(NetworkRequest.Builder().build(), netWorkCallBack)
    }

    private val netWorkCallBack = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network?) {
            connectedChange()
        }

        override fun onUnavailable() {
            connectedChange()
        }

        override fun onLost(network: Network?) {
            connectedChange()
        }
    }

    private fun connectedChange() {
        val connectivityManager: ConnectivityManager? = context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
        val networkInfo = connectivityManager?.activeNetworkInfo
        if (networkInfo != null && networkInfo.isConnected) {
            stateChange?.invoke(NetworkInfo.State.CONNECTED)
        } else {
            stateChange?.invoke(NetworkInfo.State.DISCONNECTED)
        }
    }

    fun getCurNetworkStatus(): NetworkInfo.State {
        return if (Build.VERSION.SDK_INT >= M) {
            val networkCapabilities = connectivityManager?.getNetworkCapabilities(connectivityManager?.activeNetwork)
            val hasConnect = networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                    ?: false
            val isValidate = connectivityManager?.activeNetworkInfo?.isConnected ?: false
            if (isValidate || hasConnect) NetworkInfo.State.CONNECTED else NetworkInfo.State.DISCONNECTED
        } else {
            val activeNetwork = connectivityManager?.activeNetworkInfo
            val hasConn = activeNetwork != null && activeNetwork.isConnected
            if (hasConn) NetworkInfo.State.CONNECTED else NetworkInfo.State.DISCONNECTED
        }
    }

    fun shutDown() {
        try {
            if (netWorkBrodCast != null && Build.VERSION.SDK_INT < N) context?.unregisterReceiver(netWorkBrodCast)
            else connectivityManager?.unregisterNetworkCallback(netWorkCallBack)
        } catch (e: Exception) {
        }
    }

    class NetWorkBrodCast(private val connectedChange: () -> Unit) : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            connectedChange.invoke()
        }
    }
}
