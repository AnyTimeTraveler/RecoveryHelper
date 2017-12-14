package org.simonscode.recoveryhelper

import java.util.*

object TestUtils {
    val random: Random by lazy { Random() }

    fun getString(length: Int): String {
        val sb = StringBuilder()
        for (n in 0 until length) {
            sb.append((random.nextInt(26) + 97).toChar())
        }
        return sb.toString()
    }
}