package com.example

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import com.example.data.notes.MemoDraft
import com.example.data.notes.MemoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MemoRepositoryTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun createEditSearchDeleteSurviveRepositoryRestart() = runBlocking {
        val prefs = context.getSharedPreferences("memo_test_lifecycle", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        val first = MemoRepository(prefs, now = { 100L })
        val saved = first.save(null, "周末计划", "买花\n给朋友写信")
        assertNotNull(saved)
        assertEquals(1, first.search("买花").size)
        assertTrue(first.search("不存在").isEmpty())

        val reopened = MemoRepository(prefs, now = { 200L })
        assertEquals("周末计划", reopened.notes.value.single().title)
        val changed = reopened.save(saved!!.id, "周末计划", "买花和咖啡")
        assertEquals(saved.id, changed?.id)
        assertEquals(100L, changed?.createdAt)
        assertEquals(200L, changed?.updatedAt)

        val reopenedAgain = MemoRepository(prefs)
        assertEquals("买花和咖啡", reopenedAgain.search("咖啡").single().body)
        assertTrue(reopenedAgain.delete(saved.id))
        assertTrue(MemoRepository(prefs).notes.value.isEmpty())
    }

    @Test
    fun blankMemoIsNotStoredAndUnknownIdCannotCreateDuplicate() = runBlocking {
        val prefs = context.getSharedPreferences("memo_test_invalid", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        val repository = MemoRepository(prefs)
        assertNull(repository.save(null, "  ", "\n "))
        assertNull(repository.save("missing", "内容", "正文"))
        assertTrue(repository.notes.value.isEmpty())
    }

    @Test
    fun unreadableDataIsNeverOverwritten() = runBlocking {
        val prefs = context.getSharedPreferences("memo_test_corrupt", Context.MODE_PRIVATE)
        prefs.edit().clear().putString("notes_v1", "{broken").commit()
        val repository = MemoRepository(prefs)
        assertTrue(repository.storageError.value)
        assertNull(repository.save(null, "安全检查", "不应覆盖"))
        assertFalse(repository.delete("unknown"))
        assertEquals("{broken", prefs.getString("notes_v1", null))
    }

    @Test
    fun largeUnsavedDraftLivesInPrivateStoreAndRestoresAfterRepositoryRestart() {
        val prefs = context.getSharedPreferences("memo_test_draft", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        val text = "长".repeat(300_000)
        val first = MemoRepository(prefs)
        first.submitDraft(MemoDraft(null, "长草稿", text))
        assertEquals(text, first.currentDraft()?.body)
        runBlocking { assertTrue(first.flushDraft()) }
        assertEquals(text, MemoRepository(prefs).readDraft()?.body)
        assertTrue(MemoRepository(prefs).notes.value.isEmpty())
        first.submitDraft(null)
        runBlocking { assertTrue(first.flushDraft()) }
        assertNull(MemoRepository(prefs).readDraft())
    }

    @Test
    fun clearWinsOverConcurrentOlderDraftWrite() = runBlocking {
        val prefs = context.getSharedPreferences("memo_test_draft_order", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        val repository = MemoRepository(prefs)
        repository.submitDraft(MemoDraft(null, "旧草稿", "旧内容"))
        val earlierFlush = async(Dispatchers.IO) { repository.flushDraft() }
        repository.submitDraft(null)
        assertTrue(repository.flushDraft())
        earlierFlush.await()
        delay(250) // The debounced writer must not bring the old draft back.
        assertNull(MemoRepository(prefs).readDraft())
    }

    @Test
    fun failedDiskCommitIsReportedWithoutClaimingDraftPersisted() = runBlocking {
        val backing = context.getSharedPreferences("memo_test_draft_commit", Context.MODE_PRIVATE)
        backing.edit().clear().commit()
        val failing = object : SharedPreferences by backing {
            override fun edit(): SharedPreferences.Editor {
                val delegate = backing.edit()
                return object : SharedPreferences.Editor by delegate {
                    override fun commit(): Boolean = false
                }
            }
        }
        val repository = MemoRepository(failing)
        repository.submitDraft(MemoDraft(null, "未落盘", "内容"))
        assertFalse(repository.flushDraft())
        assertTrue(repository.draftWriteError.value)
        assertNull(backing.getString("draft_v1", null))
    }
}
