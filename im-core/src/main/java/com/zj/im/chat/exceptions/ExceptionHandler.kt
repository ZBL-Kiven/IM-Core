package com.zj.im.chat.exceptions

import com.zj.im.utils.log.logger.printInFile

/**
 * created by ZJJ
 *
 * chat exception logger
 * */

internal object ExceptionHandler {

    private const val TAG = "IMExceptionHandler ==> "

    fun postError(e: Throwable?) {
        e?.let { printInFile(TAG, it.message) }
    }

    fun postError(case: String) {
        printInFile(TAG, case)
    }
}
