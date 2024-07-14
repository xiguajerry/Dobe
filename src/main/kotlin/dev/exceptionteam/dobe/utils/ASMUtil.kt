package dev.exceptionteam.dobe.utils

import dev.exceptionteam.dobe.process.transformers.BadASMTransformer
import org.objectweb.asm.Handle
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.*

fun addCatching(methodNode: MethodNode) {
    for (i in 0 until nextInt(5, 10)) {
        addTryCatch(methodNode, HANDLER_NAMES[nextInt(0, HANDLER_NAMES.size)], "NIGGER")
    }
}

fun addCrasher(classNode: ClassNode, methodNode: MethodNode) {
    if (classNode.version >= 51) {
        methodNode.instructions.insert(getBadInvokeDynamic())
    }
}

fun getBadInvokeDynamic(): InsnList {
    val labelNode = LabelNode()
    val insnList = InsnList()
    val handle = Handle(H_INVOKESTATIC, "java/blyat", massiveString, "(I)V", false)
    val invokeDynamicInsnNode = InvokeDynamicInsnNode("protected_by_dobe", "()V", handle)
    val string = BadASMTransformer.randomTypes[nextInt(0, BadASMTransformer.randomTypes.size)]
    val handle2 = Handle(H_INVOKESTATIC, "java/lang/a", "a", "(IIIIIIIIIIIIIIIIIIIIIIII)Ljava/lang/Throwable;", false)
    val invokeDynamicInsnNode2 = InvokeDynamicInsnNode(
        BadASMTransformer.javaKeywords[nextInt(0, BadASMTransformer.javaKeywords.size)],
        string,
        handle2
    )
    insnList.add(nextInt(1, Int.MAX_VALUE).toInsnNode())
    insnList.add(JumpInsnNode(IFNE, labelNode))
    insnList.add(invokeDynamicInsnNode)
    insnList.add(invokeDynamicInsnNode2)
    if (string == "()J" || string.contains("()D")) {
        insnList.add(InsnNode(POP2))
    } else {
        insnList.add(InsnNode(POP))
    }
    insnList.add(labelNode)
    return insnList
}

fun getOpcodeInsn(value: Int): Int {
    if (value >= -1 && value <= 5) {
        return value + 0x3 /* ICONST_M1 0x2 (-0x1 + 0x3 = 0x2)... */
    } else if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) {
        return BIPUSH
    } else if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) {
        return SIPUSH
    }
    throw RuntimeException("Expected value over -1 and under Short.MAX_VALUE")
}

val Int.asInsnNode get() = toInsnNode(this)

fun toInsnNode(i: Int): AbstractInsnNode = when {
    i >= -1 && i <= 5 -> InsnNode(i + 3)
    i > -128 && i < 127 -> IntInsnNode(BIPUSH, i)
    i >= -32768 && i <= 32767 -> IntInsnNode(SIPUSH, i)
    else -> LdcInsnNode(i)
}

fun ClassNode.getOrCreateClinit(): MethodNode {
    return this.methods.firstOrNull { it.name.equals("<clinit>") } ?: MethodNode(
        ACC_STATIC,
        "<clinit>",
        "()V",
        null,
        null
    ).apply {
        visitCode()
        visitInsn(RETURN)
        visitEnd()
    }.also { this.methods.add(it) }
}

fun hasLabels(methodNode: MethodNode): Boolean = methodNode.instructions.any { it is LabelNode }
