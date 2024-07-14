package dev.exceptionteam.dobe.process.transformers

import dev.exceptionteam.dobe.config.value
import dev.exceptionteam.dobe.process.Transformer
import dev.exceptionteam.dobe.process.resource.NameGenerator
import dev.exceptionteam.dobe.process.resource.ResourceCache
import dev.exceptionteam.dobe.utils.count
import dev.exceptionteam.dobe.utils.isExcludedIn
import dev.exceptionteam.dobe.utils.isNotExcludedIn
import dev.exceptionteam.dobe.utils.logging.Logger
import dev.exceptionteam.dobe.utils.nextBadKeyword
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object FieldRenameTransformer : Transformer("FieldRename") {

    private val dictionary by value("Dictionary", "Alphabet")
    private val mixinSupport by value("MixinSupport", false)
    private val randomKeywordPrefix by value("RandomKeywordPrefix", false)
    private val prefix by value("Prefix", "")
    private val exclusion by value("Exclusion", listOf())
    private val excludedName by value("ExcludedName", listOf())

    override fun ResourceCache.transform() {
        Logger.info(" - Renaming fields...")
        val remap = HashMap<String, String>()
        val fields: MutableList<FieldNode> = ArrayList()
        nonExcluded.forEach { fields.addAll(it.fields) }
        fields.shuffle()

        val dictionaries = ConcurrentHashMap<ClassNode?, NameGenerator>()
        val count = count {
            fieldNode@ for (fieldNode in fields) {
                if (fieldNode.name.isExcludedIn(excludedName)) continue
                if (fieldNode.visibleAnnotations != null) {
                    if (mixinSupport && fieldNode.visibleAnnotations.isNotEmpty()) {
                        annotation@ for (annotationNode in fieldNode.visibleAnnotations) {
                            if (annotationNode.desc.startsWith("Lorg/spongepowered/asm/mixin/gen/Accessor") ||
                                annotationNode.desc.startsWith("Lorg/spongepowered/asm/mixin/gen/Invoker") ||
                                annotationNode.desc.startsWith("Lorg/spongepowered/asm/mixin/Shadow") ||
                                annotationNode.desc.startsWith("Lorg/spongepowered/asm/mixin/Overwrite")
                            ) {
                                //println("[FIELD Mixin Support] ${annotationNode.desc} ${fieldNode.name}")
                                continue@fieldNode
                            }
                        }
                    }
                }
                val c = getOwner(fieldNode, classes)
                val dic = dictionaries.getOrPut(c) { NameGenerator.getByName(dictionary) }
                val name = (if (randomKeywordPrefix) "$nextBadKeyword " else "") + prefix + dic.nextName()
                val stack: Stack<ClassNode> = Stack()
                stack.add(c)
                while (stack.size > 0) {
                    val classNode = stack.pop()
                    val key = classNode.name + "." + fieldNode.name
                    if (key.isNotExcludedIn(exclusion)) {
                        remap[key] = name
                    }
                    classes.values.forEach {
                        if (it.superName == classNode.name || it.interfaces.contains(classNode.name))
                            stack.add(it)
                    }
                }
                add()
            }
        }.get()

        Logger.info("    Applying remapping for fields...")
        applyRemap("fields", remap)

        Logger.info("    Renamed $count fields")
    }

    private fun getOwner(f: FieldNode, classNodes: MutableMap<String, ClassNode>): ClassNode? {
        for (c in classNodes.values) {
            if (c.fields.contains(f)) {
                return c
            }
        }
        return null
    }

}