package dev.exceptionteam.dobe.utils.logging

object Logger : ILogger by SimpleLogger(
    "Dobe",
    //"logs/${SimpleDateFormat("yyyy-MM-dd HH-mm-ss").format(Date())}.txt"
)