package dev.exceptionteam.dobe.process.transformers

import dev.exceptionteam.dobe.config.Configs
import dev.exceptionteam.dobe.config.value
import dev.exceptionteam.dobe.process.Transformer
import dev.exceptionteam.dobe.process.hierarchy.FastHierarchy
import dev.exceptionteam.dobe.process.resource.NameGenerator
import dev.exceptionteam.dobe.process.resource.ResourceCache
import dev.exceptionteam.dobe.utils.*
import dev.exceptionteam.dobe.utils.logging.Logger
import dev.exceptionteam.dobe.utils.scope.ConcurrentScope
import kotlinx.coroutines.launch
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import kotlin.system.measureTimeMillis

object MethodRenameTransformer : Transformer("MethodRename") {

    private val dictionary by value("Dictionary", "Alphabet")
    private val heavyOverloads by value("HeavyOverloads", false)
    private val randomKeywordPrefix by value("RandomKeywordPrefix", false)
    private val ignoreInterface by value("IgnoreInterfaces", false)
    private val mixinSupport by value("MixinSupport", false)
    private val prefix by value("Prefix", "")
    private val exclusion by value("Exclusion", listOf())
    private val excludedName by value("ExcludedName", listOf())

    override fun ResourceCache.transform() {
        Logger.info(" - Renaming methods...")
        val hierarchy = FastHierarchy(this)
        Logger.info("    Building hierarchy graph...")
        val buildTime = measureTimeMillis {
            hierarchy.build()
        }
        Logger.info("    Took ${buildTime}ms to build ${hierarchy.size} hierarchies")
        val dic = NameGenerator.getByName(dictionary)
        val mapping = ConcurrentHashMap<String, String>()

        val count = count {
            // Generate names and apply to children
            val tasks = mutableListOf<Runnable>()
            nonExcluded.asSequence()
                .filter { !it.isEnum && !it.isAnnotation && it.name.isNotExcludedIn(exclusion) }
                .forEach { classNode ->
                    val task = Runnable {
                        val info = hierarchy.getHierarchyInfo(classNode)
                        if (!info.missingDependencies) {
                            method@ for (methodNode in classNode.methods) {
                                if (ignoreInterface && classNode.isInterface) continue
                                if (methodNode.visibleAnnotations != null) {
                                    if (mixinSupport && methodNode.visibleAnnotations.isNotEmpty()) {
                                        annotation@ for (annotationNode in methodNode.visibleAnnotations) {
                                            if (annotationNode.desc.startsWith("Lorg/spongepowered/asm/mixin/gen/Accessor") ||
                                                annotationNode.desc.startsWith("Lorg/spongepowered/asm/mixin/gen/Invoker") ||
                                                annotationNode.desc.startsWith("Lorg/spongepowered/asm/mixin/Shadow") ||
                                                annotationNode.desc.startsWith("Lorg/spongepowered/asm/mixin/Overwrite")
                                            ) {
                                                //println("[METHOD Mixin Support] ${annotationNode.desc} ${classNode.name}")
                                                continue@method
                                            }
                                        }
                                    }
                                }
                                if (methodNode.name.startsWith("<")) continue
                                if (methodNode.name == "main") continue
                                if (methodNode.name.isExcludedIn(excludedName)) continue
                                if (hierarchy.isPrimeMethod(classNode, methodNode)) {
                                    val newName = (if (randomKeywordPrefix) "$nextBadKeyword " else "") +
                                            prefix + dic.nextName(heavyOverloads, methodNode.desc)
                                    mapping[combine(classNode.name, methodNode.name, methodNode.desc)] = newName
                                    // Apply to children
                                    info.children.forEach { c ->
                                        if (c is FastHierarchy.HierarchyInfo)
                                            mapping[combine(c.classNode.name, methodNode.name, methodNode.desc)] =
                                                newName
                                    }
                                    add()
                                } else continue
                            }
                        }
                    }
                    if (Configs.Settings.parallel) tasks.add(task) else task.run()
                }
            if (Configs.Settings.parallel) {
                val cdl = CountDownLatch(tasks.size)
                ConcurrentScope.launch {
                    tasks.forEach {
                        it.run()
                        cdl.countDown()
                    }
                }
                cdl.await()
            }
        }.get()

        Logger.info("    Applying remapping for methods...")
        // Remap
        applyRemap("methods", mapping)
        Logger.info(
            "    Renamed $count methods" +
                    if (heavyOverloads) " with ${dic.overloadsCount} overloads in ${dic.actualNameCount} names" else ""
        )
    }

    private fun combine(owner: String, name: String, desc: String) = "$owner.$name$desc"

    private fun FastHierarchy.isPrimeMethod(owner: ClassNode, method: MethodNode): Boolean {
        val ownerInfo = getHierarchyInfo(owner)
        if (ownerInfo.missingDependencies) return false
        return ownerInfo.parents.none { p ->
            if (p is FastHierarchy.HierarchyInfo)
                p.classNode.methods.any { it.name == method.name && it.desc == method.desc }
            else false//Missing dependencies
        }
    }

}