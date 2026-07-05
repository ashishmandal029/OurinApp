package com.example.ui

import android.content.Intent
import android.net.Uri
import kotlin.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.MainViewModel
import com.example.api.Comment
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@OptIn(UnstableApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun NativePlayerView(
    videoUrl: String,
    watchUrl: String,
    title: String,
    seriesName: String?,
    seasonNumber: Int?,
    episodeNumber: Int?,
    quality: String?,
    format: String?,
    uploader: String?,
    duration: Long?,
    expiresAt: Long?,
    viewModel: MainViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val testVideoId = remember(watchUrl, title) {
        // Stable video ID hash
        val rawId = "$watchUrl|$title|$seriesName|$seasonNumber|$episodeNumber"
        java.security.MessageDigest.getInstance("SHA-256")
            .digest(rawId.toByteArray())
            .joinToString("") { "%02x".format(it) }
            .take(16)
    }

    // Load dynamic data (Comments, Likes)
    LaunchedEffect(testVideoId) {
        viewModel.loadComments(testVideoId)
        viewModel.loadLikes(testVideoId)
    }

    val comments by viewModel.comments.collectAsState()
    val likesCount by viewModel.likesCount.collectAsState()
    val isLiked by viewModel.isLiked.collectAsState()

    var playbackSpeed by remember { mutableStateOf(1.0f) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showCommentSheet by remember { mutableStateOf(false) }

    // ExoPlayer State Management
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri(Uri.parse(videoUrl))
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true
        }
    }

    LaunchedEffect(playbackSpeed) {
        exoPlayer.playbackParameters = PlaybackParameters(playbackSpeed)
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = CosmicText,
                            maxLines = 1
                        )
                        if (!seriesName.isNullOrEmpty()) {
                            Text(
                                text = seriesName + (episodeNumber?.let { " - Ep $it" } ?: ""),
                                style = MaterialTheme.typography.bodySmall,
                                color = NeonCyan
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = CosmicText
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CosmicBg
                )
            )
        },
        containerColor = CosmicBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 1. AndroidView wrapping ExoPlayer PlayerView
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(Color.Black)
            ) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = true
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // 2. Scrollable Detail & Interaction Pane
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Title and Meta
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = CosmicText
                )

                // Badges Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!quality.isNullOrEmpty()) {
                        BadgeCard(text = quality, color = NeonGreen)
                    }
                    if (!format.isNullOrEmpty()) {
                        BadgeCard(text = format, color = NeonCyan)
                    }
                    if (playbackSpeed != 1.0f) {
                        BadgeCard(text = "${playbackSpeed}x Speed", color = CosmicRed)
                    }
                }

                // Action Row (Like, Comment, Speed, Download, Share)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ActionButton(
                        icon = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        label = if (likesCount > 0) "Like ($likesCount)" else "Like",
                        isActive = isLiked,
                        tag = "like_button",
                        onClick = { viewModel.toggleLike(testVideoId) }
                    )

                    ActionButton(
                        icon = Icons.Default.Comment,
                        label = if (comments.isNotEmpty()) "Comment (${comments.size})" else "Comment",
                        isActive = showCommentSheet,
                        tag = "comment_button",
                        onClick = { showCommentSheet = true }
                    )

                    ActionButton(
                        icon = Icons.Default.Speed,
                        label = "${playbackSpeed}x",
                        isActive = playbackSpeed != 1.0f,
                        tag = "speed_button",
                        onClick = { showSpeedDialog = true }
                    )

                    ActionButton(
                        icon = Icons.Default.FileDownload,
                        label = "Save",
                        isActive = false,
                        tag = "save_button",
                        onClick = {
                            viewModel.downloadToDevice(context, videoUrl, title)
                        }
                    )

                    ActionButton(
                        icon = Icons.Default.Share,
                        label = "Share",
                        isActive = false,
                        tag = "share_button",
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, watchUrl)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Watch Link"))
                        }
                    )
                }

                HorizontalDivider(color = CosmicMuted.copy(alpha = 0.3f))

                // Metadata Details Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = CosmicCard),
                    border = BorderStroke(1.dp, CosmicBorder),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "// VIDEO METADATA",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = NeonGreen,
                            fontWeight = FontWeight.Bold
                        )

                        if (!uploader.isNullOrEmpty()) {
                            MetadataRow(label = "Uploader", value = uploader, valueColor = NeonGreen)
                        }
                        if (duration != null && duration > 0) {
                            val durationFormatted = remember(duration) {
                                val min = duration / 60
                                val sec = duration % 60
                                "$min:${sec.toString().padStart(2, '0')}"
                            }
                            MetadataRow(label = "Duration", value = durationFormatted, valueColor = NeonCyan)
                        }
                        if (expiresAt != null) {
                            var timeLeft by remember { mutableStateOf("") }
                            LaunchedEffect(expiresAt) {
                                while (true) {
                                    val now = System.currentTimeMillis() / 1000
                                    val diff = expiresAt - now
                                    if (diff <= 0) {
                                        timeLeft = "Expired"
                                        break
                                    } else {
                                        val mins = diff / 60
                                        timeLeft = "$mins min left"
                                    }
                                    delay(30000)
                                }
                            }
                            MetadataRow(label = "Link Expires", value = timeLeft, valueColor = CosmicRed)
                        }
                    }
                }

                // Interactive Inline Comment Preview
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Comments (${comments.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = CosmicText
                        )
                        Text(
                            text = "View All",
                            style = MaterialTheme.typography.bodyMedium,
                            color = NeonGreen,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable { showCommentSheet = true }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (comments.isEmpty()) {
                        Text(
                            text = "No comments yet. Write the first comment!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = CosmicMuted
                        )
                    } else {
                        comments.take(3).forEach { comment ->
                            CommentRowItem(comment)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }

    // Playback Speed Dialog
    if (showSpeedDialog) {
        Dialog(onDismissRequest = { showSpeedDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = CosmicCard),
                border = BorderStroke(1.dp, CosmicBorder),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Playback Speed",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = CosmicText,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
                    speeds.forEach { speed ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    playbackSpeed = speed
                                    showSpeedDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (speed == 1.0f) "Normal (1x)" else "${speed}x",
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (playbackSpeed == speed) NeonGreen else CosmicText,
                                fontWeight = if (playbackSpeed == speed) FontWeight.Bold else FontWeight.Normal
                            )
                            if (playbackSpeed == speed) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = NeonGreen
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Real-time Comments Bottom Sheet
    if (showCommentSheet) {
        Dialog(onDismissRequest = { showCommentSheet = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = CosmicCard),
                border = BorderStroke(1.dp, CosmicBorder),
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Comments (${comments.size})",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = CosmicText
                        )
                        IconButton(onClick = { showCommentSheet = false }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = CosmicText
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (comments.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(48.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No comments yet.",
                                        color = CosmicMuted,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }
                        } else {
                            items(comments) { comment ->
                                CommentRowItem(comment)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Comment Entry Input
                    var commentText by remember { mutableStateOf("") }
                    val commenterName by viewModel.usernameState.collectAsState()

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .imePadding(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextField(
                            value = commentText,
                            onValueChange = { commentText = it },
                            placeholder = { Text("Comment as $commenterName...") },
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(24.dp))
                                .testTag("comment_input"),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = CosmicBg,
                                unfocusedContainerColor = CosmicBg,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            maxLines = 3,
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Send
                            ),
                            keyboardActions = KeyboardActions(
                                onSend = {
                                    if (commentText.isNotBlank()) {
                                        viewModel.postComment(testVideoId, commentText)
                                        commentText = ""
                                    }
                                }
                            )
                        )

                        IconButton(
                            onClick = {
                                if (commentText.isNotBlank()) {
                                    viewModel.postComment(testVideoId, commentText)
                                    commentText = ""
                                }
                            },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(CosmicRed)
                                .testTag("post_comment_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Post",
                                tint = Color.Black
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CommentRowItem(comment: Comment) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CosmicBg.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = comment.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = NeonGreen
                )
                
                val formattedTime = remember(comment.ts) {
                    val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                    sdf.format(Date(comment.ts * 1000))
                }
                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.labelSmall,
                    color = CosmicMuted,
                    fontFamily = FontFamily.Monospace
                )
            }

            Text(
                text = comment.text,
                style = MaterialTheme.typography.bodyMedium,
                color = CosmicText
            )
        }
    }
}

@Composable
fun BadgeCard(text: String, color: Color) {
    Box(
        modifier = Modifier
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.05f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = text.uppercase(),
            color = color,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isActive: Boolean,
    tag: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(8.dp)
            .testTag(tag)
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(if (isActive) CosmicRed else CosmicRed.copy(alpha = 0.12f))
                .border(1.5.dp, CosmicRed.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive) Color.Black else CosmicRed,
                modifier = Modifier.size(20.dp)
            )
        }
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = CosmicMuted,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun MetadataRow(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = CosmicMuted,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(96.dp)
        )
        Text(
            text = value,
            color = valueColor,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
    }
}
