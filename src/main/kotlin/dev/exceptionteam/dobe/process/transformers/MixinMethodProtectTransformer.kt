package dev.exceptionteam.dobe.process.transformers

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import dev.exceptionteam.dobe.config.value
import dev.exceptionteam.dobe.process.Transformer
import dev.exceptionteam.dobe.process.resource.NameGenerator
import dev.exceptionteam.dobe.process.resource.ResourceCache
import dev.exceptionteam.dobe.utils.*
import dev.exceptionteam.dobe.utils.logging.Logger
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.*
import java.nio.charset.StandardCharsets

object MixinMethodProtectTransformer : Transformer("MixinMethodProtect") {

    private val dictionaryClass by value("GenerateClassNameDictionary", "Alphabet")
    private val dictionaryMethod by value("GenerateMethodNameDictionary", "Alphabet")
    private val parent by value("Parent", "dobe/obf/mixin")
    private val mixinFile by value("MixinFile", "mixins.example.json")

    override fun ResourceCache.transform() {
        Logger.info(" - Redirecting mixin method")

        val count = count {
            val classNameGenerator = NameGenerator.getByName(dictionaryClass)
            nonExcluded.asSequence()
                .filter { it.isMixinClass() && !it.isInterface }
                .forEach { classNode ->
                    val canObf = classNode.methods.asSequence()
                        .filter { !it.isAbstract && !it.isNative }
                        .filter {  methodNode ->
                            !methodNode.instructions.any {
                                it is InvokeDynamicInsnNode ||
                                        it is FieldInsnNode && it.owner == classNode.name ||
                                        it is MethodInsnNode && it.owner == classNode.name ||
                                        it is VarInsnNode && !methodNode.isStatic && it.`var` == 0
                            }
                        }

                    if (canObf.count() != 0) {
                        val methodNameGenerator = NameGenerator.getByName(dictionaryMethod)
                        val className = "${parent.removeSuffix("/")}/Mixin_${classNameGenerator.nextName()}"
                        canObf.forEach { methodNode ->
                            val genClazz = classes.getOrPut(className) {
                                ClassNode().apply {
                                    visit(Opcodes.V17, Opcodes.ACC_PUBLIC, className, null, "java/lang/Object", null)
                                    shouldReCalcFrame.add(this.name)
                                }
                            }

                            val static = methodNode.isStatic

                            val genMethod = MethodNode(Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC,
                                methodNameGenerator.nextName(),
                                methodNode.desc,
                                methodNode.signature,
                                methodNode.exceptions.toTypedArray()
                            ).apply {
                                tryCatchBlocks.addAll(methodNode.tryCatchBlocks)
                                instructions.add(methodNode.instructions)
                                if (!static) {
                                    instructions.filterIsInstance<VarInsnNode>().forEach {
                                        it.`var`--
                                    }
                                }
                                methodNode.tryCatchBlocks.clear()
                                methodNode.instructions.clear()

                                genClazz.methods.add(this)
                            }

                            methodNode.visitCode()
                            var stack = if (static) 0 else 1
                            Type.getArgumentTypes(methodNode.desc).forEach {
                                methodNode.visitVarInsn(it.getLoadType(), stack)
                                stack += it.size
                            }
                            methodNode.visitMethodInsn(Opcodes.INVOKESTATIC, genClazz.name, genMethod.name, genMethod.desc, false)
                            methodNode.visitInsn(methodNode.desc.getReturnType())
                            methodNode.visitEnd()
                            add()
                        }
                    }
                }
        }.get()

        val newMixinFile = resources[mixinFile]?.let { bytes ->
            val mainObject = JsonObject()
            Gson().fromJson(
                String(bytes, StandardCharsets.UTF_8),
                JsonObject::class.java
            ).apply {
                asMap().forEach { (name, value) ->
                    when (name) {
                        "package" -> {
                            mainObject.addProperty(
                                "package",
                                value.asString.replace(".", "/")
                            )
                        }
                        else -> mainObject.add(name, value)
                    }
                }
            }
            GsonBuilder().setPrettyPrinting().create().toJson(mainObject).toByteArray(Charsets.UTF_8)
        }
        if (newMixinFile != null) resources[mixinFile] = newMixinFile

        Logger.info("    Redirected $count mixin  method")

    }
}