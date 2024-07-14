package dev.exceptionteam.dobe.utils.extension.asm

import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.*

fun AbstractInsnNode.isNotInstruction(): Boolean = this is LineNumberNode || this is FrameNode || this is LabelNode

fun AbstractInsnNode.isDefective(): Boolean = this.opcode == NOP || this.isNotInstruction()