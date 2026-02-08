package com.example.foldersync

import android.content.ContentResolver
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.Mockito.*
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class SyncLogicTest {

    @Test
    fun testSyncCopiesFileWhenMissingInDest() = runTest {
        val mockResolver = mock(ContentResolver::class.java)
        val mockCallback = mock(SyncCallback::class.java)
        val mockSourceDir = mock(DocumentFile::class.java)
        val mockDestDir = mock(DocumentFile::class.java)
        val mockSourceFile = mock(DocumentFile::class.java)

        val sourceUri = mock(Uri::class.java)
        val destUri = mock(Uri::class.java)
        val newFile = mock(DocumentFile::class.java)

        // Setup source
        `when`(mockSourceDir.listFiles()).thenReturn(arrayOf(mockSourceFile))
        `when`(mockSourceFile.isDirectory).thenReturn(false)
        `when`(mockSourceFile.name).thenReturn("test.txt")
        `when`(mockSourceFile.uri).thenReturn(sourceUri)
        `when`(mockSourceFile.type).thenReturn("text/plain")

        // Setup dest
        `when`(mockDestDir.findFile("test.txt")).thenReturn(null)
        `when`(mockDestDir.createFile("text/plain", "test.txt")).thenReturn(newFile)
        `when`(newFile.uri).thenReturn(destUri)

        // Setup streams
        `when`(mockResolver.openInputStream(sourceUri)).thenReturn(ByteArrayInputStream("content".toByteArray()))
        `when`(mockResolver.openOutputStream(destUri)).thenReturn(ByteArrayOutputStream())

        val syncLogic = SyncLogic(mockResolver, mockCallback)
        syncLogic.syncFolder(mockSourceDir, mockDestDir, false)

        verify(mockDestDir).createFile("text/plain", "test.txt")
        verify(mockResolver).openInputStream(sourceUri)
        verify(mockResolver).openOutputStream(destUri)
        verify(mockCallback).onLog("Copying: test.txt")
    }
}
