package org.simonscode.recoveryhelper

class MyFile(var path: String, var status: Enums.Status = Enums.Status.INDEXED) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as MyFile
        if (path != other.path) return false
        return true
    }

    override fun hashCode(): Int {
        return path.hashCode() + status.hashCode()
    }

    override fun toString(): String {
        return "MyFile(path='$path' status='$status')"
    }
}