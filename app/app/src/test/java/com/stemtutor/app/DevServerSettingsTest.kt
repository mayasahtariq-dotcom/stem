package com.stemtutor.app

import com.stemtutor.app.di.DevServerSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DevServerSettingsTest {
    @Test
    fun addressesAreNormalised() {
        assertEquals("http://192.168.1.5:8000/", DevServerSettings.normalize("192.168.1.5:8000"))
        assertEquals("http://192.168.1.5:8000/", DevServerSettings.normalize(" http://192.168.1.5:8000 "))
        assertEquals("https://api.example.com/", DevServerSettings.normalize("https://api.example.com/"))
    }

    @Test
    fun junkIsRejected() {
        assertNull(DevServerSettings.normalize(""))
        assertNull(DevServerSettings.normalize("http://"))
        assertNull(DevServerSettings.normalize("192.168 1.5"))
    }
}
