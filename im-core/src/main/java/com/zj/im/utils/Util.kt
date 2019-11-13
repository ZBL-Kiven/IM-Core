@file:Suppress("unused")

package com.zj.im.utils

import com.zj.im.chat.exceptions.ExceptionHandler
import com.zj.im.chat.exceptions.ParamPathNotFoundException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.ConcurrentModificationException
import kotlin.collections.ArrayList

/**
 * Created by ZJJ
 */

internal val imService: ExecutorService = Executors.newFixedThreadPool(5)

@Suppress("UNCHECKED_CAST")
fun <T, N> cast(t: T?): N? {
    return try {
        t as? N
    } catch (e: java.lang.Exception) {
        null
    } catch (e: Exception) {
        null
    }
}

fun <T : Any> getValidate(vararg params: T?, predicate: (T?) -> Boolean): T? {
    params.forEach {
        if (it != null && predicate(it)) return it
    }
    return null
}

internal fun <R : Any> R?.runSync(block: (R) -> Unit) {
    this?.let {
        synchronized(it) {
            block(it)
        }
    }
}

fun today(time: Long = System.currentTimeMillis()): String {
    val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return format.format(Date(time))
}

fun now(time: Long = System.currentTimeMillis()): String {
    val format = SimpleDateFormat("HH", Locale.getDefault())
    return format.format(Date(time))
}

fun nio(time: Long = System.currentTimeMillis()): String {
    val format = SimpleDateFormat("HH:mm:ss:SSS", Locale.getDefault())
    return format.format(Date(time))
}

fun full(time: Long = System.currentTimeMillis()): String {
    val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS", Locale.getDefault())
    return format.format(Date(time))
}

internal fun <T> cusListOf(): CustomList<T> = CustomList()

/** safe thread list of IM , nullable supported */
@Suppress("MemberVisibilityCanBePrivate")
internal class CustomList<OUT> {

    private val lst: MutableList<OUT> = mutableListOf()

    val count: Int; get() = lst.size

    fun contains(element: OUT?): Boolean? {
        synchronized(lst) {
            return isNotEmpty() && lst.contains(element ?: return false)
        }
    }

    fun contains(predicate: (other: OUT) -> Boolean): Boolean {
        synchronized(lst) {
            return isNotEmpty() && lst.any { other -> predicate(other) }
        }
    }

    fun add(element: OUT?) {
        lst.runSync { lst ->
            element?.let {
                lst.add(element)
            }
        }
    }

    fun asReversed(): MutableList<OUT> {
        return lst.asReversed()
    }

    fun add(index: Int, element: OUT?) {
        if (element != null && index in 0 until lst.lastIndex)
            lst.runSync {
                it.add(index, element)
            }
    }

    fun addOnly(element: OUT?) {
        if (element != null)
            lst.runSync {
                it.clear()
                it.add(element)
            }
    }

    fun addIf(element: OUT?, index: Int = -1, predicate: (`in`: OUT, other: OUT) -> Boolean) {
        lst.runSync { lst ->
            element?.let {
                if (lst.all { other -> predicate(it, other) }) {
                    if (index in 0 until lst.lastIndex) lst.add(index, it)
                    else lst.add(it)
                }
            }
        }
    }

    fun <R : Comparable<R>> sort(selector: (OUT) -> R) {
        lst.sortBy(selector)
    }

    fun addAll(elements: Collection<OUT>?) {
        lst.runSync { lst -> elements?.let { lst.addAll(it) } }
    }

    fun removeIf(element: OUT?, predicate: (`in`: OUT, other: OUT) -> Boolean) {
        lst.runSync { lst ->
            if (isNotEmpty()) element?.let {
                if (isNotEmpty() && lst.contains(element) && lst.any { predicate(element, it) }) {
                    lst.remove(element)
                }
            }
        }
    }

    fun removeIf(predicate: (other: OUT) -> Boolean) {
        lst.runSync { lst ->
            if (!lst.isNullOrEmpty()) {
                val each = lst.iterator()
                while (each.hasNext()) {
                    if (predicate(each.next())) {
                        each.remove()
                    }
                }
            }
        }
    }

    fun remove(element: OUT?) {
        lst.runSync {
            if (isNotEmpty()) it.remove(element)
        }
    }

    fun removeAll(predicate: (OUT) -> Boolean) {
        lst.runSync {
            if (isNotEmpty()) {
                it.removeAll(predicate)
            }
        }
    }

    fun <R : Any> mapTo(asReversed: Boolean = false, transfer: (OUT) -> R): MutableList<R> {
        return synchronized(lst) { (if (asReversed) copyOf().asReversed() else copyOf()).mapNotNullTo(mutableListOf(), transfer) }
    }

    fun copyOf(): MutableList<OUT> {
        return ArrayList(lst)
    }

    fun printList(joinWith: (OUT) -> CharSequence): String {
        synchronized(lst) {
            return lst.asSequence().joinToString(", ", "", "", -1, "...", joinWith)
        }
    }

    fun getFirst(): OUT? {
        synchronized(lst) {
            return lst.firstOrNull()
        }
    }

    fun getFirst(predicate: (other: OUT) -> Boolean): OUT? {
        synchronized(lst) {
            return lst.firstOrNull(predicate)
        }
    }

    fun forEach(block: (OUT) -> Unit) {
        lst.forEach(block)
    }

    fun clear() {
        lst.runSync { it.clear() }
    }

    fun isNotEmpty(): Boolean {
        return !lst.isNullOrEmpty()
    }

    fun isEmpty(): Boolean {
        return lst.isNullOrEmpty()
    }
}

/**
 * mutable layer map , support query sub map
 */
internal fun cusParamsMapOf() = MutableParamsMap()

class MutableParamsMap {

    private val params = ConcurrentHashMap<String, Any>()

    private val subMaps: ConcurrentHashMap<String, MutableParamsMap> = ConcurrentHashMap()

    private var transactionKey = getIncrementKey()
    private var curTransactionKey: String = transactionKey

    private fun checkTransactionCode() {
        if (transactionKey != curTransactionKey) throw ConcurrentModificationException("the change would be execute in one transaction")
    }

    fun put(name: String, obj: Any) {
        checkTransactionCode()
        params[name] = obj
    }

    fun remove(name: String) {
        checkTransactionCode()
        params.remove(name)
    }

    fun putAll(map: Map<String, Any>) {
        checkTransactionCode()
        params.putAll(map)
    }

    fun clear() {
        checkTransactionCode()
        params.clear()
        subMaps.clear()
    }

    fun putSubMap(name: String, map: Map<String, Any>) {
        checkTransactionCode()
        subMaps[name] = cusParamsMapOf().apply {
            putAll(map)
        }
    }

    fun transactionSubParams(names: Array<out String>, block: (MutableParamsMap) -> Unit) {

        fun getNamesMap(param: MutableParamsMap, index: Int = 0): MutableParamsMap? {
            return if (index == names.lastIndex) {
                param.subMaps[names[index]]
            } else {
                val p = param.subMaps[names[index]] ?: throw ParamPathNotFoundException("your params path : ${names.joinToString { "$it -> " }} was unable to continue with step \"names[index]\"")
                getNamesMap(p, index + 1)
            }
        }
        try {
            val sub = getNamesMap(this)
            if (sub != null) {
                sub.curTransactionKey = sub.transactionKey
                block(sub)
                sub.transactionKey = getIncrementKey()
            }
        } catch (e: Exception) {
            ExceptionHandler.postError(e)
        }
    }

    fun get(): Map<String, Any> {
        subMaps.forEach { (n, v) ->
            params[n] = v.get()
        }
        return this.params
    }
}

private var incrementValue: Long = 0
    get() {
        if (field >= 99) field = 0
        return ++field
    }

fun getIncrementKey(): String {
    return "${System.currentTimeMillis()}.${if (incrementValue < 10) "0$incrementValue" else "$incrementValue"}"
}

fun getIncrementNumber(): Double {
    return getIncrementKey().toDouble()
}