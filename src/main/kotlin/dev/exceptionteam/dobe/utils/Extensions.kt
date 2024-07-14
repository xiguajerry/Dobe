package dev.exceptionteam.dobe.utils

import dev.exceptionteam.dobe.config.Configs
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.*
import java.lang.reflect.Modifier

inline val String.shouldRemove
    get() = Configs.Settings.fileRemovePrefix.any { startsWith(it) }
            || Configs.Settings.fileRemoveSuffix.any { endsWith(it) }

inline val String.isExcluded get() = Configs.Settings.exclusions.any { this.startsWith(it) }

inline val MethodNode.isNative get() = Modifier.isNative(access)

inline val MethodNode.isAbstract get() = Modifier.isAbstract(access)

inline val MethodNode.isStatic get() = Modifier.isStatic(access)

inline val ClassNode.isAbstract get() = Modifier.isAbstract(access)

inline val ClassNode.isInterface get() = Modifier.isInterface(access)

inline val ClassNode.isAnnotation get() = access and Opcodes.ACC_ANNOTATION != 0

inline val ClassNode.isEnum get() = access and Opcodes.ACC_ENUM != 0

fun FieldNode.setPublic() {
    if (Modifier.isPublic(access)) return
    if (Modifier.isPrivate(access)) {
        access = access and Opcodes.ACC_PRIVATE.inv()
        access = access or Opcodes.ACC_PUBLIC
    } else if (Modifier.isProtected(access)) {
        access = access and Opcodes.ACC_PROTECTED.inv()
        access = access or Opcodes.ACC_PUBLIC
    } else access = access or Opcodes.ACC_PUBLIC
}


fun String.isNotExcludedIn(list: List<String>): Boolean = !isExcludedIn(list)

fun String.isExcludedIn(list: List<String>): Boolean = list.any { this.startsWith(it) }

fun Int.toInsnNode(): AbstractInsnNode = when (this) {
    in -1..5 -> InsnNode(this + 0x3)
    in Byte.MIN_VALUE..Byte.MAX_VALUE -> IntInsnNode(Opcodes.BIPUSH, this)
    in Short.MIN_VALUE..Short.MAX_VALUE -> IntInsnNode(Opcodes.SIPUSH, this)
    else -> LdcInsnNode(this)
}

fun MethodInsnNode.match(owner: String, name: String, desc: String): Boolean {
    return this.owner == owner && this.name == name && this.desc == desc
}