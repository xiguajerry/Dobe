package dev.exceptionteam.dobe.process.hierarchy

interface Hierarchy {
    fun build()
    fun isSubType(child: String, father: String): Boolean
    val size: Int
}