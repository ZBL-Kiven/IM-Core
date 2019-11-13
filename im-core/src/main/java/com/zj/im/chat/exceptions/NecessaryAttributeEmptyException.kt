package com.zj.im.chat.exceptions

/**
 * created by ZJJ
 *
 * it called when the necessary params not exists
 * */

class NecessaryAttributeEmptyException(case: String) : ChatException(case)
