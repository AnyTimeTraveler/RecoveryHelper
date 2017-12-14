package org.simonscode.recoveryhelper

import org.testng.Assert.*
import org.testng.annotations.Test

@Suppress("ReplaceCallWithComparison")
class MyFileTest {
    private val file0 = MyFile("path0")
    private val file1 = MyFile("path1", Enums.Status.INDEXED)
    private val file2 = MyFile("path2", Enums.Status.FAILED)

    @Test
    fun equalsTest() {
        assertFalse(file0.equals(0))
        assertEquals(file0.toString(), file0.toString())
        assertNotEquals(file0, file1)
        assertEquals(file0.status, file1.status)
        assertNotEquals(file1.status, file2.status)
        assertNotEquals(file1.path, file2.path)
    }

    @Test
    fun hashCodeTest() {
        assertEquals(file0.hashCode(), file0.path.hashCode() + file0.status.hashCode())
        assertNotEquals(file0.hashCode(), file1.hashCode())
    }

    @Test
    fun toStringTest() {
        val path = ""
        val status = Enums.Status.FAILED

        val file = MyFile(path, status)
        assertEquals(file.toString(), "MyFile(path='$path' status='$status')")

    }
}