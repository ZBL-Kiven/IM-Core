@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package com.zj.im.persistence

import com.zj.im.chat.exceptions.ExceptionHandler
import java.lang.reflect.Field
import kotlin.Comparator
import kotlin.math.max

/**
 * Created by ZJJ
 */

class Query<T : Any> private constructor(private val cls: Class<T>?) {

    companion object {

        const val SORT_COMPARATOR = 1
        const val SORT_MODE = 2
        const val SORT_DEFAULT = 2

        fun <T : Any> from(cls: Class<T>): QueryData<T> {
            return QueryData(Query(cls))
        }

        fun allEvents(): Query<Any> {
            return Query(Any::class.java)
        }
    }

    enum class SortType(val value: Boolean) {
        ASCENDING(true), DESCENDING(false)
    }

    private var comparator: Comparator<T>? = null

    private var sortMode: Sort? = null

    private var needLocal: Boolean = true

    private var maxCount = -1

    private val queryData = arrayListOf<QueryInfo>()

    fun getQueryData(): ArrayList<QueryInfo> {
        return queryData
    }

    fun getQueryClass(): Class<T>? {
        return cls
    }

    fun getComparator(): Comparator<T>? {
        return comparator
    }

    fun getSortMode(): Sort? {
        return sortMode
    }

    fun getMaxCount(): Int {
        return maxCount
    }

    fun needSort(): Int {
        return if (comparator != null) SORT_COMPARATOR else if (sortMode != null) SORT_MODE else SORT_DEFAULT
    }

    fun needLocal(): Boolean {
        return needLocal
    }

    fun and(): QueryData<T> {
        return QueryData(this, ContentionType.And)
    }

    fun or(): QueryData<T> {
        return QueryData(this, ContentionType.Or)
    }

    fun needLessLocal(): Query<T> {
        needLocal = false
        return this
    }

    fun sort(comparator: Comparator<T>): Query<T> {
        this.comparator = comparator
        return this
    }

    fun sort(sortBy: String, sortType: SortType): Query<T> {
        this.sortMode = Sort(sortBy, sortType)
        return this
    }

    fun maxCount(maxCount: Int): Query<T> {
        this.maxCount = max(-1, maxCount)
        return this
    }

    class QueryData<T : Any>(private val query: Query<T>, private val contentionType: ContentionType? = null) {

        fun equalTo(s1: String, s2: Any): Query<T> {
            return record(s1, s2, Condition.Equals)
        }

        fun like(s1: String, s2: String): Query<T> {
            return record(s1, s2, Condition.Like)
        }

        fun all(): Query<T> {
            return query
        }

        private fun record(name: String, value: Any, condition: Condition): Query<T> {
            query.queryData.add(QueryInfo(name, value, condition, contentionType))
            return query
        }
    }

    data class QueryInfo(
        val name: String,
        val value: Any,
        val condition: Condition = Condition.Equals,
        val contentionType: ContentionType? = ContentionType.And
    )

    data class Sort(val by: String, val type: SortType)

    enum class ContentionType {
        And, Or
    }

    enum class Condition {
        Equals, Like
    }

    fun getQueryResult(compareData: T?): Boolean {
        if (getQueryClass() == Any::class.java) return true
        if (compareData == null) return false
        if (this.getQueryClass() != compareData::class.java) return false
        var result = true
        try {
            val queryData = this.getQueryData()
            if (queryData.isNotEmpty())
                queryData.forEach {
                    val declaredField: Field? = this.getQueryClass()?.getDeclaredField(it.name)
                    declaredField?.isAccessible = true
                    val declaredData = declaredField?.get(compareData).toString()
                    val establish = when (it.condition) {
                        Condition.Equals -> declaredData == it.value.toString()
                        Condition.Like -> declaredData.contains(it.value.toString()) || it.value.toString().contains(
                            declaredData
                        )
                    }
                    result = when (it.contentionType) {
                        ContentionType.And -> result && establish
                        ContentionType.Or -> result || establish
                        else -> establish
                    }
                }
            return result
        } catch (e: Exception) {
            ExceptionHandler.postError(e)
            return false
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other == null || other !is Query<*>) return false
        if (getQueryClass()?.name != other.getQueryClass()?.name) return false
        val map = hashMapOf<String, QueryEqualsInfo>()
        queryData.forEach {
            val con = if (it.condition == Condition.Equals) 1 else 0
            val type = if (it.contentionType == ContentionType.And) "+" else "-"
            map[it.name] = QueryEqualsInfo(it.value, con, type)
        }
        other.getQueryData().forEach {
            val con = if (it.condition == Condition.Equals) 1 else 0
            val type = if (it.contentionType == ContentionType.And) "+" else "-"
            if (map.containsKey(it.name)) {
                if (map[it.name]?.value != it.value) return false
            }
            map[it.name] = QueryEqualsInfo(it.value, con, type)
        }
        if (other.getQueryData().size == map.size) {
            map.forEach { (k, v) ->
                val nameN = k.toInt()
                v.acci = "${v.type}$nameN${v.con}".toInt()
            }
        } else return false
        var endQueryAci = 0L
        queryData.forEach {
            endQueryAci += (map[it.name]?.acci ?: 0)
        }
        var endOtherAci = 0L
        other.getQueryData().forEach {
            endOtherAci += (map[it.name]?.acci ?: 0)
        }
        return endQueryAci == endOtherAci
    }

    private data class QueryEqualsInfo(val value: Any, val con: Int, val type: String, var acci: Int = 0)

    override fun hashCode(): Int {
        return super.hashCode() + queryData.size
    }
}
