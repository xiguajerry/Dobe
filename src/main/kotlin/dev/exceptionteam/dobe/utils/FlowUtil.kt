package dev.exceptionteam.dobe.utils

import org.objectweb.asm.tree.*
import java.io.IOError
import java.io.IOException
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Modifier

val HANDLER_NAMES = arrayOf(
    RuntimeException::class.java,
    LinkageError::class.java,
    Error::class.java,
    Exception::class.java,
    Throwable::class.java,
    IllegalMonitorStateException::class.java,
    IllegalArgumentException::class.java,
    IllegalStateException::class.java,
    IllegalAccessError::class.java,
    InvocationTargetException::class.java,
    IOException::class.java,
    IOError::class.java,
    BootstrapMethodError::class.java,
    NoClassDefFoundError::class.java,
    EnumConstantNotPresentException::class.java,
    NegativeArraySizeException::class.java,
    UnsupportedOperationException::class.java,
    IndexOutOfBoundsException::class.java,
    ArrayIndexOutOfBoundsException::class.java,
    StringIndexOutOfBoundsException::class.java,
    ArithmeticException::class.java,
    ArrayStoreException::class.java,
    SecurityException::class.java
).map { it.name.replace(".", "/") }.toTypedArray()

fun addTryCatch(methodNode: MethodNode, string: String, string2: String?) {
    if (methodNode.name.startsWith("<") || Modifier.isAbstract(methodNode.access)) {
        return
    }
    if (methodNode.instructions.size() > 10) {
        val labelNode = LabelNode()
        val labelNode2 = LabelNode()
        val labelNode3 = LabelNode()
        methodNode.instructions.insert(labelNode)
        methodNode.instructions.add(labelNode3)
        methodNode.instructions.add(labelNode2)
        methodNode.instructions.add(InsnNode(89))
        methodNode.instructions.add(JumpInsnNode(198, labelNode2))
        if (nextBoolean() && string2 != null) {
            methodNode.instructions.add(
                MethodInsnNode(
                    184,
                    string2,
                    dynamicString,
                    "(Ljava/lang/Throwable;)Ljava/lang/Throwable;",
                    false
                )
            )
        }
        methodNode.instructions.add(InsnNode(191))
        val tryCatchBlockNode = TryCatchBlockNode(labelNode, labelNode3, labelNode2, string)
        methodNode.tryCatchBlocks.add(tryCatchBlockNode)
    }
}