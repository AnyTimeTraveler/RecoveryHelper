package org.simonscode.recoveryhelper

import org.simonscode.recoveryhelper.TestUtils.getString
import org.testng.Assert.*
import org.testng.annotations.AfterClass
import org.testng.annotations.BeforeClass
import org.testng.annotations.Test
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.PrintStream
import java.util.*

class ArgumentParsing {
    private val slash = File.separator

    private val basePath = "_working_directory"
    private val srcFolderName = "test_src"
    private val dstFolderName = "test_dst"
    private val configFilePath = basePath + slash + "test_config.json"

    private val folders = 10
    private val filesPerFolder = 10
    private val fileSizeInCharacters = 10_000
    private val folderNameLength = 10
    private val fileNameLength = 10
    private val fileExtensionhLength = 3


    private val srcFolder = File(basePath + slash + srcFolderName + slash)
    private val dstFolder = File(basePath + slash + dstFolderName + slash)

    private val referenceFileIndex = ArrayList<MyFile>()
    /**
     * Generates a simple randomized folder structure to backup.
     */
    @BeforeClass
    fun setup() {
        File(configFilePath).delete()
        val cfg = Config()
        cfg.configFilePath = configFilePath
        cfg.srcPath = srcFolder.absolutePath
        cfg.dstPath = dstFolder.absolutePath
        RecoveryHelper.data = cfg
        dstFolder.deleteRecursively()
        srcFolder.mkdirs()
        for (folder in 0..folders) {
            val fn = getString(folderNameLength)
            File(srcFolder.absolutePath + slash + fn).mkdirs()
            for (file in 0..filesPerFolder) {
                val f = File(srcFolder.absolutePath + slash + fn + slash + getString(fileNameLength) + "." + getString(fileExtensionhLength))
                f.writeText(getString(fileSizeInCharacters))
                referenceFileIndex.add(MyFile(f.absolutePath.substring(srcFolder.absolutePath.length)))
            }
        }

        dstFolder.mkdirs()
        dstFolder.deleteRecursively()
    }

    /**
     * Be clean and delete everything again
     */
    @AfterClass
    fun teardown() {
        srcFolder.deleteRecursively()
        dstFolder.deleteRecursively()
        referenceFileIndex.clear()
        File(configFilePath).delete()
    }

    @Test
    fun parseOneArgNoConfigFile() {
        RecoveryHelper.data = Config()
        try {
            RecoveryHelper.parseArgs(arrayOf(configFilePath))
            fail("Config file does not exist, but program didn't error")
        } catch (e: FileNotFoundException) {
        }
    }

    @Test
    fun parseOneArgWithConfig() {
        val cfg = Config()
        cfg.configFilePath = configFilePath
        cfg.srcPath = srcFolder.absolutePath
        cfg.dstPath = dstFolder.absolutePath
        cfg.files.add(MyFile("EMPTY", Enums.Status.FAILED))
        cfg.save()
        RecoveryHelper.data = Config()
        RecoveryHelper.parseArgs(arrayOf(configFilePath))
        assertEquals(File(RecoveryHelper.data.configFilePath).absolutePath, File(configFilePath).absolutePath)
        assertEquals(RecoveryHelper.data.srcPath, cfg.srcPath)
        assertEquals(RecoveryHelper.data.dstPath, cfg.dstPath)
        assertEquals(RecoveryHelper.data.files.size, 1)
    }

    @Test
    fun parseOneArgWithCorruptedConfig() {
        val cfg = Config()
        cfg.configFilePath = configFilePath
        cfg.srcPath = srcFolder.absolutePath
        cfg.dstPath = dstFolder.absolutePath
        cfg.files.add(MyFile("EMPTY", Enums.Status.FAILED))
        cfg.save()

        val configFile = File(configFilePath)
        val configData = configFile.readLines()
        configFile.delete()
        configFile.createNewFile()
        configFile.writeText(configData.subList(0, configData.size / 2).joinToString("\n"))
        try {
            RecoveryHelper.parseArgs(arrayOf(configFilePath))
            fail("Config file is cut in half. It should have not succeeded in reading")
        } catch (e: Exception) {

        }
    }

    @Test
    fun parseThreeArgsWithoutConfig() {
        File(configFilePath).delete()
        RecoveryHelper.data = Config()
        RecoveryHelper.parseArgs(arrayOf(srcFolder.absolutePath, dstFolder.absolutePath, configFilePath))
        assertEquals(File(RecoveryHelper.data.configFilePath).absolutePath, File(configFilePath).absolutePath)
        assertEquals(RecoveryHelper.data.srcPath, srcFolder.absolutePath)
        assertEquals(RecoveryHelper.data.dstPath, dstFolder.absolutePath)
        assertEquals(RecoveryHelper.data.files.size, 0)
    }


    @Test
    fun parseThreeArgsWithCorruptedConfig() {
        val cfg = Config()
        cfg.configFilePath = configFilePath
        cfg.srcPath = srcFolder.absolutePath
        cfg.dstPath = dstFolder.absolutePath
        cfg.files.add(MyFile("EMPTY", Enums.Status.FAILED))
        cfg.save()

        val configFile = File(configFilePath)
        val configData = configFile.readLines()
        configFile.delete()
        configFile.createNewFile()
        configFile.writeText(configData.subList(0, configData.size / 2).joinToString("\n"))

        // Redirect output streams
        val stream = ByteArrayOutputStream()
        System.setErr(PrintStream(stream))
        RecoveryHelper.parseArgs(arrayOf("", "", configFilePath))
        assertTrue(stream.toString().startsWith("Config file not readable!\n" +
                "Starting from scratch!"))
        System.setErr(null)
    }

    @Test
    fun parseThreeArgsWithConfig() {
        val cfg = Config()
        cfg.configFilePath = configFilePath
        cfg.srcPath = srcFolder.absolutePath
        cfg.dstPath = dstFolder.absolutePath
        cfg.files.add(MyFile("EMPTY", Enums.Status.FAILED))
        cfg.save()
        RecoveryHelper.data = Config()
        RecoveryHelper.parseArgs(arrayOf("modified source path", "modified destination path", configFilePath))
        assertEquals(File(RecoveryHelper.data.configFilePath).absolutePath, File(configFilePath).absolutePath)
        assertEquals(RecoveryHelper.data.srcPath, "modified source path")
        assertEquals(RecoveryHelper.data.dstPath, "modified destination path")
        assertEquals(RecoveryHelper.data.files.size, 1)
    }

    @Test
    fun noArgs() {
        try {
            RecoveryHelper.parseArgs(arrayOf())
            fail()
        } catch (e: Exception) {
            assertEquals(e.message, "Invalid Usage")
        }

    }
}