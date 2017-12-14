package org.simonscode.recoveryhelper

import java.io.File
import java.io.FileNotFoundException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.*
import java.util.zip.CRC32

object RecoveryHelper {
    private val className = "RecoveryHelper"
    private val usageString = "USAGE: $className <SOURCE_ROOT> <DESTINATION_ROOT> <CONFIG_FILE>\n" +
            "or" +
            "USAGE: $className <CONFIG_FILE>"
    private var srcPrefixLength = 0
    private var dstPrefixLength = 0
    var data: Config = Config()
    private var running = true

    @JvmStatic
    fun main(args: Array<String>) {
        try {
            parseArgs(args)
        } catch (e: Exception) {
            if (e.message != "Invalid Usage")
                throw e
            return
        }
        val mainThread = Thread.currentThread()
        Runtime.getRuntime().addShutdownHook(object : Thread("shutdown hook") {
            override fun run() {
                running = false
                var i = 0
                while (mainThread.isAlive) {
                    Thread.sleep(10)
                    if (i++ % 2_000 == 0) {
                        println("Please wait while shutting down! Do not unplug your device, yet!")
                    }
                }
                println("Saving progress...")
                data.save()
                println("Saved!")
                println("You can now unplug your device.")
            }
        })
        doTheActualRecovering()
    }

    fun parseArgs(args: Array<String>) {
        outer@ when {
            args.size == 1 -> {
                val configFile = File(args[0])
                if (!configFile.exists()) {
                    println(usageString)
                    throw FileNotFoundException(configFile.absolutePath)
                }
                try {
                    data = Config.load(configFile)
                } catch (e: Exception) {
                    System.err.println("Config file not readable!")
                    e.printStackTrace()
                    throw e
                }
            }
            args.size == 3 -> {
                val configFile = File(args[2])
                if (configFile.exists()) {
                    try {
                        data = Config.load(configFile)
                        data.srcPath = args[0]
                        data.dstPath = args[1]
                        return@outer
                    } catch (e: Exception) {
                        System.err.println("Config file not readable!\nStarting from scratch!")
                        e.printStackTrace()
                    }
                }
                data = Config()
                data.srcPath = args[0]
                data.dstPath = args[1]
                data.configFilePath = args[2]
                data.save()
            }
            else -> {
                println(usageString)
                throw Exception("Invalid Usage")
            }
        }
    }

    fun doTheActualRecovering() {
        try {
            srcPrefixLength = File(data.srcPath).absolutePath.length
            dstPrefixLength = File(data.dstPath).absolutePath.length

            if (data.progress == Enums.Progress.INDEXING_SRC) {
                println("Started indexing source...")
                indexSourceFolder(File(data.srcPath))
                if (running)
                    data.progress = Enums.Progress.INDEXING_DST
                data.save()
                println("Completed indexing source!")
            }
            if (data.progress == Enums.Progress.INDEXING_DST) {
                println("Started indexing destination...")
                indexDestination()
                if (running)
                    data.progress = Enums.Progress.COPYING
                data.save()
                println("Completed indexing destination!")
            }
            if (data.progress == Enums.Progress.COPYING) {
                println("Started copying...")
                copy()
                if (running && data.files.none { it.status == Enums.Status.INDEXED })
                    data.progress = Enums.Progress.VERIFYING
                data.save()
                println("Completed copying!")
            }
            if (data.progress == Enums.Progress.VERIFYING) {
                println("Started verifying...")
                verify()
                if (running && data.files.none { it.status == Enums.Status.COPIED })
                    data.progress = Enums.Progress.DONE
                data.save()
                println("Completed verifying!")
            } else if (data.progress == Enums.Progress.DONE) {
                println("Nothing to do!\n" +
                        "Rerun to start indexing again!")
                data.progress = Enums.Progress.INDEXING_SRC
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        data.save()
    }

    fun indexSourceFolder(parent: File) {
        parent.listFiles().forEach {
            if (!running) {
                return
            }
            val success = indexSourceFile(it)
            if (!success) {
                println("\nError 001!")
                data.save()
                System.exit(0)
            }
        }
    }

    fun indexSourceFile(file: File): Boolean {
        val path = file.absolutePath.substring(srcPrefixLength)
        var retries = 0
        var successful = false
        var failed = false
        while (!successful && !failed) {
            try {
                @Suppress("LiftReturnOrAssignment")
                if (file.isDirectory) {
                    println("Indexing Folder:  $path")
                    indexSourceFolder(file)
                    successful = true
                } else {
                    println("Indexing File:    $path")
                    val indexedFile = MyFile(path)
                    if (!data.files.contains(indexedFile))
                        data.files.add(indexedFile)
                    successful = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
                print("Failed ${++retries} times")
                if (retries < 10)
                    println(", trying again")
                else
                    failed = true
            }
        }
        return !failed
    }

    fun indexDestination() {
        data.files.forEach {
            if (!running) {
                return
            }
            try {
                indexDestinationFile(it)
            } catch (e: Exception) {
                e.printStackTrace()
                println("Destination is not supposed to have any errors!")
                data.save()
                System.exit(-1)
            }
        }
    }

    private fun indexDestinationFile(file: MyFile) {
        if (File(data.dstPath + file.path).exists()) {
            println("Indexed destination File:    ${file.path}")
            file.status = Enums.Status.COPIED
        }
    }

    fun copy() {
        for (file in data.files) {
            copyFile(file)
            if (file.status == Enums.Status.FAILED) {
                println("Error 002!")
                data.save()
                System.exit(0)
            }
        }
    }

    private fun copyFile(file: MyFile) {
        var retries = 0
        while (running && (file.status == Enums.Status.INDEXED || (file.status == Enums.Status.FAILED && retries < 10))) {
            try {
                val dstFile = File(data.dstPath + file.path)
                if (dstFile.exists()) {
                    println("Copied File:     ${file.path} (File already existed at destination)")
                    file.status = Enums.Status.COPIED
                    return
                }
                dstFile.mkdirs()
                dstFile.delete()
                Files.copy(File(data.srcPath + file.path).toPath(), dstFile.toPath(), StandardCopyOption.COPY_ATTRIBUTES)
                println("Copied File:     ${file.path}")
                file.status = Enums.Status.COPIED
            } catch (e: Exception) {
                e.printStackTrace()
                print("Failed ${++retries} times")
                if (retries < 10)
                    println(", trying again")
                else {
                    println()
                    file.status = Enums.Status.FAILED
                }
            }
        }
    }

    fun verify() {
        for (file in data.files) {
            if (!running) {
                return
            }
            verifyFile(file)
            if (file.status == Enums.Status.FAILED) {
                data.progress = Enums.Progress.INDEXING_SRC
                println("Error 003!")
                data.save()
                System.exit(0)
            }
        }
    }

    fun verifyFile(file: MyFile) {
        var retries = 0
        while (running && file.status == Enums.Status.COPIED) {
            try {
                val srcCRC = CRC32()
                val dstCRC = CRC32()
                val srcFile = File(data.srcPath + file.path)
                val dstFile = File(data.dstPath + file.path)
                if (!srcFile.exists()) {
                    println("Failed verification of source File: ${file.path}\nFile not found!\n\nRemove from files to be recovered? [Y/n]")
                    val sc = Scanner(System.`in`)
                    file.status = when (sc.nextLine().toLowerCase()) {
                        "no", "n", "nein" -> {
                            Enums.Status.INDEXED
                        }
                        "yes", "y", "ja", "j", "" -> {
                            Enums.Status.GIVEN_UP
                        }
                        else -> {
                            Enums.Status.GIVEN_UP
                        }
                    }
                    sc.close()
                }
                if (!dstFile.exists()) {
                    println("Failed verification of destination File: ${file.path}\nFile not found!")
                    file.status = Enums.Status.INDEXED
                }
                srcCRC.update(srcFile.readBytes())
                dstCRC.update(dstFile.readBytes())
                if (srcCRC.value != dstCRC.value) {
                    println("Failed verification of File: ${file.path}")
                    file.status = Enums.Status.FAILED
                } else {
                    println("Verified File:    ${file.path}")
                    file.status = Enums.Status.VERIFIED
                }
            } catch (e: Exception) {
                e.printStackTrace()
                print("Failed ${++retries} times")
                if (retries < 10)
                    println(", trying again")
                else {
                    println()
                    file.status = Enums.Status.FAILED
                }
            }
        }
    }
}
