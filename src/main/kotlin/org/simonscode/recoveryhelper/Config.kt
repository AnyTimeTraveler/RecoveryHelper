package org.simonscode.recoveryhelper

import com.google.gson.GsonBuilder
import java.io.File
import java.io.IOException
import java.lang.reflect.Modifier
import java.nio.charset.Charset

class Config internal constructor() {
    var progress: Enums.Progress = Enums.Progress.INDEXING_SRC
    var srcPath = ""
    var dstPath = ""
    var files = ArrayList<MyFile>()
    @Transient
    var configFilePath = ""

    companion object {
        private val gson = GsonBuilder().excludeFieldsWithModifiers(Modifier.TRANSIENT, Modifier.STATIC, Modifier.FINAL).setPrettyPrinting().create()

        @Throws(IOException::class)
        fun load(file: File): Config {
            val config = gson.fromJson(file.readText(Charset.forName("UTF-8")), Config::class.java)
            config.configFilePath = file.absolutePath
            return config
        }
    }

    fun save() {
        File(configFilePath).writeText(gson.toJson(this))
    }
}