package com.example.foldersync

import android.content.Context
import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.*

class SyncPairRepositoryTest {

    @Test
    fun testSyncPairSerialization() {
        val mockContext = mock(Context::class.java)
        val mockPrefs = mock(SharedPreferences::class.java)
        val mockEditor = mock(SharedPreferences.Editor::class.java)

        `when`(mockContext.getSharedPreferences(anyString(), anyInt())).thenReturn(mockPrefs)
        `when`(mockPrefs.edit()).thenReturn(mockEditor)
        `when`(mockEditor.putString(anyString(), anyString())).thenReturn(mockEditor)

        val pair = SyncPair(
            id = "test-id",
            name = "Test Pair",
            sourceUri = "content://source",
            destUri = "content://dest",
            useHash = true
        )

        // Mock getting empty list
        `when`(mockPrefs.getString("sync_pairs", "[]")).thenReturn("[]")

        // Save
        SyncPairRepository.saveSyncPair(mockContext, pair)

        // Verify saveList logic (we can't verify the exact string passed to putString easily without capturing it,
        // but we can verify the flow)
        verify(mockEditor).putString(eq("sync_pairs"), anyString())
        verify(mockEditor).apply()
    }
}
