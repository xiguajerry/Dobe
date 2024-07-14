package dev.exceptionteam.dobe.process.transformers

import dev.exceptionteam.dobe.config.value
import dev.exceptionteam.dobe.process.Transformer
import dev.exceptionteam.dobe.process.resource.NameGenerator
import dev.exceptionteam.dobe.process.resource.ResourceCache
import dev.exceptionteam.dobe.utils.*
import dev.exceptionteam.dobe.utils.logging.Logger
import org.objectweb.asm.Handle
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.*
import kotlin.random.Random

object BadASMTransformer : Transformer("BadASM") {
    private val mixinSupport by value("MixinSupport", false)
    val randomTypes =
        arrayOf("()Ljava/lang/Throwable;", "()I", "()Z", "()B", "()C", "()S", "()F", "()J", "()D")
    val javaKeywords = arrayOf(
        "abstract",
        "assert",
        "boolean",
        "break",
        "byte",
        "case",
        "catch",
        "char",
        "class",
        "const",
        "continue",
        "default",
        "do",
        "double",
        "else",
        "enum",
        "extends",
        "false",
        "final",
        "finally",
        "float",
        "for",
        "goto",
        "if",
        "implements",
        "import",
        "instanceof",
        "int",
        "interface",
        "long",
        "native",
        "new",
        "null",
        "package",
        "private",
        "protected",
        "public",
        "return",
        "short",
        "static",
        "strictfp",
        "super",
        "switch",
        "synchronized",
        "this",
        "throw",
        "throws",
        "transient",
        "true",
        "try",
        "void",
        "volatile",
        "while"
    )

    override fun ResourceCache.transform() {
        Logger.info(" - Transforming BadASM...")
        val confuseName = NameGenerator.getByName("Confuse")
        for (classNode in nonExcluded.filter { shouldTransform(it) && (!mixinSupport || !mixinFound(it)) }) {
            for (methodNode in classNode.methods.filter { !it.isNative }) {
                val labelNode = LabelNode()
                val insnList = InsnList()

                insnList.add(nextInt(1, Int.MAX_VALUE).toInsnNode())
                val jumpInsnArray = intArrayOf(Opcodes.IFEQ, Opcodes.IFNE, Opcodes.IFLT, Opcodes.IFGT, Opcodes.IFLE)
                insnList.add(JumpInsnNode(jumpInsnArray[Random.nextInt(jumpInsnArray.size)], labelNode))

                if (classNode.version >= 51) {
                    val handle = Handle(
                        Opcodes.H_INVOKESTATIC,
                        "java/blyat/" + if (nextBoolean()) "nimasile" else "kannimane",
                        genKey(nextInt(20, 30)),
                        "(I)V",
                        false
                    )

                    val bootstrapMethod = InvokeDynamicInsnNode(
                        "protected_by_Dobe",
                        "()V",
                        handle
                    )

                    val randomWords = arrayOf("I", "Z", "B", "C", "S", "F", "J", "D")
                    var randomString = ""
                    for (i in 0..nextInt(15, 20)) {
                        randomString += randomWords[nextInt(0, randomWords.size)]
                    }

                    val handle2 = Handle(
                        Opcodes.H_INVOKESTATIC,
                        "java/lang/${confuseName.nextName()}",
                        confuseName.nextName(),
                        "($randomString)Ljava/lang/Throwable;",
                        false
                    )

                    val randomType = randomTypes[nextInt(0, randomTypes.size)]

                    val javaKeyword = javaKeywords[nextInt(0, javaKeywords.size)]

                    val bootstrapMethod2 = InvokeDynamicInsnNode(
                        javaKeyword,
                        randomType,
                        handle2
                    )

                    insnList.add(bootstrapMethod)
                    insnList.add(bootstrapMethod2)

                    if (randomType == "()J" || randomType.contains("()D")) {
                        insnList.add(InsnNode(Opcodes.POP2))
                    } else {
                        insnList.add(InsnNode(Opcodes.POP))
                    }
                } else {
                    insnList.add(InsnNode(0))
                    insnList.add(InsnNode(0))
                }

                insnList.add(labelNode)
                methodNode.instructions.insert(insnList)
            }
        }
    }

    private fun shouldTransform(classNode: ClassNode): Boolean {
        return !classNode.isInterface && !classNode.isAbstract && (classNode.access and 16384) == 0
    }
}
