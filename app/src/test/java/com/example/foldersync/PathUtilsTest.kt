package com.example.foldersync

import android.content.Context
import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.*

class PathUtilsTest {

    @Test
    fun testFileScheme() {
        val mockContext = mock(Context::class.java)
        val mockUri = mock(Uri::class.java)

        `when`(mockUri.scheme).thenReturn("file")
        `when`(mockUri.path).thenReturn("/storage/emulated/0/Download")

        val path = PathUtils.getPath(mockContext, mockUri)
        assertEquals("/storage/emulated/0/Download", path)
    }
}
