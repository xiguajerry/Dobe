package dev.exceptionteam.dobe.process.transformers

import dev.exceptionteam.dobe.config.value
import dev.exceptionteam.dobe.process.resource.NameGenerator
import dev.exceptionteam.dobe.process.Transformer
import dev.exceptionteam.dobe.process.resource.ResourceCache
import dev.exceptionteam.dobe.utils.count
import dev.exceptionteam.dobe.utils.logging.Logger

object LocalVariableRenameTransformer : Transformer("LocalVariableRename") {

    private val dictionary by value("Dictionary", "Alphabet")
    private val thisRef by value("ThisReference", false)

    override fun ResourceCache.transform() {
        Logger.info(" - Renaming local variables...")
        val count = count {
            nonExcluded.forEach { classNode ->
                for (methodNode in classNode.methods) {
                    val dic = NameGenerator.getByName(dictionary)
                    methodNode.localVariables?.forEach {
                        if (thisRef || it.name != "this") {
                            val newName = dic.nextName()
                            it.name = newName
                            add()
                        }
                    }
                }
            }
        }.get()
        Logger.info("    Renamed $count local variables")
    }

}