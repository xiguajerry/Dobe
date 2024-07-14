package dev.exceptionteam.dobe.process.transformers

import dev.exceptionteam.dobe.config.value
import dev.exceptionteam.dobe.process.Transformer
import dev.exceptionteam.dobe.process.resource.ResourceCache
import dev.exceptionteam.dobe.utils.logging.Logger
import dev.exceptionteam.dobe.utils.massiveString
import dev.exceptionteam.dobe.utils.mixinFound
import dev.exceptionteam.dobe.utils.nextBoolean
import dev.exceptionteam.dobe.utils.nextInt
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode
import java.lang.reflect.Modifier
import java.util.concurrent.CopyOnWriteArrayList

object MiscellaneousObfuscationTransformer : Transformer("MiscObfuscation") {
    private val randomExceptionsList = ArrayList<ClassNode>()
    private val randomExceptions by value("RandomExceptions", true)
    private val massiveSignature by value("MassiveSignature", true)
    private val massiveSource by value("MassiveSource", true)
    private val pushTransient by value("PushTransient", true)
    private val pushVarArgs by value("PushVarArgs", true)
    private val invalidAnnotation by value("InvalidAnnotation", true)
    private val mixinSupport by value("MixinSupport", false)

    override fun ResourceCache.transform() {
        Logger.info(" - Miscellaneous Transformer...")
        if (randomExceptions) {
            nonExcluded.forEach { classNode ->
                if (Modifier.isPublic(classNode.access) && !classNode.name.contains("$")) {
                    randomExceptionsList.add(classNode)
                }
            }
        }
        nonExcluded.forEach { classNode ->
            if (!Modifier.isInterface(classNode.access)) {
                if (!mixinSupport || !mixinFound(classNode)) {
                    if (pushTransient) {
                        pushTransient(classNode)
                    }
                    if (pushVarArgs) {
                        pushVarargs(classNode)
                    }
                }
            }
            if (randomExceptions) {
                ArrayList(classNode.methods).forEach { methodNode -> randomExceptions(methodNode) }
            }
            if (massiveSignature) {
                massiveSignature(classNode)
            }
            if (massiveSource) {
                massiveSource(classNode)
            }
            if (invalidAnnotation) {
                invalidAnnotations(classNode)
            }
        }
    }

    private fun getSynchornizedFields(classNode: ClassNode): CopyOnWriteArrayList<FieldNode> {
        return CopyOnWriteArrayList(classNode.fields)
    }

    private fun getSynchornizedMethods(classNode: ClassNode): CopyOnWriteArrayList<MethodNode> {
        return CopyOnWriteArrayList(classNode.methods)
    }

    private fun randomExceptions(methodNode: MethodNode) {
        if (nextBoolean()) {
            for (i in 0 until nextInt(0, 3)) {
                methodNode.exceptions.add(randomExceptionsList[nextInt(0, randomExceptionsList.size)].name)
            }
        }
    }

    private fun massiveSignature(classNode: ClassNode) {
        val signature = massiveString
        val allFields = getSynchornizedFields(classNode)
        allFields.forEach { fieldNode ->
            if (mixinSupport && fieldNode.visibleAnnotations != null) {
                annotation@ for (annotationNode in ArrayList(fieldNode.visibleAnnotations)) {
                    if (!annotationNode.desc.startsWith("Lorg/spongepowered/asm/mixin/gen/Accessor") &&
                        !annotationNode.desc.startsWith("Lorg/spongepowered/asm/mixin/gen/Invoker") &&
                        !annotationNode.desc.startsWith("Lorg/spongepowered/asm/mixin/Shadow") &&
                        !annotationNode.desc.startsWith("Lorg/spongepowered/asm/mixin/Overwrite")
                    ) {
                        fieldNode.signature = signature
                    }
                }
            } else {
                fieldNode.signature = signature
            }
        }
        val finalMethod = getSynchornizedMethods(classNode)
        finalMethod.forEach { methodNode ->
            if (mixinSupport && methodNode.visibleAnnotations != null) {
                annotation@ for (annotationNode in ArrayList(methodNode.visibleAnnotations)) {
                    if (!annotationNode.desc.startsWith("Lorg/spongepowered/asm/mixin/gen/Accessor") &&
                        !annotationNode.desc.startsWith("Lorg/spongepowered/asm/mixin/gen/Invoker") &&
                        !annotationNode.desc.startsWith("Lorg/spongepowered/asm/mixin/Shadow") &&
                        !annotationNode.desc.startsWith("Lorg/spongepowered/asm/mixin/Overwrite")
                    ) {
                        methodNode.signature = signature
                        if (methodNode.localVariables != null) {
                            methodNode.localVariables.forEach { it.signature = signature }
                        }
                    }
                }
            } else {
                methodNode.signature = signature
                if (methodNode.localVariables != null) {
                    methodNode.localVariables.forEach { it.signature = signature }
                }
            }
        }
        classNode.signature = signature
    }

    private fun massiveSource(classNode: ClassNode) {
        classNode.sourceFile = massiveString
        classNode.sourceDebug = massiveString
    }

    private fun pushVarargs(classNode: ClassNode) {
        if (!Modifier.isInterface(classNode.access)) {
            classNode.methods.stream().filter { methodNode ->
                (methodNode.access and Opcodes.ACC_SYNTHETIC == 0) && (methodNode.access and Opcodes.ACC_BRIDGE == 0)
            }.forEach { it.access = it.access or Opcodes.ACC_VARARGS }
        }
    }

    private fun pushTransient(classNode: ClassNode) {
        if (!Modifier.isInterface(classNode.access)) {
            classNode.fields.forEach { it.access = it.access or Opcodes.ACC_TRANSIENT }
        }
    }

    private fun invalidAnnotations(classNode: ClassNode) {
        if (!mixinSupport || !mixinFound(classNode)) {
            if (classNode.visibleAnnotations == null) {
                classNode.visibleAnnotations = ArrayList()
            }
            if (classNode.invisibleAnnotations == null) {
                classNode.invisibleAnnotations = ArrayList()
            }
            classNode.visibleAnnotations.add(AnnotationNode("@"))
            classNode.invisibleAnnotations.add(AnnotationNode("@"))
        }

        fun methodAnnotation(methodNode: MethodNode) {
            if (methodNode.visibleAnnotations == null) {
                methodNode.visibleAnnotations = ArrayList()
            }
            if (methodNode.invisibleAnnotations == null) {
                methodNode.invisibleAnnotations = ArrayList()
            }
            methodNode.visibleAnnotations.add(AnnotationNode("@"))
            methodNode.invisibleAnnotations.add(AnnotationNode("@"))
        }

        fun fieldAnnotation(fieldNode: FieldNode) {
            if (fieldNode.visibleAnnotations == null) {
                fieldNode.visibleAnnotations = ArrayList()
            }
            if (fieldNode.invisibleAnnotations == null) {
                fieldNode.invisibleAnnotations = ArrayList()
            }
            fieldNode.visibleAnnotations.add(AnnotationNode("@"))
            fieldNode.invisibleAnnotations.add(AnnotationNode("@"))
        }

        getSynchornizedMethods(classNode).forEach { methodNode ->
            if (mixinSupport && methodNode.visibleAnnotations != null) {
                for (annotationNode in ArrayList(methodNode.visibleAnnotations)) {
                    if (!annotationNode.desc.startsWith("Lorg/spongepowered/asm/mixin/gen/Accessor") &&
                        !annotationNode.desc.startsWith("Lorg/spongepowered/asm/mixin/gen/Invoker") &&
                        !annotationNode.desc.startsWith("Lorg/spongepowered/asm/mixin/Shadow") &&
                        !annotationNode.desc.startsWith("Lorg/spongepowered/asm/mixin/Overwrite") &&
                        !annotationNode.desc.startsWith("Lorg/spongepowered/asm/mixin/injection/At")
                    ) {
                        methodAnnotation(methodNode)
                    }
                }
            } else {
                methodAnnotation(methodNode)
            }
        }
        getSynchornizedFields(classNode).forEach { fieldNode ->
            if (mixinSupport && fieldNode.visibleAnnotations != null) {
                for (annotationNode in ArrayList(fieldNode.visibleAnnotations)) {
                    if (!annotationNode.desc.startsWith("Lorg/spongepowered/asm/mixin/gen/Accessor") &&
                        !annotationNode.desc.startsWith("Lorg/spongepowered/asm/mixin/gen/Invoker") &&
                        !annotationNode.desc.startsWith("Lorg/spongepowered/asm/mixin/Shadow") &&
                        !annotationNode.desc.startsWith("Lorg/spongepowered/asm/mixin/Overwrite") &&
                        !annotationNode.desc.startsWith("Lorg/spongepowered/asm/mixin/injection/At")
                    ) {
                        fieldAnnotation(fieldNode)
                    }
                }
            } else {
                fieldAnnotation(fieldNode)
            }
        }
    }
}