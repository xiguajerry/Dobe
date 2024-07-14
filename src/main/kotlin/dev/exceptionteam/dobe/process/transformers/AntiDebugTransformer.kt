package dev.exceptionteam.dobe.process.transformers

import dev.exceptionteam.dobe.config.value
import dev.exceptionteam.dobe.process.Transformer
import dev.exceptionteam.dobe.process.resource.ResourceCache
import dev.exceptionteam.dobe.utils.count
import dev.exceptionteam.dobe.utils.logging.Logger
import org.objectweb.asm.tree.LineNumberNode

object AntiDebugTransformer : Transformer("AntiDebug") {

    private val sourceDebug by value("SourceDebug", true)
    private val lineDebug by value("LineDebug", true)
    private val renameSourceDebug by value("RenameSourceDebug", false)
    private val sourceNames by value(
        "SourceNames", listOf(
            "114514.java",
            "1919810.kt",
            "69420.java",
            "你妈死了.kt"
        )
    )

    override fun ResourceCache.transform() {
        Logger.info(" - Removing/Editing debug information...")
        val count = count {
            nonExcluded.forEach { classNode ->
                if (sourceDebug) {
                    if (renameSourceDebug) {
                        classNode.sourceDebug = sourceNames.random()
                        classNode.sourceFile = sourceNames.random()
                    } else {
                        classNode.sourceDebug = null
                        classNode.sourceFile = null
                    }
                    add()
                }
                if (lineDebug) classNode.methods.forEach { methodNode ->
                    methodNode.instructions.toList().forEach { insn ->
                        if (insn is LineNumberNode) {
                            methodNode.instructions.remove(insn)
                            add()
                        }
                    }
                }
            }
        }.get()
        Logger.info("    Removed/Edited $count debug information")
    }

}