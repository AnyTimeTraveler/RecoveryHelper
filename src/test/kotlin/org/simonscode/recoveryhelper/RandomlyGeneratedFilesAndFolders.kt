package org.simonscode.recoveryhelper

import org.simonscode.recoveryhelper.TestUtils.getString
import org.simonscode.recoveryhelper.TestUtils.random
import org.testng.Assert.assertEquals
import org.testng.Assert.assertTrue
import org.testng.annotations.AfterMethod
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class RandomlyGeneratedFilesAndFolders {

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
    @BeforeMethod
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
    @AfterMethod
    fun teardown() {
        srcFolder.deleteRecursively()
        dstFolder.deleteRecursively()
        referenceFileIndex.clear()
        File(configFilePath).delete()
    }

    /**
     * Tests if the source files are properly indexed.
     */
    @Test
    fun testIndexingSource() {
        RecoveryHelper.indexSourceFolder(srcFolder)
        val indexedFiles = RecoveryHelper.data.files
        // Assert that the sizes of indexed files match
        assertEquals(indexedFiles.size, referenceFileIndex.size)
        // Assert that all indexed files are also present in the reference list
        assertTrue(indexedFiles.containsAll(referenceFileIndex))
    }

    /**
     * Tests if existing destination files are properly indexed.
     */
    @Test
    fun testIndexingDestination() {
        RecoveryHelper.data.files.clear()
        RecoveryHelper.data.files.addAll(referenceFileIndex)
        var destinationFileCount = 0
        for (i in 0 until filesPerFolder) {
            val file = referenceFileIndex[TestUtils.random.nextInt(referenceFileIndex.size)]
            val dstFile = File(dstFolder.absolutePath + file.path)
            dstFile.mkdirs()
            dstFile.delete()
            Files.copy(File(srcFolder.absolutePath + file.path).toPath(), dstFile.toPath(), StandardCopyOption.COPY_ATTRIBUTES)
            if (dstFile.exists())
                destinationFileCount++
        }
        RecoveryHelper.indexDestination()
        // Assert that the correct amount of files has been identified.
        assertEquals(RecoveryHelper.data.files.filter { it.status == Enums.Status.COPIED }.count(), destinationFileCount)
    }

    /**
     * Tests if files are properly copied.
     */
    @Test
    fun testCopying() {
        RecoveryHelper.data.files = referenceFileIndex
        RecoveryHelper.copy()
        // Assert that the correct amount of files has been identified.
        assertEquals(RecoveryHelper.data.files.size, referenceFileIndex.size)
        // Assert that all indexed files are also present in the reference list
        assertTrue(RecoveryHelper.data.files.containsAll(referenceFileIndex))
    }

    /**
     * Tests if files are properly verified.
     */
    @Test
    fun testVerifying() {
        val file = referenceFileIndex[random.nextInt(referenceFileIndex.size)]
        val srcFile = File(srcFolder.absolutePath + file.path)
        val dstFile = File(dstFolder.absolutePath + file.path)

        dstFile.mkdirs()
        dstFile.delete()
        Files.copy(srcFile.toPath(), dstFile.toPath(), StandardCopyOption.COPY_ATTRIBUTES)
        file.status = Enums.Status.COPIED
        RecoveryHelper.verifyFile(file)

        // File is unmodified,
        assertEquals(file.status, Enums.Status.VERIFIED)
        val dstData = srcFile.readBytes()
        for (i in 0..dstData.size / 2)
            dstData[i] = dstData[i + 1]
        dstFile.delete()
        dstFile.createNewFile()
        dstFile.writeBytes(dstData)
        file.status = Enums.Status.COPIED
        RecoveryHelper.verifyFile(file)
        assertEquals(file.status, Enums.Status.FAILED)
    }

    /**
     * Do a full run of the program.
     */
    @Test
    fun testFullRun() {
        RecoveryHelper.doTheActualRecovering()
        assertEquals(RecoveryHelper.data.progress,Enums.Progress.DONE)
        RecoveryHelper.doTheActualRecovering()
        assertEquals(RecoveryHelper.data.progress,Enums.Progress.INDEXING_SRC)
    }
}
