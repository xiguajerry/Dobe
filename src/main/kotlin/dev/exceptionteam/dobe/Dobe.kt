package dev.exceptionteam.dobe

import dev.exceptionteam.dobe.config.Configs
import dev.exceptionteam.dobe.process.Transformers
import dev.exceptionteam.dobe.process.resource.ResourceCache
import dev.exceptionteam.dobe.utils.logging.Logger
import dev.exceptionteam.dobe.utils.scope.ConcurrentScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.CountDownLatch
import kotlin.system.measureTimeMillis

const val VERSION = "1.0"
const val AUTHOR = "ExceptionTeam"
const val GITHUB = "https://github.com/ExceptionTeam6969/Dobe"

fun main(args: Array<String>) {

    println(
        """
     ________        ___.           
    \______ \   ____\_ |__   ____  
     |    |  \ /  _ \| __ \_/ __ \ 
     |    `   (  <_> ) \_\ \  ___/ 
    /_______  /\____/|___  /\___  >
            \/           \/     \/
    """.trimIndent()
    )
    println("==========================================================")
    println(" Dobe Obfuscator (Version: $VERSION, Author: $AUTHOR)")
    println(" Github: $GITHUB")
    println("==========================================================")

    Logger.info("Initializing Dobe Obfuscator...")
    val configName = args.getOrNull(0) ?: "config.json"
    Logger.info("Using config $configName")
    try {
        Configs.loadConfig(configName)
        Configs.saveConfig(configName)
    } catch (ignore: Exception) {
        Logger.info("Failed to read config $configName!But we generated a new one.")
        Configs.saveConfig(configName)
        Logger.info("Type (Y/N) if you want to continue")
        if (readlnOrNull()?.lowercase() == "n") return
    }

    val time = measureTimeMillis {
        ResourceCache(Configs.Settings.input, Configs.Settings.libraries).apply {
            runBlocking { readJar() }
            val obfTime = measureTimeMillis {
                Logger.info("Processing${if (Configs.Settings.parallel) "[MultiThread]" else ""}...")
                val cdl = CountDownLatch(Transformers.filter { it.enabled }.size)
                ConcurrentScope.launch {
                    Transformers.forEach {
                        if (it.enabled) with(it) {
                            transform()
                            cdl.countDown()
                        }
                    }
                }
                cdl.await()
            }
            Logger.info("Took $obfTime ms to process!")
            Logger.info("Dumping to ${Configs.Settings.output}")
        }.dumpJar(Configs.Settings.output)
    }
    Logger.info("Finished in $time ms!")

}