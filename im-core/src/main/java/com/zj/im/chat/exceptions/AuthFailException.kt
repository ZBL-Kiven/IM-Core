package com.zj.im.chat.exceptions

/**
 * created by ZJJ
 *
 * if you definition the auth step may receive an auth error
 * */
internal data class AuthFailException(val case: String) : ChatException(case)
