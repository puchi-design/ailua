package com.example.ui.moments

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.engine.WorldHeartbeatEngine
import com.example.data.engine.WorldStateRepository
import com.example.data.model.MomentPost
import com.example.data.model.MomentComment
import com.example.data.mock.MockData
import com.example.data.registry.CharacterRegistry
import com.example.data.projection.projectMoments
import com.example.data.relationship.repository.RelationshipStateRepository
import com.example.data.social.SocialReactionStore
import com.example.data.social.withUserLike
import com.example.ui.designsystem.*
import com.example.ui.themeengine.LocalAiluaTheme

private const val MAX_MOMENT_COMMENT_LENGTH = 160

@Composable
fun MomentsScreen(
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
    onBackToHome: () -> Unit = {},
    onOpenProfile: (String) -> Unit = {},
    onGoHome: () -> Unit = onBackToHome,
) {
    val worldEvents by WorldStateRepository.events.collectAsStateWithLifecycle()
    val seedPosts = remember { MockData.getMomentsFromLifeEvents() }
    val projectedPosts = remember(worldEvents) { projectMoments(seedPosts, worldEvents) }
    val context = LocalContext.current
    val reactions = remember(context.applicationContext) { SocialReactionStore.create(context) }
    val momentOverrides by reactions.momentOverrides.collectAsStateWithLifecycle()
    // Comments remain sourced from the persisted LifeEvent ledger.
    val posts = projectedPosts.map { post ->
        val comments = worldEvents.filter { it.sourceAppId == "moments" && it.sourceRefId == post.id && it.title == "你评论了动态" }
            .map { MomentComment(id = it.id, author = "你", isUser = true, content = it.description, timestamp = it.time) }
        post.withUserLike(momentOverrides[post.id]).copy(comments = post.comments + comments)
    }
    var selectedFilter by remember { mutableStateOf("全部") }
    val filterOptions = listOf("全部" to "全部") + CharacterRegistry.getAllCharacters().map { it.id to it.name }
    val filteredPosts = if (selectedFilter == "全部") posts else posts.filter { it.authorId == selectedFilter }

    val theme = LocalAiluaTheme.current
    AiluaScreenScaffold(
        title = "动态", onBack = onBackToHome, onGoHome = onGoHome,
        modifier = Modifier.imePadding().testTag("moments_screen"), backTestTag = "moments_back_btn",
    ) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = theme.layout.screenHorizontalPadding.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        ) {
            items(filterOptions) { filter ->
                AiluaChip(filter.second, selected = selectedFilter == filter.first, onClick = { selectedFilter = filter.first })
            }
        }
        LazyColumn(Modifier.weight(1f).fillMaxWidth().padding(horizontal = theme.layout.screenHorizontalPadding.dp)) {
            items(filteredPosts, key = { it.id }) { post ->
                MomentCard(post, onOpenProfile,
                    onToggleLike = {
                        projectedPosts.firstOrNull { it.id == post.id }?.let { original ->
                            reactions.toggleMomentLike(original.id, original.isLiked)
                        }
                    },
                    onAddComment = { newCommentText ->
                        val current = posts.firstOrNull { it.id == post.id }
                        if (current != null) {
                            val newComment = MomentComment(id = "c_${System.currentTimeMillis()}", author = "你",
                                isUser = true, content = newCommentText, timestamp = "刚刚")
                            val worldClock = WorldHeartbeatEngine.worldClock.value
                            RelationshipStateRepository.recordMomentComment(current.authorId, current.id, newComment.id,
                                newCommentText, worldClock.dateLabel, worldClock.timeFormatted)
                        }
                    })
            }
        }
    }
}

@Composable
private fun MomentCard(
    post: MomentPost, onOpenProfile: (String) -> Unit, onToggleLike: () -> Unit, onAddComment: (String) -> Unit,
) {
    val theme = LocalAiluaTheme.current
    var showCommentInput by remember { mutableStateOf(false) }
    var commentText by remember { mutableStateOf("") }
    val commentToSend = commentText.trim()
    val commentTooLong = commentToSend.length > MAX_MOMENT_COMMENT_LENGTH
    val canSendComment = commentToSend.isNotEmpty() && !commentTooLong
    val authorName = publicCharacterName(post.authorId, post.authorName)
    Column(
        Modifier.fillMaxWidth().testTag("moment_card_${post.id}").padding(top = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CharacterPortrait(post.authorId, PortraitVariant.AVATAR, Modifier.size(40.dp),
                onClick = { onOpenProfile(post.authorId) })
            Column(Modifier.weight(1f).clickable { onOpenProfile(post.authorId) }) {
                Text(authorName, style = theme.text.body, color = theme.palette.onSurface)
                Text(listOf(post.locationContext, post.moodTag).filter { it.isNotBlank() }.joinToString(" · "),
                    style = theme.text.caption, color = theme.palette.onSurfaceMuted,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(post.timestamp, style = theme.text.caption, color = theme.palette.onSurfaceMuted)
        }
        Text(if (post.authorId in com.example.ui.designsystem.CharacterDisplayNames.officialIds)
            com.example.ui.designsystem.CharacterDisplayNames.projectLegacyMentions(post.content) else post.content,
            style = theme.text.body, color = theme.palette.onSurface)
        if (post.imageType.isNotBlank()) MomentVisualCard(post.imageType)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(onClick = onToggleLike) {
                Icon(if (post.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "点赞",
                    Modifier.size(18.dp), tint = if (post.isLiked) theme.palette.accent else theme.palette.onSurfaceMuted)
                Spacer(Modifier.width(6.dp))
                Text("喜欢 ${post.likesCount}", style = theme.text.secondary, color = theme.palette.onSurfaceMuted)
            }
            TextButton(onClick = { showCommentInput = !showCommentInput }) {
                Icon(Icons.Default.ChatBubbleOutline, "评论", Modifier.size(18.dp), tint = theme.palette.onSurfaceMuted)
                Spacer(Modifier.width(6.dp))
                Text("评论 ${post.comments.size}", style = theme.text.secondary, color = theme.palette.onSurfaceMuted)
            }
        }
        if (post.comments.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                post.comments.forEach { comment ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        val commentAuthor = if (comment.isUser) comment.author else publicSnapshotAuthorName(comment.author)
                        val commentContent = if (comment.isUser) comment.content else
                            com.example.ui.designsystem.CharacterDisplayNames.projectLegacyMentions(comment.content)
                        Text("$commentAuthor：$commentContent", style = theme.text.secondary,
                            color = theme.palette.onSurfaceMuted, modifier = Modifier.weight(1f))
                        Text(comment.timestamp, style = theme.text.caption, color = theme.palette.onSurfaceMuted)
                    }
                }
            }
        }
        if (showCommentInput) {
            TextField(
                value = commentText, onValueChange = { commentText = it }, singleLine = true,
                modifier = Modifier.fillMaxWidth(), textStyle = theme.text.body,
                placeholder = { Text("写下对 $authorName 的回应…", style = theme.text.secondary) },
                isError = commentTooLong,
                supportingText = {
                    Text(
                        if (commentTooLong) "最多 160 字，请删减后发送（${commentToSend.length}/160）"
                        else "${commentToSend.length}/160 字",
                        style = theme.text.caption,
                    )
                },
                shape = RoundedCornerShape(theme.shapes.medium.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = theme.surfaces.inset, unfocusedContainerColor = theme.surfaces.inset,
                    focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                ),
            )
            TextButton(
                onClick = {
                    if (canSendComment) {
                        onAddComment(commentToSend)
                        commentText = ""
                        showCommentInput = false
                    }
                },
                modifier = Modifier.testTag("moment_send_comment"),
                enabled = canSendComment,
            ) { Text("发送评论", style = theme.text.secondary) }
        }
        HorizontalDivider(color = theme.surfaces.divider)
    }
}

/** Existing procedural fallback artwork. Its paint colors are illustration data, not UI tokens. */
@Composable
private fun MomentVisualCard(imageType: String) {
    val gradientColors = when (imageType) {
        "rain_window" -> listOf(Color(0xFF869EB5), Color(0xFF6B8399), Color(0xFF536A80))
        "flowers" -> listOf(Color(0xFFE4BCBC), Color(0xFFCCA2A2), Color(0xFFB08686))
        "night_book" -> listOf(Color(0xFF655F7A), Color(0xFF4C4760), Color(0xFF37324B))
        "pudding", "convenience_store", "dessert" -> listOf(Color(0xFFF6C279), Color(0xFFE59443), Color(0xFFB8591D))
        else -> listOf(Color(0xFF8BA5BE), Color(0xFF9E95B8))
    }

    AiluaMediaFrame(Modifier.fillMaxWidth().aspectRatio(1.35f)) {
        Box(Modifier.fillMaxSize().background(Brush.linearGradient(gradientColors))) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            when (imageType) {
                "rain_window" -> {
                    // Rain streaks & warm glow from inside
                    drawCircle(
                        color = Color(0xFFFFF2D9).copy(alpha = 0.35f),
                        center = Offset(w * 0.75f, h * 0.4f),
                        radius = h * 0.5f
                    )
                    for (i in 0..14) {
                        val x = (i * 28f) % w
                        val y = (i * 19f) % (h * 0.8f)
                        drawLine(
                            color = Color.White.copy(alpha = 0.45f),
                            start = Offset(x, y),
                            end = Offset(x - 6f, y + 24f),
                            strokeWidth = 2.5f
                        )
                    }
                }
                "flowers" -> {
                    // Soft white floral petals
                    drawCircle(
                        color = Color(0xFFFFFDF8),
                        center = Offset(w * 0.5f, h * 0.5f),
                        radius = 28.dp.toPx()
                    )
                    drawCircle(
                        color = Color(0xFFFFF9ED),
                        center = Offset(w * 0.43f, h * 0.45f),
                        radius = 22.dp.toPx()
                    )
                    drawCircle(
                        color = Color(0xFFFFF6E4),
                        center = Offset(w * 0.57f, h * 0.45f),
                        radius = 22.dp.toPx()
                    )
                    drawCircle(
                        color = Color(0xFFD6B57E).copy(alpha = 0.7f),
                        center = Offset(w * 0.5f, h * 0.5f),
                        radius = 8.dp.toPx()
                    )
                }
                "pudding", "convenience_store", "dessert" -> {
                    // Glass dish & Caramel Custard Pudding
                    drawCircle(
                        color = Color(0xFFFFF8EA).copy(alpha = 0.4f),
                        center = Offset(w * 0.5f, h * 0.55f),
                        radius = 48.dp.toPx()
                    )
                    // Custard base body
                    drawRoundRect(
                        color = Color(0xFFFFE89E),
                        topLeft = Offset(w * 0.38f, h * 0.38f),
                        size = Size(w * 0.24f, h * 0.36f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(18f, 18f)
                    )
                    // Rich caramel topping
                    drawRoundRect(
                        color = Color(0xFF8F4200),
                        topLeft = Offset(w * 0.38f, h * 0.38f),
                        size = Size(w * 0.24f, h * 0.12f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f)
                    )
                    // Caramel drip highlight
                    drawCircle(
                        color = Color(0xFF8F4200),
                        center = Offset(w * 0.44f, h * 0.54f),
                        radius = 4.dp.toPx()
                    )
                    // Glossy shine
                    drawCircle(
                        color = Color.White.copy(alpha = 0.55f),
                        center = Offset(w * 0.42f, h * 0.42f),
                        radius = 3.dp.toPx()
                    )
                }
                "night_book" -> {
                    // Warm desk lamp glow & open book silhouette
                    drawCircle(
                        color = Color(0xFFFFECC4).copy(alpha = 0.28f),
                        center = Offset(w * 0.3f, h * 0.35f),
                        radius = h * 0.6f
                    )
                    drawRect(
                        color = Color(0xFFEDE8F5).copy(alpha = 0.85f),
                        topLeft = Offset(w * 0.35f, h * 0.55f),
                        size = Size(w * 0.30f, h * 0.28f)
                    )
                }
            }
        }


        }
    }
}
