package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.data.ai.repository.ProviderGraph
import com.example.data.context.CharacterContext
import com.example.data.engine.CallStateEngine
import com.example.data.engine.WorldStateRepository
import com.example.data.firstsession.FirstSessionStore
import com.example.data.local.AiluaLocalStore
import com.example.data.memory.repository.MemoryGraph
import com.example.data.relationship.repository.RelationshipStateRepository
import com.example.ui.themeengine.ThemeStore

/** Debug-only host for testing real UI/data without MainActivity's continuous world ticker. */
class V61QaHostActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val context = applicationContext
        AiluaLocalStore.init(context)
        CharacterContext.init(context)
        FirstSessionStore.init(context)
        ProviderGraph.init(context)
        MemoryGraph.init(context)
        RelationshipStateRepository.restore()
        WorldStateRepository.syncWithLocalStore()
        CallStateEngine.syncWithLocalStore()
        ThemeStore.initialize(context)
        setContent {}
    }
}
