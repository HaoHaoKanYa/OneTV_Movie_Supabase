package top.cywin.onetv.movie.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import top.cywin.onetv.movie.bean.Episode

/**
 * OneTV Movie播放器控制组件 - 按照FongMi_TV整合指南重构
 */
@Composable
fun PlayerControls(
    isPlaying: Boolean,
    currentPosition: Long,
    duration: Long,
    bufferedPosition: Long,
    episodes: List<Episode>,
    currentEpisodeIndex: Int,
    isControlsVisible: Boolean,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onEpisodeSelect: (Int) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onToggleControls: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onToggleControls() }
    ) {
        // 播放控制覆盖层
        AnimatedVisibility(
            visible = isControlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
            ) {
                // 顶部控制栏
                TopControlBar(
                    onBack = onBack,
                    modifier = Modifier.align(Alignment.TopCenter)
                )

                // 中央播放控制
                CenterPlayControls(
                    isPlaying = isPlaying,
                    onPlayPause = onPlayPause,
                    onPrevious = onPrevious,
                    onNext = onNext,
                    hasPrevious = currentEpisodeIndex > 0,
                    hasNext = currentEpisodeIndex < episodes.size - 1,
                    modifier = Modifier.align(Alignment.Center)
                )

                // 底部控制栏
                BottomControlBar(
                    currentPosition = currentPosition,
                    duration = duration,
                    bufferedPosition = bufferedPosition,
                    onSeek = onSeek,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )

                // 剧集选择器
                if (episodes.size > 1) {
                    EpisodeSelector(
                        episodes = episodes,
                        currentEpisodeIndex = currentEpisodeIndex,
                        onEpisodeSelect = onEpisodeSelect,
                        modifier = Modifier.align(Alignment.CenterEnd)
                    )
                }
            }
        }
    }
}

@Composable
private fun TopControlBar(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "返回",
                tint = Color.White
            )
        }
    }
}

@Composable
private fun CenterPlayControls(
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    hasPrevious: Boolean,
    hasNext: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(32.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onPrevious,
            enabled = hasPrevious
        ) {
            Icon(
                imageVector = Icons.Default.SkipPrevious,
                contentDescription = "上一集",
                tint = if (hasPrevious) Color.White else Color.Gray,
                modifier = Modifier.size(48.dp)
            )
        }

        IconButton(onClick = onPlayPause) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "暂停" else "播放",
                tint = Color.White,
                modifier = Modifier.size(64.dp)
            )
        }

        IconButton(
            onClick = onNext,
            enabled = hasNext
        ) {
            Icon(
                imageVector = Icons.Default.SkipNext,
                contentDescription = "下一集",
                tint = if (hasNext) Color.White else Color.Gray,
                modifier = Modifier.size(48.dp)
            )
        }
    }
}

@Composable
private fun BottomControlBar(
    currentPosition: Long,
    duration: Long,
    bufferedPosition: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // 进度条
        VideoProgressBar(
            currentPosition = currentPosition,
            duration = duration,
            bufferedPosition = bufferedPosition,
            onSeek = onSeek,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 时间显示
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatTime(currentPosition),
                color = Color.White,
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = formatTime(duration),
                color = Color.White,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun VideoProgressBar(
    currentPosition: Long,
    duration: Long,
    bufferedPosition: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = if (duration > 0) currentPosition.toFloat() / duration else 0f
    val bufferedProgress = if (duration > 0) bufferedPosition.toFloat() / duration else 0f

    Box(modifier = modifier.height(4.dp)) {
        // 缓冲进度
        LinearProgressIndicator(
            progress = bufferedProgress,
            modifier = Modifier.fillMaxSize(),
            color = Color.White.copy(alpha = 0.3f),
            trackColor = Color.White.copy(alpha = 0.1f)
        )

        // 播放进度
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.primary,
            trackColor = Color.Transparent
        )
    }
}

@Composable
private fun EpisodeSelector(
    episodes: List<Episode>,
    currentEpisodeIndex: Int,
    onEpisodeSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .width(120.dp)
            .heightIn(max = 300.dp)
            .background(
                Color.Black.copy(alpha = 0.8f),
                RoundedCornerShape(8.dp)
            )
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        itemsIndexed(episodes) { index, episode ->
            EpisodeItem(
                episode = episode,
                isSelected = index == currentEpisodeIndex,
                onClick = { onEpisodeSelect(index) }
            )
        }
    }
}

@Composable
private fun EpisodeItem(
    episode: Episode,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = episode.name,
            modifier = Modifier.padding(8.dp),
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else Color.White,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun formatTime(timeMs: Long): String {
    val totalSeconds = timeMs / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}
