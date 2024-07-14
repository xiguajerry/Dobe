package dev.exceptionteam.dobe.process.transformers

import dev.exceptionteam.dobe.process.Transformer
import dev.exceptionteam.dobe.process.resource.ResourceCache
import dev.exceptionteam.dobe.utils.*
import dev.exceptionteam.dobe.utils.logging.Logger
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.*
import java.lang.reflect.Modifier
import java.util.*

object ControlFlowTransformer : Transformer("ControlFlow") {
    private val negativeField = FieldNode(
        ACC_PRIVATE + ACC_STATIC + ACC_SYNTHETIC,
        massiveString.substring((Short.MAX_VALUE / 2.51).toInt()), "I", null, null
    )
    private val positiveField = FieldNode(
        ACC_PRIVATE + ACC_STATIC + ACC_SYNTHETIC,
        massiveString.substring((Short.MAX_VALUE / 2.49).toInt()), "I", null, null
    )

    private var conditions = intArrayOf(IF_ICMPLT, IF_ICMPLE, IF_ICMPNE)

    override fun ResourceCache.transform() {
        Logger.info(" - Obfuscating Flow...")
        val throwableClass = createThrowableClass(massiveString)
        nonExcluded.stream()
            .filter { !Modifier.isInterface(it.access) && !mixinFound(it) }
            .forEach { classNode ->
                classNode.methods.stream().filter { methodNode ->
                    (!Modifier.isAbstract(methodNode.access)
                            && !Modifier.isNative(methodNode.access))
                }.forEach { methodNode ->
                    Arrays.stream(methodNode.instructions.toArray()).forEach { insnNode ->
                        if (insnNode is LabelNode && (insnNode.getOpcode() != RETURN || insnNode.getOpcode() != ARETURN)) {
                            methodNode.instructions.insert(
                                insnNode,
                                getRandomConditionList(classNode)
                            )
                            val labelAfter = LabelNode()
                            val labelBefore = LabelNode()
                            val labelFinal = LabelNode()
                            methodNode.instructions.insertBefore(insnNode, labelBefore)
                            methodNode.instructions.insert(insnNode, labelAfter)
                            methodNode.instructions.insert(labelAfter, labelFinal)
                            methodNode.instructions.insert(
                                labelBefore,
                                JumpInsnNode(GOTO, labelAfter)
                            )
                            methodNode.instructions.insert(
                                labelAfter,
                                JumpInsnNode(GOTO, labelFinal)
                            )
                        }
                    }
                    heavyDoubleAthrow(classNode, methodNode, throwableClass)
                }
                val staticInitializer = classNode.getOrCreateClinit()
                val insnList = InsnList()
                val splitable = -nextInt(0, Short.MAX_VALUE.toInt())
                insnList.add(toInsnNode(-splitable xor 50 + nextInt(0, Short.MAX_VALUE.toInt())))
                insnList.add(toInsnNode(splitable))
                insnList.add(InsnNode(IXOR))
                insnList.add(
                    FieldInsnNode(
                        PUTSTATIC,
                        classNode.name,
                        negativeField.name,
                        "I"
                    )
                )
                insnList.add(toInsnNode(splitable xor 50 + nextInt(0, Short.MAX_VALUE.toInt())))
                insnList.add(toInsnNode(splitable))
                insnList.add(InsnNode(IXOR))
                insnList.add(
                    FieldInsnNode(
                        PUTSTATIC,
                        classNode.name,
                        positiveField.name,
                        "I"
                    )
                )
                staticInitializer.instructions.insert(insnList)
                classNode.fields.add(negativeField)
                classNode.fields.add(positiveField)
            }
        classes[throwableClass.name] = throwableClass
    }

    private fun createThrowableClass(className: String): ClassNode {
        val classNode = ClassNode()
        classNode.visit(V17, ACC_PUBLIC + ACC_SUPER, className, null, "java/lang/Throwable", null)
        val serialVersionUID = FieldNode(
            ACC_PRIVATE + ACC_FINAL + ACC_STATIC, "serialVersionUID", "J",
            null, 1L
        )
        classNode.fields.add(serialVersionUID)
        val methodNode = MethodNode(ACC_PUBLIC, "<init>", "(Ljava/lang/String;)V", null, null)
        val firstLabel = Label()
        val secondLabel = Label()
        val thirdLabel = Label()
        methodNode.visitCode()
        methodNode.visitLabel(firstLabel)
        methodNode.visitLineNumber(10, firstLabel)
        methodNode.visitVarInsn(ALOAD, 0)
        methodNode.visitVarInsn(ALOAD, 1)
        methodNode.visitMethodInsn(INVOKESPECIAL, "java/lang/Throwable", "<init>", "(Ljava/lang/String;)V", false)
        methodNode.visitLabel(secondLabel)
        methodNode.visitLineNumber(11, secondLabel)
        methodNode.visitInsn(RETURN)
        methodNode.visitLabel(thirdLabel)
        methodNode.visitLocalVariable("this", "LMain;", null, firstLabel, thirdLabel, 0)
        methodNode.visitLocalVariable("string", "Ljava/lang/String;", null, firstLabel, thirdLabel, 1)
        methodNode.visitMaxs(2, 2)
        methodNode.visitEnd()
        classNode.methods.add(methodNode)
        return classNode
    }

    private fun getRandomConditionList(classNode: ClassNode): InsnList {
        val insnList = InsnList()
        /*
		 * switch (RandomUtil.getRandom(6)) { default: final LabelNode startLabel = new
		 * LabelNode(); insnList.add(new FieldInsnNode(GETSTATIC, classNode.name,
		 * this.negativeField.name, "I")); insnList.add(new FieldInsnNode(GETSTATIC,
		 * classNode.name, this.positiveField.name, "I")); insnList.add(new
		 * JumpInsnNode(IF_ICMPLT, startLabel)); insnList.add(new
		 * InsnNode(ACONST_NULL)); insnList.add(new InsnNode(ATHROW));
		 * insnList.add(startLabel); break; }
		 */
        val startLabel = LabelNode()
        insnList.add(FieldInsnNode(GETSTATIC, classNode.name, negativeField.name, "I"))
        insnList.add(FieldInsnNode(GETSTATIC, classNode.name, positiveField.name, "I"))
        insnList.add(JumpInsnNode(IF_ICMPLT, startLabel))
        insnList.add(InsnNode(ACONST_NULL))
        insnList.add(InsnNode(ATHROW))
        insnList.add(startLabel)
        return insnList
    }

    private fun heavyDoubleAthrow(
        classNode: ClassNode, methodNode: MethodNode,
        throwableClass: ClassNode
    ) {
        val insnlist = InsnList()
        val firstLabel = LabelNode()
        val secondLabel = LabelNode()
        val firstTryCatch = TryCatchBlockNode(
            firstLabel, secondLabel, secondLabel,
            throwableClass.name
        )
        val thirdLabel = LabelNode()
        val secondTryCatch = TryCatchBlockNode(
            secondLabel, thirdLabel, firstLabel,
            throwableClass.name
        )
        insnlist.add(FieldInsnNode(GETSTATIC, classNode.name, negativeField.name, "I"))
        insnlist.add(FieldInsnNode(GETSTATIC, classNode.name, positiveField.name, "I"))
        insnlist.add(JumpInsnNode(conditions[nextInt(0, conditions.size - 1)], thirdLabel))
        insnlist.add(InsnNode(ACONST_NULL))
        insnlist.add(firstLabel)
        insnlist.add(InsnNode(ATHROW))
        insnlist.add(secondLabel)
        insnlist.add(InsnNode(ATHROW))
        insnlist.add(thirdLabel)
        methodNode.instructions.insert(insnlist)
        methodNode.tryCatchBlocks.add(firstTryCatch)
        methodNode.tryCatchBlocks.add(secondTryCatch)
    }
}
