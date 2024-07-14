package dev.exceptionteam.dobe.utils.extension.asm

import org.objectweb.asm.commons.CodeSizeEvaluator
import org.objectweb.asm.tree.MethodNode

private const val METHOD_MAX_BYTES = 65535

/**
 * Calc Method Size.
 * @return Size method.
 */
fun MethodNode.getCodeSize(): Int {
    return CodeSizeEvaluator(null).also { this.accept(it) }.maxSize
}

/**
 * Calc left space can add.
 * @return Left space can add.
 */
fun MethodNode.getLeewaySize(): Int {
    return METHOD_MAX_BYTES - getCodeSize()
}