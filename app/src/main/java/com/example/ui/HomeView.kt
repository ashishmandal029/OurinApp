package com.example.ui

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.DownloadJobState
import com.example.FetchState
import com.example.MainViewModel
import com.example.api.DownloadResult
import com.example.api.Format
import com.example.api.VideoInfo
import com.example.ui.theme.*

@Composable
fun HomeView(
    viewModel: MainViewModel,
    onPlayOnline: (DownloadResult) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = remember { context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }

    var videoUrl by remember { mutableStateOf("") }
    val fetchState by viewModel.fetchState.collectAsState()
    val downloadJobState by viewModel.downloadJobState.collectAsState()
    val serverBaseUrl by viewModel.baseUrlState.collectAsState()

    // Format Lists
    val combinedFormats by viewModel.combinedFormats.collectAsState()
    val videoFormats by viewModel.videoFormats.collectAsState()
    val audioFormats by viewModel.audioFormats.collectAsState()

    var selectedTab by remember { mutableStateOf("combined") } // combined, video, audio
    var selectedFormatId by remember { mutableStateOf<String?>(null) }

    // Clear selected format if formats reload
    LaunchedEffect(fetchState) {
        if (fetchState is FetchState.Success) {
            selectedFormatId = null
            // Auto-switch tab to whatever is available
            selectedTab = if (combinedFormats.isNotEmpty()) "combined" else "video"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BentoBg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Bento Style Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(50))
                        .background(BentoPurpleAccent)
                        .clickable { onNavigateToSettings() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Menu",
                        tint = BentoPurpleText,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = "Control Center",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Normal,
                    color = BentoTextPrimary
                )
            }

            // Profile Avatar Chip OD
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(50))
                    .background(BentoAvatarBg),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "OD",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = BentoAvatarText
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Main Search & Input Card (Bento Style)
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, BentoBorder),
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "INPUT VIDEO OR STREAM URL",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = BentoTextMuted
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = videoUrl,
                        onValueChange = { videoUrl = it },
                        placeholder = { Text("Enter Hotstar / JioHotstar URL...", color = BentoTextMuted) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BentoPurpleText,
                            unfocusedBorderColor = BentoBorder,
                            focusedTextColor = BentoTextPrimary,
                            unfocusedTextColor = BentoTextPrimary,
                            focusedContainerColor = BentoBg.copy(alpha = 0.5f),
                            unfocusedContainerColor = BentoBg.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    val clipData = clipboardManager.primaryClip
                                    if (clipData != null && clipData.itemCount > 0) {
                                        val text = clipData.getItemAt(0).text
                                        if (text != null) {
                                            videoUrl = text.toString()
                                        }
                                    }
                                },
                                modifier = Modifier.testTag("pasteBtn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentPaste,
                                    contentDescription = "Paste",
                                    tint = BentoPurpleText
                                )
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("urlInput")
                    )

                    Button(
                        onClick = { viewModel.fetchVideoMetadata(videoUrl) },
                        colors = ButtonDefaults.buttonColors(containerColor = BentoPurpleAccent),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .height(56.dp)
                            .testTag("fetchBtn")
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.Search, contentDescription = "Fetch", tint = BentoPurpleText)
                            Text(
                                text = "FETCH",
                                color = BentoPurpleText,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        // Processing / Dynamic States Viewports
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (val fetch = fetchState) {
                is FetchState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(color = NeonGreen)
                            Text(
                                text = "QUERIED SOURCE METADATA...",
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = FontFamily.Monospace,
                                color = NeonGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                is FetchState.Error -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CosmicCard),
                        border = BorderStroke(1.dp, CosmicRed.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = "Error",
                                tint = CosmicRed,
                                modifier = Modifier.size(32.dp)
                            )
                            Column {
                                Text(
                                    text = "METADATA ERROR",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = CosmicRed
                                )
                                Text(
                                    text = fetch.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CosmicText
                                )
                            }
                        }
                    }
                }
                is FetchState.Success -> {
                    // Preview Card + Selectors Scroll
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 1. Metadata Preview Header
                        item {
                            MetadataPreviewHeader(info = fetch.info)
                        }

                        // 2. Format Category Tabs Selector
                        item {
                            FormatSelectorTabs(
                                combinedCount = combinedFormats.size,
                                videoCount = videoFormats.size,
                                audioCount = audioFormats.size,
                                activeTab = selectedTab,
                                onTabClick = { selectedTab = it }
                            )
                        }

                        // 3. Formats Items Cards List
                        val listToRender = when (selectedTab) {
                            "combined" -> combinedFormats
                            "video" -> videoFormats
                            else -> audioFormats
                        }

                        if (listToRender.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No formats available in this tab.",
                                        color = CosmicMuted,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        } else {
                            items(listToRender) { format ->
                                FormatCardItem(
                                    format = format,
                                    isSelected = selectedFormatId == format.formatId,
                                    onSelect = { selectedFormatId = format.formatId }
                                )
                            }
                        }

                        // 4. Download Trigger Controls Spacer
                        item {
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
                is FetchState.Idle -> {
                    BentoDashboard(
                        onNavigateToSettings = onNavigateToSettings,
                        serverUrl = serverBaseUrl
                    )
                }
            }
        }

        // Sticky Download / Polling Panel Bottom
        AnimatedVisibility(
            visible = selectedFormatId != null || downloadJobState != DownloadJobState.Idle,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CosmicBg)
            ) {
                when (val dState = downloadJobState) {
                    is DownloadJobState.Idle -> {
                        Button(
                            onClick = {
                                selectedFormatId?.let { quality ->
                                    viewModel.startAndPollDownload(videoUrl, quality)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("startDownloadBtn")
                        ) {
                            Text(
                                text = "🚀 START SERVER DOWNLOAD",
                                color = Color.Black,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                    is DownloadJobState.Processing -> {
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
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = dState.label.uppercase(),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontFamily = FontFamily.Monospace,
                                        color = NeonCyan,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "${dState.progress}%",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = NeonGreen,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                LinearProgressIndicator(
                                    progress = { dState.progress / 100f },
                                    modifier = Modifier.fillMaxWidth(),
                                    color = NeonGreen,
                                    trackColor = CosmicMuted.copy(alpha = 0.2f),
                                )
                            }
                        }
                    }
                    is DownloadJobState.Error -> {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CosmicCard),
                            border = BorderStroke(1.dp, CosmicRed.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "SERVER COMPILATION ERROR",
                                    color = CosmicRed,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = dState.message,
                                    color = CosmicText,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Button(
                                    onClick = { viewModel.startAndPollDownload(videoUrl, selectedFormatId ?: "best") },
                                    colors = ButtonDefaults.buttonColors(containerColor = CosmicRed),
                                    modifier = Modifier.align(Alignment.End)
                                ) {
                                    Text(text = "RETRY", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    is DownloadJobState.Success -> {
                        DownloadSuccessResultCard(
                            result = dState.result,
                            onPlayClick = { onPlayOnline(dState.result) },
                            onDownloadLocalClick = {
                                viewModel.downloadToDevice(context, dState.result.url, dState.result.title)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MetadataPreviewHeader(info: VideoInfo) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, BentoBorder),
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Async Image Thumbnail loaded with Coil
            Box(
                modifier = Modifier
                    .size(width = 120.dp, height = 76.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, BentoBorder, RoundedCornerShape(16.dp))
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                if (!info.thumbnail.isNullOrEmpty()) {
                    AsyncImage(
                        model = info.thumbnail,
                        contentDescription = info.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Video preview",
                        tint = CosmicMuted,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Text Details Block
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = info.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = CosmicText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (!info.seriesName.isNullOrEmpty()) {
                    Text(
                        text = info.seriesName + (info.episodeNumber?.let { " - S1E$it" } ?: ""),
                        style = MaterialTheme.typography.bodySmall,
                        color = NeonCyan,
                        fontWeight = FontWeight.SemiBold
                    )
                } else if (!info.uploader.isNullOrEmpty()) {
                    Text(
                        text = "By ${info.uploader}",
                        style = MaterialTheme.typography.bodySmall,
                        color = CosmicMuted
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (info.duration != null && info.duration > 0) {
                        val durationText = remember(info.duration) {
                            val totalSec = info.duration.toLong()
                            val min = totalSec / 60
                            val sec = totalSec % 60
                            "$min:${sec.toString().padStart(2, '0')}"
                        }
                        Text(
                            text = durationText,
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonGreen,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (info.formatsCount != null) {
                        Text(
                            text = "${info.formatsCount} formats",
                            style = MaterialTheme.typography.labelSmall,
                            color = CosmicMuted,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FormatSelectorTabs(
    combinedCount: Int,
    videoCount: Int,
    audioCount: Int,
    activeTab: String,
    onTabClick: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        TabButton(
            label = "Video+Audio",
            count = combinedCount,
            isActive = activeTab == "combined",
            modifier = Modifier.weight(1.3f),
            onClick = { onTabClick("combined") }
        )
        TabButton(
            label = "Video Only",
            count = videoCount,
            isActive = activeTab == "video",
            modifier = Modifier.weight(1f),
            onClick = { onTabClick("video") }
        )
        TabButton(
            label = "Audio Only",
            count = audioCount,
            isActive = activeTab == "audio",
            modifier = Modifier.weight(1f),
            onClick = { onTabClick("audio") }
        )
    }
}

@Composable
fun TabButton(
    label: String,
    count: Int,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(if (isActive) NeonGreen.copy(alpha = 0.1f) else CosmicCard)
            .border(
                1.dp,
                if (isActive) NeonGreen.copy(alpha = 0.35f) else CosmicBorder,
                RoundedCornerShape(4.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label.uppercase(),
                color = if (isActive) NeonGreen else CosmicMuted,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            if (count > 0) {
                Text(
                    text = "($count streams)",
                    color = if (isActive) NeonGreen.copy(alpha = 0.7f) else CosmicMuted.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun FormatCardItem(
    format: Format,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) BentoPurpleAccent.copy(alpha = 0.5f) else Color.White
        ),
        border = BorderStroke(
            1.dp,
            if (isSelected) BentoPurpleText else BentoBorder
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (format.synthetic == true) (format.resolution ?: "audio") + "+audio" else (format.resolution ?: format.formatId),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = CosmicText
                    )
                    
                    if (format.synthetic == true) {
                        Box(
                            modifier = Modifier
                                .background(NeonCyan.copy(alpha = 0.15f), RoundedCornerShape(2.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "SYNTH",
                                color = NeonCyan,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!format.vcodec.isNullOrEmpty() && format.vcodec != "none") {
                        Text(
                            text = "v:${format.vcodec.take(10)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = CosmicMuted,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    if (!format.acodec.isNullOrEmpty() && format.acodec != "none") {
                        Text(
                            text = "a:${format.acodec.take(10)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = CosmicMuted,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Extension label
                if (!format.ext.isNullOrEmpty()) {
                    Box(
                        modifier = Modifier
                            .background(NeonCyan.copy(alpha = 0.08f), RoundedCornerShape(2.dp))
                            .border(1.dp, NeonCyan.copy(alpha = 0.15f), RoundedCornerShape(2.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = format.ext.uppercase(),
                            color = NeonCyan,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Filesize
                if (format.filesize != null && format.filesize > 0) {
                    val sizeFormatted = remember(format.filesize) {
                        val mb = format.filesize / (1024f * 1024f)
                        if (mb > 1000f) {
                            val gb = mb / 1024f
                            "%.2f GB".format(gb)
                        } else {
                            "%.1f MB".format(mb)
                        }
                    }
                    Text(
                        text = sizeFormatted,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = CosmicMuted,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Check overlay
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Selected",
                        tint = NeonGreen
                    )
                }
            }
        }
    }
}

@Composable
fun DownloadSuccessResultCard(
    result: DownloadResult,
    onPlayClick: () -> Unit,
    onDownloadLocalClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = BentoBlueAccent.copy(alpha = 0.2f)),
        border = BorderStroke(1.dp, BentoBorder),
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "DOWNLOAD COMPLETED ON CLOUD",
                color = BentoPurpleText,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            MetadataRow(label = "Title", value = result.title, valueColor = BentoTextPrimary)
            if (!result.series.isNullOrEmpty()) {
                MetadataRow(label = "Series", value = result.series, valueColor = BentoPurpleText)
            }
            if (!result.quality.isNullOrEmpty()) {
                MetadataRow(label = "Quality", value = result.quality, valueColor = BentoPurpleText)
            }
            if (!result.format.isNullOrEmpty()) {
                MetadataRow(label = "Format", value = result.format, valueColor = BentoPurpleText)
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Save locally
                OutlinedButton(
                    onClick = onDownloadLocalClick,
                    border = BorderStroke(1.dp, BentoPurpleText),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.FileDownload, contentDescription = "Download", tint = BentoPurpleText)
                        Text(
                            text = "SAVE OFFLINE",
                            color = BentoPurpleText,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Play streaming online
                Button(
                    onClick = onPlayClick,
                    colors = ButtonDefaults.buttonColors(containerColor = BentoPurpleAccent),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1.2f)
                        .height(48.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Play", tint = BentoPurpleText)
                        Text(
                            text = "PLAY ONLINE",
                            color = BentoPurpleText,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BentoDashboard(onNavigateToSettings: () -> Unit, serverUrl: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Hero Card (Large, Purple)
        Card(
            colors = CardDefaults.cardColors(containerColor = BentoPurpleAccent),
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(BentoPurpleText),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FlashOn,
                            contentDescription = "Bolt",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = 0.4f))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "ACTIVE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = BentoPurpleText
                        )
                    }
                }
                
                Column {
                    Text(
                        text = "System Performance",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = BentoPurpleText
                    )
                    Text(
                        text = "98.4%",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Light,
                        color = BentoPurpleText
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Server: " + serverUrl.substringAfter("://").take(24),
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = BentoPurpleText.copy(alpha = 0.7f)
                    )
                }
            }
        }

        // Two medium cards side-by-side
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Card 1: Usage (Blue)
            Card(
                colors = CardDefaults.cardColors(containerColor = BentoBlueAccent),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(110.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = "Usage",
                        tint = BentoBlueText,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = "VPS Storage",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = BentoBlueText
                        )
                        Text(
                            text = "1.2 TB",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Light,
                            color = BentoBlueText
                        )
                    }
                }
            }

            // Card 2: Uptime (Mauve)
            Card(
                colors = CardDefaults.cardColors(containerColor = BentoMauveAccent),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(110.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = "Uptime",
                        tint = BentoMauveText,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = "VPS Uptime",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = BentoMauveText
                        )
                        Text(
                            text = "14d 2h",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Light,
                            color = BentoMauveText
                        )
                    }
                }
            }
        }

        // Activity List / Logs Card (White with Border)
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, BentoBorder),
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "RECENT VPS LOGS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = BentoTextMuted
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "More",
                        tint = BentoTextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LogItem(dotColor = Color(0xFFB3261E), text = "Hotstar stream established", time = "2m ago")
                    LogItem(dotColor = Color(0xFF2E7D32), text = "Main proxy tunnel: Active", time = "15m ago")
                    LogItem(dotColor = Color(0xFF1565C0), text = "Daily VPS maintenance ok", time = "1h ago")
                }
            }
        }
    }
}

@Composable
fun LogItem(dotColor: Color, text: String, time: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(dotColor)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = BentoTextPrimary,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = time,
            style = MaterialTheme.typography.labelSmall,
            color = BentoTextMuted
        )
    }
}
