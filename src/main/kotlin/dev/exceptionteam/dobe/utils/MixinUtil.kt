package dev.exceptionteam.dobe.utils

import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.FieldNode

fun ClassNode.isMixinClass(): Boolean = this.invisibleAnnotations
    .takeUnless { it.isNullOrEmpty() }
    ?.any {
        it.desc.startsWith("Lorg/spongepowered/asm/mixin/")
    } ?: false

fun MethodNode.isMixinMethod(): Boolean = this.invisibleAnnotations
    .takeUnless { it.isNullOrEmpty() }
    ?.any {
        it.desc.startsWith("Lorg/spongepowered/asm/mixin/")
    } ?: false

fun FieldNode.isMixinField(): Boolean = this.invisibleAnnotations
    .takeUnless { it.isNullOrEmpty() }
    ?.any {
        it.desc.startsWith("Lorg/spongepowered/asm/mixin/")
    } ?: false

fun mixinFound(classNode: ClassNode): Boolean {
    if (classNode.invisibleAnnotations != null) {
        if (classNode.invisibleAnnotations.isNotEmpty()) {
            annotation@ for (annotationNode in classNode.invisibleAnnotations) {
                if (annotationNode.desc.startsWith("Lorg/spongepowered/asm/mixin/Mixin")) {
                    return true
                }
            }
        }
    }
    return false
}

fun isMixinFunc(methodNode: MethodNode): Boolean {
    if (methodNode.visibleAnnotations.isNullOrEmpty()) return false
    for (annotationNode in methodNode.visibleAnnotations) {
        if (annotationNode.desc.startsWith("Lorg/spongepowered/asm/mixin/gen/Accessor") ||
            annotationNode.desc.startsWith("Lorg/spongepowered/asm/mixin/gen/Invoker") ||
            annotationNode.desc.startsWith("Lorg/spongepowered/asm/mixin/Shadow") ||
            annotationNode.desc.startsWith("Lorg/spongepowered/asm/mixin/Overwrite") ||
            annotationNode.desc.startsWith("Ldev/zenhao/melon/mixins/MixinObfuscate") ||
            annotationNode.desc.startsWith("Ljava/lang/Override")
        ) {
            return true
        }
    }
    return false
}