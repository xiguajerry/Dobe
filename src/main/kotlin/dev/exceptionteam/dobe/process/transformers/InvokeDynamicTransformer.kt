package dev.exceptionteam.dobe.process.transformers

import dev.exceptionteam.dobe.config.value
import dev.exceptionteam.dobe.process.Transformer
import dev.exceptionteam.dobe.process.resource.ResourceCache
import dev.exceptionteam.dobe.utils.*
import dev.exceptionteam.dobe.utils.logging.Logger
import org.objectweb.asm.Handle
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.*
import java.lang.invoke.CallSite
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.lang.reflect.Modifier
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

object InvokeDynamicTransformer : Transformer("InvokeDynamic") {
    private val atomicInteger = AtomicInteger()
    private val mixinSupport by value("MixinSupport", false)
    private val onlyScrambledMixin by value("OnlyScrambledMixin", true)
    private val filterNonIndyMethod by value("FilterNonIndyMethod", true)

    override fun ResourceCache.transform() {
        Logger.info(" - Obfuscating InvokeDynamic...")
        nonExcluded.asSequence()
            .filter { classNode ->
                !Modifier.isInterface(classNode.access) && classNode.access and Opcodes.ACC_ENUM == 0 && classNode.version >= Opcodes.V1_7
            }
            .forEach { classNode ->
                if ((mixinSupport && mixinFound(classNode))
                    || (onlyScrambledMixin && ScrambleTransformer.mixinWhiteList.contains(classNode.name))
                    || (!onlyScrambledMixin && !mixinSupport)
                    || classNode.isIndy()
                    || classNode.methods.any { it.isIndy() }
                ) {
                    Logger.info(classNode.name)
                    val bootstrapName = massiveString
                    val decryptName = massiveString
                    val decryptValue = nextInt(50, 32767)
                    if (insertDynamic(classNode, bootstrapName, decryptValue)) {
                        val createDecrypt = createDecrypt(decryptName, decryptValue)
                        //addCrasher(classNode, createDecrypt)
                        classNode.methods.add(createDecrypt)
                        val bootstrap = createBootstrap(classNode.name, bootstrapName, decryptName)
                        //addCrasher(classNode, bootstrap)
                        classNode.methods.add(bootstrap)
                    }
                    classNode.removeIndyAnnotation()
                }
            }
        Logger.info(" - Replaced " + atomicInteger.get() + " calls")
    }

    private fun insertDynamic(classNode: ClassNode, bootstrapName: String, decryptValue: Int): Boolean {
        val atomicBoolean = AtomicBoolean()
        classNode.methods
            .stream()
            .filter { !Modifier.isAbstract(it.access) }
            .filter {
                it.isIndy()
                        || classNode.isIndy()
                        || !filterNonIndyMethod
                        || (mixinSupport && mixinFound(classNode))
                        || (onlyScrambledMixin && ScrambleTransformer.mixinWhiteList.contains(classNode.name))
                        || (!onlyScrambledMixin && !mixinSupport)
            }
            .forEach { methodNode ->
                methodNode.removeIndyAnnotation()
                methodNode.instructions.filter { insnNode ->
                    (insnNode is MethodInsnNode || insnNode is FieldInsnNode) && insnNode.opcode != Opcodes.INVOKESPECIAL
                }
                    .forEach { insnNode ->
                        if (insnNode is MethodInsnNode) {
                            val handle = Handle(
                                if (insnNode.opcode == 183) {
                                    7
                                } else {
                                    6
                                }, classNode.name, bootstrapName,
                                MethodType.methodType(
                                    CallSite::class.java,
                                    MethodHandles.Lookup::class.java,
                                    String::class.java,
                                    MethodType::class.java,
                                    String::class.java,
                                    String::class.java,
                                    String::class.java,
                                    java.lang.Integer::class.java
                                ).toMethodDescriptorString(),
                                false
                            )
                            val invokeDynamicInsnNode = InvokeDynamicInsnNode(
                                massiveString,
                                if (insnNode.opcode == Opcodes.INVOKESTATIC) insnNode.desc else insnNode.desc.replace(
                                    "(",
                                    "(Ljava/lang/Object;"
                                ),
                                handle, encrypt(insnNode.owner.replace("/", "."), decryptValue),
                                encrypt(insnNode.name, decryptValue),
                                encrypt(insnNode.desc, decryptValue),
                                if (insnNode.opcode == Opcodes.INVOKESTATIC) 0 else 1
                            )
                            methodNode.instructions.insertBefore(insnNode, invokeDynamicInsnNode)
                            methodNode.instructions.remove(insnNode)
                            atomicBoolean.set(true)
                            atomicInteger.incrementAndGet()
                        }
                    }
            }
        return atomicBoolean.get()
    }

    private fun createBootstrap(className: String, methodName: String, decryptName: String): MethodNode {
        val methodNode = MethodNode(
            Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC + Opcodes.ACC_SYNTHETIC + Opcodes.ACC_BRIDGE, methodName,
            MethodType.methodType(
                CallSite::class.java,
                MethodHandles.Lookup::class.java,
                String::class.java,
                MethodType::class.java,
                String::class.java,
                String::class.java,
                String::class.java,
                java.lang.Integer::class.java
            ).toMethodDescriptorString(),
            null, null
        )
        methodNode.visitCode()
        val firstLabel = Label()
        val secondLabel = Label()
        val thirthLabel = Label()
        methodNode.visitTryCatchBlock(firstLabel, secondLabel, thirthLabel, "java/lang/Exception")
        val fourthLabel = Label()
        val fifthLabel = Label()
        methodNode.visitTryCatchBlock(fourthLabel, fifthLabel, thirthLabel, "java/lang/Exception")
        methodNode.visitVarInsn(Opcodes.ALOAD, 3)
        methodNode.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/String")
        methodNode.visitVarInsn(Opcodes.ASTORE, 7)
        methodNode.visitVarInsn(Opcodes.ALOAD, 4)
        methodNode.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/String")
        methodNode.visitVarInsn(Opcodes.ASTORE, 8)
        methodNode.visitVarInsn(Opcodes.ALOAD, 5)
        methodNode.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/String")
        methodNode.visitVarInsn(Opcodes.ASTORE, 9)
        methodNode.visitVarInsn(Opcodes.ALOAD, 6)
        methodNode.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Integer")
        methodNode.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false)
        methodNode.visitVarInsn(Opcodes.ISTORE, 10)
        methodNode.visitVarInsn(Opcodes.ALOAD, 9)
        methodNode.visitMethodInsn(
            Opcodes.INVOKESTATIC, className, decryptName,
            "(Ljava/lang/String;)Ljava/lang/String;", false
        )
        methodNode.visitLdcInsn(Type.getType("L$className;"))
        methodNode.visitMethodInsn(
            Opcodes.INVOKEVIRTUAL, "java/lang/Class", "getClassLoader",
            "()Ljava/lang/ClassLoader;", false
        )
        methodNode.visitMethodInsn(
            Opcodes.INVOKESTATIC, "java/lang/invoke/MethodType", "fromMethodDescriptorString",
            "(Ljava/lang/String;Ljava/lang/ClassLoader;)Ljava/lang/invoke/MethodType;", false
        )
        methodNode.visitVarInsn(Opcodes.ASTORE, 11)
        methodNode.visitLabel(firstLabel)
        methodNode.visitVarInsn(Opcodes.ILOAD, 10)
        methodNode.visitInsn(Opcodes.ICONST_1)
        methodNode.visitJumpInsn(Opcodes.IF_ICMPNE, fourthLabel)
        methodNode.visitTypeInsn(Opcodes.NEW, "java/lang/invoke/ConstantCallSite")
        methodNode.visitInsn(Opcodes.DUP)
        methodNode.visitVarInsn(Opcodes.ALOAD, 0)
        methodNode.visitVarInsn(Opcodes.ALOAD, 7)
        methodNode.visitMethodInsn(
            Opcodes.INVOKESTATIC, className, decryptName,
            "(Ljava/lang/String;)Ljava/lang/String;", false
        )
        methodNode.visitMethodInsn(
            Opcodes.INVOKESTATIC, "java/lang/Class", "forName",
            "(Ljava/lang/String;)Ljava/lang/Class;", false
        )
        methodNode.visitVarInsn(Opcodes.ALOAD, 8)
        methodNode.visitMethodInsn(
            Opcodes.INVOKESTATIC, className, decryptName,
            "(Ljava/lang/String;)Ljava/lang/String;", false
        )
        methodNode.visitVarInsn(Opcodes.ALOAD, 11)
        methodNode.visitMethodInsn(
            Opcodes.INVOKEVIRTUAL, "java/lang/invoke/MethodHandles\$Lookup", "findVirtual",
            "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;",
            false
        )
        methodNode.visitVarInsn(Opcodes.ALOAD, 2)
        methodNode.visitMethodInsn(
            Opcodes.INVOKEVIRTUAL, "java/lang/invoke/MethodHandle", "asType",
            "(Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;", false
        )
        methodNode.visitMethodInsn(
            Opcodes.INVOKESPECIAL, "java/lang/invoke/ConstantCallSite", "<init>",
            "(Ljava/lang/invoke/MethodHandle;)V", false
        )
        methodNode.visitLabel(secondLabel)
        methodNode.visitInsn(Opcodes.ARETURN)
        methodNode.visitLabel(fourthLabel)
        methodNode.visitFrame(
            Opcodes.F_FULL, 12, arrayOf<Any>(
                "java/lang/invoke/MethodHandles\$Lookup", "java/lang/String",
                "java/lang/invoke/MethodType", "java/lang/Object", "java/lang/Object", "java/lang/Object",
                "java/lang/Object", "java/lang/String", "java/lang/String", "java/lang/String", Opcodes.INTEGER,
                "java/lang/invoke/MethodType"
            ),
            0, arrayOf<Any>()
        )
        methodNode.visitTypeInsn(Opcodes.NEW, "java/lang/invoke/ConstantCallSite")
        methodNode.visitInsn(Opcodes.DUP)
        methodNode.visitVarInsn(Opcodes.ALOAD, 0)
        methodNode.visitVarInsn(Opcodes.ALOAD, 7)
        methodNode.visitMethodInsn(
            Opcodes.INVOKESTATIC, className, decryptName,
            "(Ljava/lang/String;)Ljava/lang/String;", false
        )
        methodNode.visitMethodInsn(
            Opcodes.INVOKESTATIC, "java/lang/Class", "forName",
            "(Ljava/lang/String;)Ljava/lang/Class;", false
        )
        methodNode.visitVarInsn(Opcodes.ALOAD, 8)
        methodNode.visitMethodInsn(
            Opcodes.INVOKESTATIC, className, decryptName,
            "(Ljava/lang/String;)Ljava/lang/String;", false
        )
        methodNode.visitVarInsn(Opcodes.ALOAD, 11)
        methodNode.visitMethodInsn(
            Opcodes.INVOKEVIRTUAL, "java/lang/invoke/MethodHandles\$Lookup", "findStatic",
            "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;",
            false
        )
        methodNode.visitVarInsn(Opcodes.ALOAD, 2)
        methodNode.visitMethodInsn(
            Opcodes.INVOKEVIRTUAL, "java/lang/invoke/MethodHandle", "asType",
            "(Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;", false
        )
        methodNode.visitMethodInsn(
            Opcodes.INVOKESPECIAL, "java/lang/invoke/ConstantCallSite", "<init>",
            "(Ljava/lang/invoke/MethodHandle;)V", false
        )
        methodNode.visitLabel(fifthLabel)
        methodNode.visitInsn(Opcodes.ARETURN)
        methodNode.visitLabel(thirthLabel)
        methodNode.visitFrame(Opcodes.F_SAME1, 0, null, 1, arrayOf<Any>("java/lang/Exception"))
        methodNode.visitVarInsn(Opcodes.ASTORE, 12)
        methodNode.visitInsn(Opcodes.ACONST_NULL)
        methodNode.visitInsn(Opcodes.ARETURN)
        methodNode.visitMaxs(6, 13)
        methodNode.visitEnd()
        return methodNode
    }

    private fun createDecrypt(methodName: String, decryptValue: Int): MethodNode {
        val methodNode = MethodNode(
            Opcodes.ACC_PRIVATE + Opcodes.ACC_STATIC + Opcodes.ACC_BRIDGE + Opcodes.ACC_SYNTHETIC, methodName,
            "(Ljava/lang/String;)Ljava/lang/String;", null, null
        )
        methodNode.visitCode()
        methodNode.visitTypeInsn(Opcodes.NEW, "java/lang/StringBuilder")
        methodNode.visitInsn(Opcodes.DUP)
        methodNode.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false)
        methodNode.visitVarInsn(Opcodes.ASTORE, 1)
        methodNode.visitInsn(Opcodes.ICONST_0)
        methodNode.visitVarInsn(Opcodes.ISTORE, 2)
        val firstLabel = Label()
        methodNode.visitJumpInsn(Opcodes.GOTO, firstLabel)
        val secondLabel = Label()
        methodNode.visitLabel(secondLabel)
        methodNode.visitFrame(
            Opcodes.F_APPEND, 2, arrayOf<Any>("java/lang/StringBuilder", Opcodes.INTEGER), 0,
            null
        )
        methodNode.visitVarInsn(Opcodes.ALOAD, 1)
        methodNode.visitVarInsn(Opcodes.ALOAD, 0)
        methodNode.visitVarInsn(Opcodes.ILOAD, 2)
        methodNode.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "charAt", "(I)C", false)
        methodNode.visitIntInsn(getOpcodeInsn(decryptValue), decryptValue)
        methodNode.visitInsn(Opcodes.IXOR)
        methodNode.visitInsn(Opcodes.I2C)
        methodNode.visitMethodInsn(
            Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append",
            "(C)Ljava/lang/StringBuilder;", false
        )
        methodNode.visitInsn(Opcodes.POP)
        methodNode.visitIincInsn(2, 1)
        methodNode.visitLabel(firstLabel)
        methodNode.visitFrame(Opcodes.F_SAME, 0, null, 0, null)
        methodNode.visitVarInsn(Opcodes.ILOAD, 2)
        methodNode.visitVarInsn(Opcodes.ALOAD, 0)
        methodNode.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "length", "()I", false)
        methodNode.visitJumpInsn(Opcodes.IF_ICMPLT, secondLabel)
        methodNode.visitVarInsn(Opcodes.ALOAD, 1)
        methodNode.visitMethodInsn(
            Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;",
            false
        )
        methodNode.visitInsn(Opcodes.ARETURN)
        methodNode.visitMaxs(3, 3)
        methodNode.visitEnd()
        return methodNode
    }


    private fun encrypt(string: String, decryptValue: Int): String {
        val stringBuilder = StringBuilder()
        for (element in string) {
            stringBuilder.append((element.code xor decryptValue).toChar())
        }
        return stringBuilder.toString()
    }

}