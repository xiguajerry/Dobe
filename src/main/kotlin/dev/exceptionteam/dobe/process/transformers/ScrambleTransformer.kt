package dev.exceptionteam.dobe.process.transformers

import dev.exceptionteam.dobe.config.value
import dev.exceptionteam.dobe.process.Transformer
import dev.exceptionteam.dobe.process.resource.NameGenerator
import dev.exceptionteam.dobe.process.resource.ResourceCache
import dev.exceptionteam.dobe.utils.*
import dev.exceptionteam.dobe.utils.logging.Logger
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.*
import java.util.concurrent.CopyOnWriteArrayList

object ScrambleTransformer : Transformer("ScrambleTransformer") {

    private val intensity by value("Intensity", 1)
    private val randomName by value("RandomName", false)
    private val redirectGetStatic by value("RedirectGetStatic", true)
    private val redirectSetStatic by value("RedirectSetStatic", true)
    private val redirectGetField by value("RedirectGetValue", true)
    private val redirectSetField by value("RedirectSetField", true)

    private val generateOuterClass by value("GenerateOuterClass", false)
    private val excludedClasses by value("ExcludedClasses", listOf())
    private val excludedFieldName by value("ExcludedFieldName", listOf())

    private val downCalls by value("NativeDownCalls", true)
    private val upCalls by value("NativeUpCalls", false)
    private val mixinSupport by value("MixinSupport", false)
    private val nativeAnnotationCount = Counter()
    val appendedMethods = mutableListOf<MethodNode>()
    val mixinWhiteList = CopyOnWriteArrayList<String>()

    override fun ResourceCache.transform() {
        Logger.info(" - Redirecting field calls")
        val newClasses = mutableMapOf<ClassNode, ClassNode>() // Owner Companion
        var count = 0
        repeat(intensity) {
            count += process(newClasses)
        }
        Logger.info("    Redirected $count field calls")
    }

    private fun ResourceCache.process(newClasses: MutableMap<ClassNode, ClassNode>): Int {
        val nameGenerator = NameGenerator.getByEnum(NameGenerator.Dictionary.Space)
        val count = count {
            nonExcluded.asSequence()
                .filter { it.name.isNotExcludedIn(excludedClasses) }
                .forEach { classNode ->
                    classNode.methods.toList().asSequence()
                        .filter { it.name != "<init>" && it.name != "<clinit>" }
                        .forEach methodLoop@{ methodNode ->
                            //Mixin Obfuscate
                            if (mixinSupport && mixinFound(classNode) && !classNode.isInterface && methodNode.visibleAnnotations != null && methodNode.visibleAnnotations.isNotEmpty() && methodNode.visibleAnnotations.any {
                                    it.desc.contains(
                                        ("Ldev/zenhao/melon/mixins/MixinObfuscate")
                                    )
                                }) {
                                Logger.info("Method Supported: ${methodNode.name}")
                                val genMethodNode = MethodNode(
                                    Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC,
                                    nameGenerator.nextName(),
                                    "()V",
                                    methodNode.signature,
                                    methodNode.exceptions.toTypedArray()
                                ).apply {
                                    visitCode()
                                    tryCatchBlocks.addAll(methodNode.tryCatchBlocks)
                                    instructions.add(methodNode.instructions)
                                    visitMaxs(3, 3)
                                    visitEnd()
                                }
                                val className = "dev/zenhao/melon/extension/${nameGenerator.nextName()}"
                                val clazz = newClasses.getOrPut(classNode) {
                                    ClassNode().apply {
                                        visit(
                                            Opcodes.V17,
                                            Opcodes.ACC_PUBLIC,
                                            className,
                                            null,
                                            "java/lang/Object",
                                            null
                                        )
                                    }
                                }

                                methodNode.instructions.clear()
                                methodNode.tryCatchBlocks.clear()
                                //methodNode.visitFrame(Opcodes.F_FULL, genMethodNode.localVariables.size, genMethodNode.localVariables.toTypedArray(), 0, arrayOf<Any>())
//                                var stack = 0
//                                Type.getArgumentTypes(methodNode.desc).forEach {
//                                    methodNode.visitVarInsn(it.getLoadType(), stack)
//                                    stack += it.size
//                                }
//                                genMethodNode.instructions.filterIsInstance<VarInsnNode>().forEach {
//                                    it.`var`--
//                                }
                                methodNode.visitCode()
//                                genMethodNode.instructions.forEach {
//                                    Logger.info(it.toString())
//                                    if (it is FrameNode) {
//                                        Logger.info("==========")
//                                        Logger.info(it.local.toString())
//                                        Logger.info(it.stack.toString())
//                                        Logger.info(it.type.toString())
//                                        Logger.info("==========")
//                                        methodNode.instructions.add(it)
//                                        //methodNode.visitFrame(it.type, it.local.size, it.local.toTypedArray(), it.stack.size, it.stack.toTypedArray())
//                                    }
//                                }
                                methodNode.visitMethodInsn(
                                    Opcodes.INVOKESTATIC,
                                    clazz.name,
                                    genMethodNode.name,
                                    "()V",
                                    false
                                )
                                methodNode.visitInsn(methodNode.desc.getReturnType())
                                methodNode.visitMaxs(3, 3)
                                for (annotationsCount in methodNode.visibleAnnotations.indices) {
                                    if (methodNode.visibleAnnotations[annotationsCount].desc != "Ldev/zenhao/melon/mixins/MixinObfuscate;") continue
                                    methodNode.visibleAnnotations.removeAt(annotationsCount)
                                    break
                                }
                                methodNode.visitEnd()
                                clazz.methods.add(genMethodNode)
                                if (!mixinWhiteList.contains(clazz.name)) {
                                    mixinWhiteList.add(clazz.name)
                                }

                                return@methodLoop
                            }

                            if (!mixinFound(classNode)) {
                                methodNode.instructions.toList().forEach {
                                    if (it is FieldInsnNode && it.name.isNotExcludedIn(excludedFieldName)) {
                                        var shouldOuter = generateOuterClass
                                        val callingField =
                                            classes[it.owner]?.fields?.find { f -> f.name == it.name && f.desc == it.desc }
                                        if (callingField != null) callingField.setPublic() else shouldOuter = false
                                        val genMethod = when {
                                            it.opcode == Opcodes.GETSTATIC && redirectGetStatic ->
                                                genMethod(
                                                    it,
                                                    if (randomName) getRandomString(10)
                                                    else "get_${it.name}${getRandomString(5)}"
                                                ).appendAnnotation(false)

                                            it.opcode == Opcodes.PUTSTATIC && redirectSetStatic ->
                                                genMethod(
                                                    it,
                                                    if (randomName) getRandomString(10)
                                                    else "set_${it.name}${getRandomString(5)}"
                                                ).appendAnnotation(true)

                                            it.opcode == Opcodes.GETFIELD && redirectGetField ->
                                                genMethod(
                                                    it,
                                                    if (randomName) getRandomString(10)
                                                    else "get_${it.name}${getRandomString(5)}"
                                                ).appendAnnotation(false)

                                            it.opcode == Opcodes.PUTFIELD && redirectSetField ->
                                                genMethod(
                                                    it,
                                                    if (randomName) getRandomString(10)
                                                    else "set_${it.name}${getRandomString(5)}"
                                                ).appendAnnotation(true)

                                            else -> throw Exception("Unsupported")
                                        }

                                        if (shouldOuter) {
                                            genMethod.access = Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC
                                            val clazz = newClasses.getOrPut(classNode) {
                                                ClassNode().apply {
                                                    visit(
                                                        Opcodes.V17,
                                                        Opcodes.ACC_PUBLIC,
                                                        "${classNode.name}\$Static",
                                                        null,
                                                        "java/lang/Object",
                                                        null
                                                    )
                                                }
                                            }
                                            methodNode.instructions.set(
                                                it,
                                                MethodInsnNode(
                                                    Opcodes.INVOKESTATIC,
                                                    clazz.name,
                                                    genMethod.name,
                                                    genMethod.desc
                                                )
                                            )
                                            clazz.methods.add(genMethod)
                                        } else {
                                            methodNode.instructions.set(
                                                it,
                                                MethodInsnNode(
                                                    Opcodes.INVOKESTATIC,
                                                    classNode.name,
                                                    genMethod.name,
                                                    genMethod.desc
                                                )
                                            )
                                            classNode.methods.add(genMethod)
                                        }
                                        add()
                                    }
                                }
                            }
                        }
                }
            newClasses.forEach { (_, c) ->
                classes[c.name] = c
            }
        }.get()
        return count
    }

    private fun MethodNode.appendAnnotation(downCall: Boolean): MethodNode {
        if (NativeCandidateTransformer.enabled) {
            if (downCall && downCalls) {
                appendedMethods.add(this)
                visitAnnotation(NativeCandidateTransformer.nativeAnnotation, false)
                nativeAnnotationCount.add()
            } else if (upCalls) {
                appendedMethods.add(this)
                visitAnnotation(NativeCandidateTransformer.nativeAnnotation, false)
                nativeAnnotationCount.add()
            }
        }
        return this
    }

    private fun genMethod(field: FieldInsnNode, methodName: String): MethodNode {
        return when (field.opcode) {
            Opcodes.GETFIELD -> MethodNode(
                Opcodes.ACC_PRIVATE + Opcodes.ACC_STATIC,
                methodName,
                "(L${field.owner};)${field.desc}",
                null,
                null
            ).apply {
                visitCode()
                instructions = InsnList().apply {
                    add(VarInsnNode(Opcodes.ALOAD, 0))
                    add(FieldInsnNode(Opcodes.GETFIELD, field.owner, field.name, field.desc))
                    add(InsnNode(desc.getReturnType()))
                }
                visitEnd()
            }

            Opcodes.PUTFIELD -> MethodNode(
                Opcodes.ACC_PRIVATE + Opcodes.ACC_STATIC,
                methodName,
                "(L${field.owner};${field.desc})V",
                null,
                null,
            ).apply {
                visitCode()
                instructions = InsnList().apply {
                    var stack = 0
                    Type.getArgumentTypes(desc).forEach {
                        add(VarInsnNode(it.getLoadType(), stack))
                        stack += it.size
                    }
                    add(FieldInsnNode(Opcodes.PUTFIELD, field.owner, field.name, field.desc))
                    add(InsnNode(Opcodes.RETURN))
                }
                visitEnd()
            }

            Opcodes.GETSTATIC -> MethodNode(
                Opcodes.ACC_PRIVATE + Opcodes.ACC_STATIC,
                methodName,
                "()${field.desc}",
                null,
                null
            ).apply {
                visitCode()
                instructions = InsnList().apply {
                    add(FieldInsnNode(Opcodes.GETSTATIC, field.owner, field.name, field.desc))
                    add(InsnNode(desc.getReturnType()))
                }
                visitEnd()
            }

            Opcodes.PUTSTATIC -> MethodNode(
                Opcodes.ACC_PRIVATE + Opcodes.ACC_STATIC,
                methodName,
                "(${field.desc})V",
                null,
                null,
            ).apply {
                visitCode()
                instructions = InsnList().apply {
                    var stack = 0
                    Type.getArgumentTypes(desc).forEach {
                        add(VarInsnNode(it.getLoadType(), stack))
                        stack += it.size
                    }
                    add(FieldInsnNode(Opcodes.PUTSTATIC, field.owner, field.name, field.desc))
                    add(InsnNode(Opcodes.RETURN))
                }
                visitEnd()
            }

            else -> throw Exception("Unsupported")
        }
    }

}