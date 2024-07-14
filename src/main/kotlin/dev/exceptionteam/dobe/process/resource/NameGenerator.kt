package dev.exceptionteam.dobe.process.resource

import dev.exceptionteam.dobe.config.Configs
import java.util.concurrent.atomic.AtomicInteger

sealed class NameGenerator(val name: Dictionary) {
    enum class Dictionary(val dicName: String) {
        Alphabet("alphabet"),
        Chinese("chinese"),
        Confuse("confuse"),
        Custom("custom"),
        Space("space"),
        Emoji("emoji"),
        Latin("latin")
    }

    abstract val chars: List<Char>
    private val size get() = chars.size
    private val index = AtomicInteger(Configs.Settings.dictionaryStartIndex)
    private val methodOverloads = hashMapOf<String, MutableList<String>>() // Name Descs

    var overloadsCount = 0; private set
    var actualNameCount = 0; private set

    fun nextName(): String {
        var index = index.getAndIncrement()
        return if (index == 0) chars[0].toString()
        else {
            val charArray = mutableListOf<Char>()
            while (true) {
                charArray.add(chars[index % size])
                index /= size
                if (index == 0) break
            }
            charArray.reversed().joinToString(separator = "")
        }
    }

    @Synchronized
    fun nextName(overload: Boolean, desc: String): String {
        if (!overload) return nextName()
        else {
            //nameCache[desc]?.let { return it }
            for (pair in methodOverloads) {
                if (!pair.value.contains(desc)) {
                    pair.value.add(desc)
                    overloadsCount++
                    return pair.key
                }
            }
            // Generate a new one
            val newName = nextName()
            methodOverloads[newName] = mutableListOf(desc)
            actualNameCount++
            return newName
        }
    }

    class Alphabet : NameGenerator(Dictionary.Alphabet) {
        override val chars = ('a'..'z') + ('A'..'Z')
    }

    class Chinese : NameGenerator(Dictionary.Chinese) {
        override val chars = listOf('操', '你', '妈', '傻', '逼', '滚', '笨', '猪')
    }

    class Confuse : NameGenerator(Dictionary.Confuse) {
        override val chars = listOf('i', 'I', 'l', '1')
    }

    class Custom : NameGenerator(Dictionary.Custom) {
        override val chars = kotlin.run {
            val charList = mutableListOf<Char>()
            Configs.Settings.customDictionary.forEach {
                if (it.isNotEmpty()) charList.add(it[0])
            }
            charList.toSet().toList().ifEmpty { ('a'..'z') + ('A'..'Z') }
        }
    }

    class Space : NameGenerator(Dictionary.Space) {
        override val chars = listOf(
            '\u2000', '\u2001', '\u2002', '\u2003', '\u2004', '\u2005', '\u2006', '\u2007',
            '\u2008', '\u2009', '\u200a', '\u200b', '\u200c', '\u200d', '\u200e', '\u200f'
        )
    }

    class Emoji : NameGenerator(Dictionary.Emoji) {
        override val chars = listOf(
            '\u1F60', '\u1F61', '\u1F62', '\u1F63', '\u1F64', '\u1F65', '\u1F66', '\u1F67',
            '\u1F68', '\u1F69', '\u1F6a', '\u1F6b', '\u1F6c', '\u1F6d', '\u1F6e', '\u1F6f'
        )
    }

    class Latin : NameGenerator(Dictionary.Latin) {
        override val chars = listOf(
            '\u00BF', '\u00B1', '\u00B2', '\u00B3', '\u00B4', '\u00B5', '\u00B6', '\u00B7',
            '\u00B8', '\u00B9', '\u00B9', '\u00BA', '\u00BB', '\u00BC', '\u00BD', '\u00BE'
        )
    }

    companion object {
        fun getByName(name: String): NameGenerator =
            when (name.lowercase()) {
                "alphabet" -> Alphabet()
                "chinese" -> Chinese()
                "confuse" -> Confuse()
                "custom" -> Custom()
                "space" -> Space()
                "emoji" -> Emoji()
                "latin" -> Latin()
                else -> Alphabet()
            }

        fun getByEnum(name: Dictionary): NameGenerator =
            when (name.dicName) {
                "alphabet" -> Alphabet()
                "chinese" -> Chinese()
                "confuse" -> Confuse()
                "custom" -> Custom()
                "space" -> Space()
                "emoji" -> Emoji()
                "latin" -> Latin()
                else -> Alphabet()
            }
    }

}