package dev.exceptionteam.dobe.process

import dev.exceptionteam.dobe.config.Configurable
import dev.exceptionteam.dobe.config.value
import dev.exceptionteam.dobe.process.resource.ResourceCache

abstract class Transformer(name: String) : Configurable(name) {
    val enabled by value("Enabled", false)
    abstract fun ResourceCache.transform()
}