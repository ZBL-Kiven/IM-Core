package com.zj.im.chat.exceptions

/**
 * created by ZJJ
 *
 * it called when the sending params path not exists
 * */

class ParamPathNotFoundException(case: String) : ChatException(case)
