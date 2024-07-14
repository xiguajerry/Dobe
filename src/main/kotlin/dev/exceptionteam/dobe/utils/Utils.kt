package dev.exceptionteam.dobe.utils

import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import java.util.zip.CRC32
import java.util.zip.ZipOutputStream
import kotlin.random.Random

fun String.getReturnType(): Int = when (Type.getReturnType(this).sort) {
    Type.BOOLEAN -> Opcodes.IRETURN
    Type.CHAR -> Opcodes.IRETURN
    Type.BYTE -> Opcodes.IRETURN
    Type.SHORT -> Opcodes.IRETURN
    Type.INT -> Opcodes.IRETURN
    Type.LONG -> Opcodes.LRETURN
    Type.FLOAT -> Opcodes.FRETURN
    Type.DOUBLE -> Opcodes.DRETURN
    Type.VOID -> Opcodes.RETURN
    else -> Opcodes.ARETURN
}

fun Type.getLoadType(): Int = when (sort) {
    Type.BOOLEAN -> Opcodes.ILOAD
    Type.CHAR -> Opcodes.ILOAD
    Type.BYTE -> Opcodes.ILOAD
    Type.SHORT -> Opcodes.ILOAD
    Type.INT -> Opcodes.ILOAD
    Type.LONG -> Opcodes.LLOAD
    Type.FLOAT -> Opcodes.FLOAD
    Type.DOUBLE -> Opcodes.DLOAD
    else -> Opcodes.ALOAD
}

fun Type.getStoreType(): Int = when (sort) {
    Type.BOOLEAN -> Opcodes.ISTORE
    Type.CHAR -> Opcodes.ISTORE
    Type.BYTE -> Opcodes.ISTORE
    Type.SHORT -> Opcodes.ISTORE
    Type.INT -> Opcodes.ISTORE
    Type.LONG -> Opcodes.LSTORE
    Type.FLOAT -> Opcodes.FSTORE
    Type.DOUBLE -> Opcodes.DSTORE
    else -> Opcodes.ASTORE
}

fun getRandomString(length: Int): String {
    val charSet = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890"
    var str = ""
    repeat(length) {
        str += charSet[(charSet.length * Random.nextInt(0, 100) / 100f).toInt()]
    }
    return str
}

val nextBadKeyword get() = badKeywords.random()

val javaKeywords = arrayOf(
    "abstract",
    "assert",
    "boolean",
    "break",
    "byte",
    "case",
    "catch",
    "char",
    "class",
    "const",
    "continue",
    "default",
    "do",
    "double",
    "else",
    "enum",
    "extends",
    "false",
    "final",
    "finally",
    "float",
    "for",
    "goto",
    "if",
    "implements",
    "import",
    "instanceof",
    "int",
    "interface",
    "long",
    "native",
    "new",
    "null",
    "package",
    "private",
    "protected",
    "public",
    "return",
    "short",
    "static",
    "strictfp",
    "super",
    "switch",
    "synchronized",
    "this",
    "throw",
    "throws",
    "transient",
    "true",
    "try",
    "void",
    "volatile",
    "while"
)

val badKeywords = arrayOf(
    "public",
    "private",
    "protected",
    "static",
    "final",
    "native",
    "class",
    "interface",
    "enum",
    "abstract",
    "int",
    "float",
    "double",
    "short",
    "byte",
    "long",
    "synchronized",
    "strictfp",
    "volatile",
    "transient",
    "return",
    "for",
    "while",
    "switch",
    "break"
)


fun genKey(n: Int): String {
    return genKey(n, 600, 900)
}

fun genKey(n: Int, n2: Int, n3: Int): String {
    val stringBuilder = StringBuilder()
    while (stringBuilder.length < n) {
        stringBuilder.append((n2.toDouble() + Math.random() * (n3 - n2).toDouble()).toInt().toChar())
    }
    return stringBuilder.toString()
}

fun ZipOutputStream.corruptCRC32() {
    val field = ZipOutputStream::class.java.getDeclaredField("crc")
    field.isAccessible = true
    field[this] = object : CRC32() {
        override fun update(bytes: ByteArray, i: Int, length: Int) {}
        override fun getValue(): Long {
            return Random.nextInt(0, Int.MAX_VALUE).toLong()
        }
    }
}
