package dev.exceptionteam.dobe.process.transformers

import dev.exceptionteam.dobe.config.value
import dev.exceptionteam.dobe.process.Transformer
import dev.exceptionteam.dobe.process.resource.ResourceCache
import dev.exceptionteam.dobe.utils.count
import dev.exceptionteam.dobe.utils.isInterface
import dev.exceptionteam.dobe.utils.logging.Logger
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.FieldNode
import java.util.Objects

object WatermarkTransformer : Transformer("Watermark") {

    private val marker by value("Watermark Message", "PROTECTED BY EXCEPTIONTEAM")

    override fun ResourceCache.transform() {
        Logger.info(" - Adding watermark fields")
        val count = count {
            nonExcluded.asSequence()
                .filter { !it.isInterface }
                .forEach { classNode ->
                    val name: String
                    val desc: String
                    val value: Any?
                    when((0..2).random()) {
                        0 -> {
                            name = "_$marker _"
                            desc = "Ljava/lang/String;"
                            value = marker
                        }
                        1 -> {
                            name = "_$marker _"
                            desc = "I"
                            value = null
                        }
                        else -> {
                            name = "ExceptionTeam Is Really Good"
                            desc = "Ljava/lang/String;"
                            value = marker
                        }
                    }
                    classNode.visitField(Opcodes.ACC_PRIVATE + Opcodes.ACC_STATIC, name, desc, null, value)
                    add()
                }
        }.get()
        Logger.info("    Added $count watermark fields")
    }

}