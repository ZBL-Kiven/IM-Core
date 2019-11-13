package com.zj.im.listeners

import com.zj.im.persistence.Query
import com.zj.im.utils.cast

/**
 * Created by ZJJ
 */

abstract class BaseReceiveListener<OUT : Any, IN : Any> {

    abstract var canReceive: Boolean

    abstract fun getOutData(data: IN): Any?

    /**
     * notify Subscriber refresh when the data receiving.
     *
     * @param data the data.
     */
    open fun onReceived(data: OUT) {}

    /**
     * the data have sending to subscriber after filter and unique,
     * it may post to onReceived ， override it to made another logic.
     *
     **/
    open fun beforeReceive(data: IN, out: OUT) {
        onReceived(out)
    }

    /**
     * used when it has OnSendBefore callback and onProgress called
     * */
    open fun onProgressChange(percent: Int, callId: String) {}

    /**
     * when the local data called（as the data has been changed）,to notify the Subscriber refresh.
     * */
    open fun getLocalData(done: (result: List<OUT>?) -> Unit) {
        done(null)
    }

    /**
     * is equals to another? when the local data has exists，the default condition was a == b
     *override this method to custom your filter rules.
     * */
    open fun isEquals(a: OUT?, b: OUT?): Boolean {
        return a?.equals(b) ?: false
    }

    open fun localData(data: List<OUT>?, done: () -> Unit) {
        done.invoke()
    }

    open fun filterLocalData(data: OUT?): OUT? {
        return data
    }

    /**
     * filter if wrong classes, wrong query filters and filter data before get.
     * */
    internal fun filter(query: Query<OUT>, data: IN): OUT? {
        val outData = cast<Any, OUT>(getOutData(data))
        return if (!query.getQueryResult(outData)) null else outData
    }

    open fun log(content: String, data: OUT?, local: List<OUT>?) {}
}
