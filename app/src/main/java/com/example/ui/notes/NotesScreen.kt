package com.example.ui.notes

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.notes.Memo
import com.example.data.notes.MemoDraft
import com.example.data.notes.MemoRepository
import com.example.ui.designsystem.AiluaScreenScaffold
import com.example.ui.themeengine.LocalAiluaTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Personal notes, intentionally empty until the user writes one. */
@Composable
fun NotesScreen(
    onBack: () -> Unit = {},
    onGoHome: () -> Unit = onBack,
) {
    val context = LocalContext.current
    val repository = remember(context) { MemoRepository.from(context) }
    val notes by repository.notes.collectAsStateWithLifecycle()
    val storageError by repository.storageError.collectAsStateWithLifecycle()
    val draftWriteFailed by repository.draftWriteError.collectAsStateWithLifecycle()
    val theme = LocalAiluaTheme.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    val restoredDraft = remember(repository) { repository.currentDraft() }
    val restoredNote = remember(repository, restoredDraft) {
        notes.firstOrNull { it.id == restoredDraft?.editingId }
    }
    var query by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf(restoredDraft != null) }
    var editingId by remember { mutableStateOf(restoredNote?.id) }
    var title by remember { mutableStateOf(restoredDraft?.title.orEmpty()) }
    var body by remember { mutableStateOf(restoredDraft?.body.orEmpty()) }
    var originalTitle by remember { mutableStateOf(restoredNote?.title.orEmpty()) }
    var originalBody by remember { mutableStateOf(restoredNote?.body.orEmpty()) }
    var saving by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var confirmDiscard by remember { mutableStateOf(false) }
    var leaveToHome by remember { mutableStateOf(false) }

    fun open(note: Memo?) {
        editingId = note?.id
        title = note?.title.orEmpty()
        body = note?.body.orEmpty()
        originalTitle = title
        originalBody = body
        editing = true
        repository.submitDraft(MemoDraft(editingId, title, body))
    }

    fun finishEditor(home: Boolean) {
        saving = true
        val draftToRetry = MemoDraft(editingId, title, body)
        repository.submitDraft(null)
        scope.launch {
            val cleared = repository.flushDraft()
            saving = false
            if (cleared) {
                editing = false
                if (home) onGoHome()
            } else {
                repository.submitDraft(draftToRetry)
                snackbar.showSnackbar("草稿清理失败，请重试")
            }
        }
    }

    fun leaveEditor(home: Boolean) {
        if (!editing) {
            if (home) onGoHome() else onBack()
        } else if (saving) {
            // Keep the editor visible until storage confirms the result.
        } else if (title != originalTitle || body != originalBody) {
            leaveToHome = home
            confirmDiscard = true
        } else {
            finishEditor(home)
        }
    }

    BackHandler(enabled = editing) { leaveEditor(home = false) }

    AiluaScreenScaffold(
        title = if (editing) if (editingId == null) "新备忘录" else "编辑备忘录" else "备忘录",
        onBack = { leaveEditor(home = false) },
        onGoHome = { leaveEditor(home = true) },
        trailing = {
            if (editing) {
                if (editingId != null) {
                    IconButton(onClick = { confirmDelete = true }, enabled = !saving && !storageError) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "删除备忘录", tint = theme.palette.onSurfaceMuted)
                    }
                }
                TextButton(onClick = {
                    if (title.isBlank() && body.isBlank()) {
                        scope.launch { snackbar.showSnackbar("写一点内容后再保存") }
                    } else {
                        saving = true
                        scope.launch {
                            val saved = repository.save(editingId, title, body)
                            if (saved == null) {
                                saving = false
                                snackbar.showSnackbar("保存失败，内容仍留在编辑页")
                            } else {
                                editingId = saved.id
                                title = saved.title
                                body = saved.body
                                originalTitle = title
                                originalBody = body
                                repository.submitDraft(null)
                                val cleared = repository.flushDraft()
                                saving = false
                                if (cleared) {
                                    editing = false
                                    snackbar.showSnackbar("已保存到本机")
                                } else {
                                    repository.submitDraft(MemoDraft(editingId, title, body))
                                    snackbar.showSnackbar("已保存，但草稿清理失败；请重试返回")
                                }
                            }
                        }
                    }
                }, enabled = !saving && !storageError) {
                    Text(if (saving) "保存中" else "保存", style = theme.text.secondary, color = theme.palette.accent)
                }
            } else {
                TextButton(onClick = { open(null) }, enabled = !storageError) {
                    Text("新建", style = theme.text.secondary, color = theme.palette.accent)
                }
            }
        },
    ) {
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                if (storageError) {
                    Text(
                        "本机备忘录数据无法读取。为保护原内容，已暂停保存与删除。",
                        style = theme.text.secondary,
                        color = theme.palette.accent,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = theme.layout.screenHorizontalPadding.dp, vertical = 12.dp),
                    )
                }
                if (editing && draftWriteFailed) {
                    Text(
                        "草稿暂存失败，请使用“保存”写入备忘录。",
                        style = theme.text.secondary,
                        color = theme.palette.accent,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = theme.layout.screenHorizontalPadding.dp, vertical = 12.dp),
                    )
                }
                if (editing) {
                    MemoEditor(
                        title = title,
                        onTitleChange = {
                            title = it
                            repository.submitDraft(MemoDraft(editingId, title, body))
                        },
                        body = body,
                        onBodyChange = {
                            body = it
                            repository.submitDraft(MemoDraft(editingId, title, body))
                        },
                        enabled = !saving,
                    )
                } else {
                    MemoList(
                        notes = notes,
                        query = query,
                        onQueryChange = { query = it },
                        onOpen = ::open,
                        onNew = { open(null) },
                    )
                }
            }
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
        }
    }

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text("放弃未保存的修改？") },
            text = { Text("修改还没有写入本机。") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDiscard = false
                    finishEditor(leaveToHome)
                }) { Text("放弃修改") }
            },
            dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text("继续编辑") } },
        )
    }

    if (confirmDelete && editingId != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("删除这条备忘录？") },
            text = { Text("删除后无法恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    val id = editingId ?: return@TextButton
                    confirmDelete = false
                    saving = true
                    scope.launch {
                        val deleted = repository.delete(id)
                        if (deleted) {
                            editingId = null
                            title = ""
                            body = ""
                            originalTitle = ""
                            originalBody = ""
                            repository.submitDraft(null)
                            val cleared = repository.flushDraft()
                            saving = false
                            if (cleared) {
                                editing = false
                                snackbar.showSnackbar("已删除")
                            } else {
                                repository.submitDraft(MemoDraft(null, "", ""))
                                snackbar.showSnackbar("已删除，但草稿清理失败；请重试返回")
                            }
                        } else {
                            saving = false
                            snackbar.showSnackbar("删除失败，备忘录仍保留")
                        }
                    }
                }) { Text("删除") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("取消") } },
        )
    }
}

@Composable
private fun MemoList(
    notes: List<Memo>,
    query: String,
    onQueryChange: (String) -> Unit,
    onOpen: (Memo) -> Unit,
    onNew: () -> Unit,
) {
    val theme = LocalAiluaTheme.current
    val filtered = remember(notes, query) {
        val term = query.trim()
        if (term.isEmpty()) notes else notes.filter {
            it.title.contains(term, ignoreCase = true) || it.body.contains(term, ignoreCase = true)
        }
    }
    Column(Modifier.fillMaxSize().padding(horizontal = theme.layout.screenHorizontalPadding.dp)) {
        Row(
            Modifier.fillMaxWidth().background(theme.surfaces.inset, RoundedCornerShape(theme.shapes.medium.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(Icons.Default.Search, contentDescription = null, tint = theme.palette.onSurfaceMuted)
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = theme.text.body.copy(color = theme.palette.onSurface),
                cursorBrush = SolidColor(theme.palette.accent),
                modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    Box {
                        if (query.isEmpty()) Text("搜索标题或正文", style = theme.text.body, color = theme.palette.onSurfaceMuted)
                        inner()
                    }
                },
            )
            if (query.isNotEmpty()) IconButton(onClick = { onQueryChange("") }) {
                Icon(Icons.Default.Close, contentDescription = "清除搜索", tint = theme.palette.onSurfaceMuted)
            }
        }
        Spacer(Modifier.height(theme.layout.itemGap.dp))
        when {
            notes.isEmpty() -> {
                Column(
                    Modifier.fillMaxWidth().padding(top = 72.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("还没有备忘录", style = theme.text.section, color = theme.palette.onSurface)
                    Text("把想记的事情放在这里", style = theme.text.secondary, color = theme.palette.onSurfaceMuted)
                    TextButton(onClick = onNew) { Text("写一条", color = theme.palette.accent) }
                }
            }
            filtered.isEmpty() -> Text(
                "没有找到相关备忘录", style = theme.text.body, color = theme.palette.onSurfaceMuted,
                modifier = Modifier.padding(top = theme.layout.sectionGap.dp),
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = theme.layout.sectionGap.dp),
            ) {
                items(filtered, key = { it.id }) { note ->
                    Column(
                        Modifier.fillMaxWidth().clickable { onOpen(note) }
                            .padding(vertical = 18.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            note.title.ifBlank { note.body.lineSequence().firstOrNull().orEmpty() },
                            style = theme.text.section,
                            color = theme.palette.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (note.title.isNotBlank() && note.body.isNotBlank()) {
                            Text(note.body, style = theme.text.secondary, color = theme.palette.onSurfaceMuted,
                                maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                        Text(formatMemoDate(note.updatedAt), style = theme.text.caption, color = theme.palette.onSurfaceMuted)
                    }
                    HorizontalDivider(color = theme.surfaces.divider)
                }
            }
        }
    }
}

@Composable
private fun MemoEditor(
    title: String,
    onTitleChange: (String) -> Unit,
    body: String,
    onBodyChange: (String) -> Unit,
    enabled: Boolean,
) {
    val theme = LocalAiluaTheme.current
    Column(
        Modifier.fillMaxSize().padding(horizontal = theme.layout.screenHorizontalPadding.dp),
        verticalArrangement = Arrangement.spacedBy(theme.layout.itemGap.dp),
    ) {
        BasicTextField(
            value = title,
            onValueChange = onTitleChange,
            singleLine = true,
            readOnly = !enabled,
            textStyle = theme.text.title.copy(color = theme.palette.onSurface),
            cursorBrush = SolidColor(theme.palette.accent),
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            decorationBox = { inner ->
                Box {
                    if (title.isEmpty()) Text("标题", style = theme.text.title, color = theme.palette.onSurfaceMuted)
                    inner()
                }
            },
        )
        HorizontalDivider(color = theme.surfaces.divider)
        BasicTextField(
            value = body,
            onValueChange = onBodyChange,
            readOnly = !enabled,
            textStyle = theme.text.body.copy(color = theme.palette.onSurface),
            cursorBrush = SolidColor(theme.palette.accent),
            modifier = Modifier.fillMaxWidth().weight(1f).padding(vertical = 12.dp),
            decorationBox = { inner ->
                Box {
                    if (body.isEmpty()) Text("写下想记的事……", style = theme.text.body, color = theme.palette.onSurfaceMuted)
                    inner()
                }
            },
        )
    }
}

private fun formatMemoDate(timestamp: Long): String =
    SimpleDateFormat("M月d日 HH:mm", Locale.getDefault()).format(Date(timestamp))
