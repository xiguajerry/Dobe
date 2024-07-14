package dev.exceptionteam.dobe.utils

import dev.exceptionteam.dobe.process.resource.NameGenerator
import java.text.DecimalFormat

private val emojiName = NameGenerator.getByEnum(NameGenerator.Dictionary.Emoji)

private val MASSIVE_STRING: String by lazy {
    buildString {
        repeat(Short.MAX_VALUE.toInt() - 1) {
            append(" ")
        }
    }
}

val massiveString: String get() = MASSIVE_STRING

val dynamicString: String get() = emojiName.nextName()

val randomString: String
    get() = java.lang.Long.toHexString(java.lang.Double.doubleToLongBits(Math.random()))

fun getFileSize(size: Long): String {
    val decimalFormat = DecimalFormat("0.00")
    val kilobytes = 1024.0f
    val megabytes = kilobytes * kilobytes
    return if (size < megabytes)
        decimalFormat.format((size / kilobytes).toDouble()) + " KB"
    else
        decimalFormat.format((size / megabytes).toDouble()) + " MB"
}
