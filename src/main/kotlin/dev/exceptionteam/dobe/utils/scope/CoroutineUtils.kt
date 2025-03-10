package dev.exceptionteam.dobe.utils.scope

import kotlinx.coroutines.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledThreadPoolExecutor
import kotlin.math.max

//Scopes
val defaultScope = CoroutineScope(Dispatchers.Default)

inline fun runAsyncThread(crossinline task: () -> Unit) {
    val service = Executors.newScheduledThreadPool(
        (Runtime.getRuntime().availableProcessors() / 2).coerceAtMost(2).coerceAtLeast(1)
    )
    service.execute { task() }
}

object ConcurrentScope : CoroutineScope by CoroutineScope(concurrentContext) {
    val context = concurrentContext
}

object BackgroundScope : CoroutineScope by CoroutineScope(backgroundContext) {
    val pool = backgroundPool
    val context = backgroundContext
}

//Private Field
private val defaultContext =
    CoroutineName("<Melon> Default") + Dispatchers.Default + CoroutineExceptionHandler { _, _ ->

    }


@OptIn(ExperimentalCoroutinesApi::class)
private val concurrentContext = CoroutineName("<Melon> Concurrent") + Dispatchers.Default.limitedParallelism(
    max(
        Runtime.getRuntime().availableProcessors() / 2,
        1
    )
) + CoroutineExceptionHandler { _, _ ->
}

suspend inline fun delay(timeMillis: Int) {
    delay(timeMillis.toLong())
}

private val backgroundPool =
    ScheduledThreadPoolExecutor(
        Runtime.getRuntime().availableProcessors(),
        CountingThreadFactory("<Melon> Background") {
            isDaemon = true
            priority = 3
        })

private val backgroundContext =
    CoroutineName("<Melon> Background") + backgroundPool.asCoroutineDispatcher() + CoroutineExceptionHandler { _, _ ->

    }

inline val Job?.isActiveOrFalse get() = this?.isActive ?: false