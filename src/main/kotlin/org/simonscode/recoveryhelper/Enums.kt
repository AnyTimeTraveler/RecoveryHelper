package org.simonscode.recoveryhelper

class Enums {
    enum class Progress {
        INDEXING_SRC,
        INDEXING_DST,
        COPYING,
        VERIFYING,
        DONE
    }

    enum class Status {
        INDEXED,
        FAILED,
        COPIED,
        VERIFIED,
        GIVEN_UP
    }
}