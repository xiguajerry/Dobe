package dev.exceptionteam.dobe.process.transformers

import dev.exceptionteam.dobe.config.value
import dev.exceptionteam.dobe.process.Transformer
import dev.exceptionteam.dobe.process.resource.NameGenerator
import dev.exceptionteam.dobe.process.resource.ResourceCache
import dev.exceptionteam.dobe.utils.addCrasher
import dev.exceptionteam.dobe.utils.genKey
import dev.exceptionteam.dobe.utils.logging.Logger
import dev.exceptionteam.dobe.utils.nextInt
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode

object TrashClassTransformer : Transformer("TrashClass") {
    private val trashCount by value("TrashCount", 3)
    private val classPath by value("ClassPath", "dev/zenhao/melon/trash/")
    val whiteListClass = ArrayList<ClassNode>()

    override fun ResourceCache.transform() {
        val classPathFix = classPath.plus(
            if (!classPath.endsWith("/")) {
                "/"
            } else {
                ""
            }
        )
        Logger.info(" - Generating FakeClasses By Count(${trashCount})")
        val className = NameGenerator.getByEnum(NameGenerator.Dictionary.Emoji)
        val spaceName = NameGenerator.getByEnum(NameGenerator.Dictionary.Space)
        for (i in 0..trashCount) {
            val clazz = ClassNode().apply {
                visit(
                    114514,
                    Opcodes.ACC_PUBLIC,
                    "$classPathFix${className.nextName()}",
                    null,
                    "java/lang/Object",
                    null
                )
            }
            for (generatedCount in 0..nextInt(1, 10)) {
                val accessCode = when (nextInt(0, 1)) {
                    0 -> Opcodes.ACC_PUBLIC
                    else -> Opcodes.ACC_PRIVATE
                } + Opcodes.ACC_STATIC
                val genField = FieldNode(
                    Opcodes.ACC_PRIVATE + Opcodes.ACC_STATIC,
                    spaceName.nextName(),
                    "Ljava/lang/String;",
                    null,
                    genKey(nextInt(20, 1145))
                )
                val genMethod = MethodNode(
                    accessCode,
                    spaceName.nextName(),
                    "(Ljava/lang/String;)Ljava/lang/String;",
                    null,
                    null
                ).apply {
                    visitCode()
                    visitInsn(Opcodes.ARETURN)
                    visitMaxs(0, 0)
                    visitEnd()
                }
                clazz.methods.add(genMethod)
                addCrasher(clazz, genMethod)
                clazz.fields.add(genField)
                clazz.methods.forEach { methodNode ->
                    methodNode.instructions.forEach {
                        methodNode.instructions.set(
                            it,
                            MethodInsnNode(
                                Opcodes.INVOKESTATIC,
                                clazz.name,
                                genMethod.name,
                                genMethod.desc
                            )
                        )
                    }
                    methodNode.visibleAnnotations = ArrayList()
                    methodNode.visitAnnotation("Ldev/zenhao/melon/NIGGER;", false)
                }
            }
            whiteListClass.add(clazz)
            //classes[classNode.name] = classNode
        }
        Logger.info("FakeClass Count: ${whiteListClass.size}")
        whiteListClass.forEach { clazz ->
            Logger.info(clazz.name)
            classes[clazz.name] = clazz
        }
    }
}