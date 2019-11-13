@file:Suppress("unused")

package com.zj.im.utils

import android.annotation.SuppressLint
import android.app.Application
import android.os.Looper
import android.widget.Toast

/**
 * Created by ZJJ
 */

internal object ToastUtils {

    private var context: Application? = null

    fun init(context: Application?) {
        this.context = context
    }

    private var mToast: Toast? = null

    @SuppressLint("ShowToast")
    fun show(msg: String) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            if (mToast == null) {
                mToast = Toast.makeText(context, msg, Toast.LENGTH_SHORT)
            } else {
                mToast?.setText(msg)
            }
            mToast?.show()
        }
    }
}
