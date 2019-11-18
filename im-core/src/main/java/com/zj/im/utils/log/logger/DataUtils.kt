package com.zj.im.utils.log.logger

import java.lang.IllegalArgumentException
import java.lang.reflect.Field

internal object DataUtils {

    const val SPLIT_CHAR = "  "

    inline fun <reified T : Any> toString(info: T?): String {
        val sb = StringBuilder()
        try {
            val fields: Array<Field>? = T::class.java.declaredFields
            fields?.forEach { it ->
                it.isAccessible = true
                val name = it.name
                val value = it.get(info)
                sb.append(SPLIT_CHAR).append(name).append(SPLIT_CHAR)
                sb.append(value).append("\n")
            } ?: return ""
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return sb.toString()
    }

    inline fun <reified T : Any> toModule(dataStr: String?): T? {
        if (dataStr.isNullOrEmpty()) return null
        try {
            val result = T::class.java.newInstance()
            dataStr.trim().replace("\n", "").split(SPLIT_CHAR).let { lst ->
                if (lst.size % 2 != 0) return null
                for (i in 0 until lst.size / 2) {
                    val key = lst[i * 2]
                    val value = lst[i * 2 + 1]
                    val field = try {
                        T::class.java.getDeclaredField(key)
                    } catch (e: NoSuchFieldException) {
                        e(T::class.java.name, e.message.toString())
                        null
                    }
                    try {
                        field?.isAccessible = true
                        val setValue: Any = when (field?.type) {
                            Long::class.java -> value.toLong()
                            Int::class.java -> value.toInt()
                            Float::class.java -> value.toFloat()
                            Double::class.java -> value.toDouble()
                            else -> value
                        }
                        field?.set(result, setValue)
                    } catch (e1: IllegalAccessException) {
                        e(T::class.java.name, e1.message.toString())
                    } catch (e2: IllegalArgumentException) {
                        e(T::class.java.name, e2.message.toString())
                    }
                }
                return result
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}