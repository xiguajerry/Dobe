package dev.exceptionteam.dobe.process.transformers

import dev.exceptionteam.dobe.config.value
import dev.exceptionteam.dobe.process.Transformer
import dev.exceptionteam.dobe.process.resource.ResourceCache
import dev.exceptionteam.dobe.utils.count
import dev.exceptionteam.dobe.utils.logging.Logger

object ShuffleMembersTransformer : Transformer("ShuffleMembers") {

    private val methods by value("Methods", true)
    private val fields by value("Fields", true)
    private val annotations by value("Annotations", true)

    override fun ResourceCache.transform() {
        Logger.info(" - Shuffling members...")
        val count = count {
            nonExcluded.forEach { classNode ->
                if (methods) classNode.methods?.let {
                    classNode.methods = it.shuffled()
                    add(it.size)
                }
                if (fields) classNode.fields?.let {
                    classNode.fields = it.shuffled()
                    add(it.size)
                }
                if (annotations) {
                    classNode.visibleAnnotations?.let {
                        classNode.visibleAnnotations = it.shuffled()
                        add(it.size)
                    }
                    classNode.invisibleAnnotations?.let {
                        classNode.invisibleAnnotations = it.shuffled()
                        add(it.size)
                    }
                    classNode.methods?.forEach { methodNode ->
                        methodNode.visibleAnnotations?.let {
                            methodNode.visibleAnnotations = it.shuffled()
                            add(it.size)
                        }
                        methodNode.invisibleAnnotations?.let {
                            methodNode.invisibleAnnotations = it.shuffled()
                            add(it.size)
                        }
                    }
                }
            }
        }.get()
        Logger.info("    Shuffled $count members")
    }

}