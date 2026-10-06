package com.example.ui.relations

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.RelationLink
import com.example.data.registry.CharacterRegistry
import com.example.data.relationship.repository.RelationshipStateRepository
import com.example.ui.designsystem.AiluaScreenScaffold
import com.example.ui.designsystem.CharacterDisplayNames
import com.example.ui.designsystem.CharacterPortrait
import com.example.ui.designsystem.publicCharacterName
import com.example.ui.designsystem.PortraitVariant
import com.example.ui.themeengine.LocalAiluaTheme

@Composable
fun RelationsScreen(
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
    onBack: () -> Unit = {},
    onGoHome: () -> Unit = onBack,
) {
    val theme = LocalAiluaTheme.current
    val states by RelationshipStateRepository.states.collectAsStateWithLifecycle()
    val relations = states.map { state ->
        RelationLink(
            id = "${state.fromCharacterId}_${state.toCharacterId}",
            fromCharacterId = state.fromCharacterId,
            toCharacterId = state.toCharacterId,
            fromName = if (state.fromCharacterId == "user") "你" else CharacterRegistry.getCharacter(state.fromCharacterId).name,
            toName = if (state.toCharacterId == "user") "你" else CharacterRegistry.getCharacter(state.toCharacterId).name,
            relationshipLabel = when (state.stage) {
                com.example.data.relationship.model.RelationshipStage.STRANGER -> "陌生"
                com.example.data.relationship.model.RelationshipStage.ACQUAINTANCE -> "初识"
                com.example.data.relationship.model.RelationshipStage.FAMILIAR -> "熟悉"
                com.example.data.relationship.model.RelationshipStage.CLOSE -> "亲近"
                com.example.data.relationship.model.RelationshipStage.INTIMATE -> "亲密"
                com.example.data.relationship.model.RelationshipStage.STRAINED -> "紧张"
            },
            closeness = state.affinity,
            recentInteraction = state.recentInteraction?.let {
                "${CharacterDisplayNames.projectLegacyMentions(it)} · ${state.lastMeaningfulInteractionAt ?: state.updatedAt}"
            } ?: "尚无共同互动",
            sharedMemory = state.sharedMemory?.let(CharacterDisplayNames::projectLegacyMentions)
                ?: "尚无共同记忆（共 ${state.sharedMemoryCount} 条）",
        )
    }


    AiluaScreenScaffold(
        title = "关系",
        onBack = onBack,
        onGoHome = onGoHome,
        backTestTag = "relations_back_btn",
        modifier = Modifier.testTag("relations_screen"),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                horizontal = theme.layout.screenHorizontalPadding.dp,
                vertical = theme.layout.itemGap.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(theme.layout.sectionGap.dp),
        ) {
            if (relations.isEmpty()) {
                item {
                    Text("还没有共同的互动记录", style = theme.text.body, color = theme.palette.onSurfaceMuted)
                }
            }
            items(relations, key = { it.id }) { relation ->
                RelationItem(relation)
            }
        }
    }
}

@Composable
private fun RelationItem(relation: RelationLink) {
    val theme = LocalAiluaTheme.current
    Column(
        modifier = Modifier.fillMaxWidth().testTag("relation_card_${relation.id}"),
        verticalArrangement = Arrangement.spacedBy(theme.layout.itemGap.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            RelationPerson(relation.fromCharacterId, relation.fromName, Modifier.weight(1f))
            HorizontalDivider(Modifier.weight(0.7f), color = theme.surfaces.divider)
            RelationPerson(relation.toCharacterId, relation.toName, Modifier.weight(1f))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(relation.relationshipLabel, style = theme.text.section, color = theme.palette.onSurface)
            Text("亲密度 ${relation.closeness}%", style = theme.text.caption, color = theme.palette.onSurfaceMuted)
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("最近互动", style = theme.text.caption, color = theme.palette.onSurfaceMuted)
            Text(relation.recentInteraction, style = theme.text.body, color = theme.palette.onSurface)
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("共同记忆", style = theme.text.caption, color = theme.palette.onSurfaceMuted)
            Text(relation.sharedMemory, style = theme.text.body, color = theme.palette.onSurface)
        }
        HorizontalDivider(Modifier.padding(top = theme.layout.itemGap.dp), color = theme.surfaces.divider)
    }
}

@Composable
private fun RelationPerson(characterId: String, name: String, modifier: Modifier = Modifier) {
    val theme = LocalAiluaTheme.current
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (characterId == "user") {
            Box(
                modifier = Modifier.size(48.dp).clip(CircleShape).background(theme.surfaces.inset),
                contentAlignment = Alignment.Center,
            ) {
                Text("你", style = theme.text.body, color = theme.palette.onSurface)
            }
        } else {
            CharacterPortrait(characterId, PortraitVariant.AVATAR, Modifier.size(48.dp))
        }
        Text(publicCharacterName(characterId, name), style = theme.text.body, color = theme.palette.onSurface)
    }
}
