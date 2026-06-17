package com.kianirani.jarvis.core.memory

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MemoryPolicyTest {

    @Test fun `blank and very short messages are not worth remembering`() {
        assertFalse(MemoryPolicy.worthRemembering(""))
        assertFalse(MemoryPolicy.worthRemembering("hi"))
        assertFalse(MemoryPolicy.worthRemembering("   سلام   "))
    }

    @Test fun `a short command is not remembered`() {
        assertFalse(MemoryPolicy.worthRemembering("open camera"))
    }

    @Test fun `a substantial statement is remembered`() {
        assertTrue(MemoryPolicy.worthRemembering("I am building an Android launcher called Vision"))
    }

    @Test fun `a substantial Persian statement is remembered`() {
        assertTrue(MemoryPolicy.worthRemembering("اسم پروژه من ویژن است و یک لانچر اندروید است"))
    }

    @Test fun `long text with too few words is not remembered`() {
        assertFalse(MemoryPolicy.worthRemembering("https://github.com/kian-irani/Jarvis-android-repo"))
        assertFalse(MemoryPolicy.worthRemembering("supercalifragilisticexpialidocious word"))
    }
}
