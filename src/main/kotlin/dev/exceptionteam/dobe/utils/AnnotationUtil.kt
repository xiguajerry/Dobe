package dev.exceptionteam.dobe.utils

import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode

var annotationClassName = "dev/zenhao/melon/mixins/IndyClassObfuscate"
var annotationMethodName = "dev/zenhao/melon/mixins/IndyMethodObfuscate"
var annotationFieldName = "dev/zenhao/melon/mixins/IndyFieldObfuscate"

fun ClassNode.isIndy(): Boolean {
    return (!visibleAnnotations.isNullOrEmpty() && visibleAnnotations.any { annotationNode ->
        annotationNode.desc.contains(
            ("L${annotationClassName}")
        )
    })
}

fun MethodNode.isIndy(): Boolean {
    return (!visibleAnnotations.isNullOrEmpty() && visibleAnnotations.any { annotationNode ->
        annotationNode.desc.contains(
            ("L${annotationMethodName}")
        )
    })
}

fun FieldNode.isIndy(): Boolean {
    return (!visibleAnnotations.isNullOrEmpty() && visibleAnnotations.any { annotationNode ->
        annotationNode.desc.contains(
            ("L${annotationFieldName}")
        )
    })
}

fun ClassNode.removeIndyAnnotation() {
    if (!visibleAnnotations.isNullOrEmpty()) {
        for (annotationsCount in visibleAnnotations.indices) {
            if (visibleAnnotations[annotationsCount].desc != "L${annotationClassName};") continue
            visibleAnnotations.removeAt(annotationsCount)
            break
        }
    }
}

fun MethodNode.removeIndyAnnotation() {
    if (!visibleAnnotations.isNullOrEmpty()) {
        for (annotationsCount in visibleAnnotations.indices) {
            if (visibleAnnotations[annotationsCount].desc != "L${annotationMethodName};") continue
            visibleAnnotations.removeAt(annotationsCount)
            break
        }
    }
}

fun FieldNode.removeIndyAnnotation() {
    if (!visibleAnnotations.isNullOrEmpty()) {
        for (annotationsCount in visibleAnnotations.indices) {
            if (visibleAnnotations[annotationsCount].desc != "L${annotationFieldName};") continue
            visibleAnnotations.removeAt(annotationsCount)
            break
        }
    }
}