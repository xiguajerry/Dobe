package dev.exceptionteam.dobe.process.transformers

import dev.exceptionteam.dobe.config.value
import dev.exceptionteam.dobe.process.Transformer
import dev.exceptionteam.dobe.process.resource.ResourceCache
import dev.exceptionteam.dobe.utils.addCatching
import dev.exceptionteam.dobe.utils.logging.Logger
import dev.exceptionteam.dobe.utils.mixinFound

object TryCatchTransformer : Transformer("TryCatchTransformer") {
    private val mixinSupport by value("MixinSupport", true)
    override fun ResourceCache.transform() {
        Logger.info(" - Applying Try Catch...")
        nonExcluded.filter { !mixinSupport || !mixinFound(it) }.forEach { classNode ->
            classNode.methods.forEach {
                addCatching(it)
            }
        }
    }
}