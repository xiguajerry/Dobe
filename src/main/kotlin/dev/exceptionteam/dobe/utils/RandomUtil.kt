package dev.exceptionteam.dobe.utils

import kotlin.random.Random

fun randomDelay(minDelay: Int, maxDelay: Int): Long {
    return nextInt(minDelay, maxDelay).toLong()
}

fun randomString(minLength: Int, maxLength: Int): String {
    val data = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789@!_=+^*()$"
    val random = Random
    val length = random.nextInt(maxLength) % (maxLength - minLength + 1) + minLength
    val sb = StringBuilder()
    for (i in 0 until length) {
        val number = random.nextInt(72)
        sb.append(data[number])
    }
    return sb.toString()
}

fun nextBoolean(): Boolean {
    return Random.nextBoolean()
}

fun nextInt(startInclusive: Int, endExclusive: Int): Int {
    return if (endExclusive - startInclusive <= 0) startInclusive else startInclusive + Random.nextInt(
        endExclusive - startInclusive
    )
}

fun nextDouble(startInclusive: Double, endInclusive: Double): Double {
    return if (startInclusive == endInclusive || endInclusive - startInclusive <= 0.0) startInclusive else startInclusive + (endInclusive - startInclusive) * Math.random()
}

fun nextLong(startInclusive: Long, endInclusive: Long): Long {
    return if (endInclusive - startInclusive <= 0L) startInclusive else (startInclusive + (endInclusive - startInclusive) * Math.random()).toLong()
}

fun nextFloat(startInclusive: Float, endInclusive: Float): Float {
    return if (startInclusive == endInclusive || endInclusive - startInclusive <= 0f) startInclusive else (startInclusive + (endInclusive - startInclusive) * Math.random()).toFloat()
}

fun randSensitive(): Int {
    return Random.nextInt(10) + 1
}