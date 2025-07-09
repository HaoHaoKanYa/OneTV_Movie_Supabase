package top.cywin.onetv.movie.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.cywin.onetv.movie.data.models.VodFlag
import top.cywin.onetv.movie.data.models.VodEpisode

/**
 * 播放器控制组件 (临时实现)
 */
@Composable
fun PlayerControls(
    isPlaying: Boolean,
    position: Long,
    duration: Long,
    isVisible: Boolean,
    modifier: Modifier = Modifier,
    onPlayPause: () -> Unit = {},
    onSeek: (Long) -> Unit = {},
    onPrevious: () -> Unit = {},
    onNext: () -> Unit = {},
    onFullscreen: () -> Unit = {}
) {
    if (isVisible) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
        ) {
            // 中央播放控制
            Row(
                modifier = Modifier.align(Alignment.Center),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onPrevious,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "上一集",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                
                IconButton(
                    onClick = onPlayPause,
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "暂停" else "播放",
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }
                
                IconButton(
                    onClick = onNext,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "下一集",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            
            // 底部进度条和控制
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // 进度条
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = formatTime(position),
                        color = Color.White,
                        fontSize = 12.sp
                    )
                    
                    Slider(
                        value = if (duration > 0) position.toFloat() / duration.toFloat() else 0f,
                        onValueChange = { progress ->
                            onSeek((progress * duration).toLong())
                        },
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.White,
                            inactiveTrackColor = Color.Gray
                        )
                    )
                    
                    Text(
                        text = formatTime(duration),
                        color = Color.White,
                        fontSize = 12.sp
                    )
                    
                    IconButton(onClick = onFullscreen) {
                        Icon(
                            imageVector = Icons.Default.Fullscreen,
                            contentDescription = "全屏",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

/**
 * 播放源选择器
 */
@Composable
fun SourceSelector(
    flags: List<VodFlag>,
    currentFlag: VodFlag?,
    modifier: Modifier = Modifier,
    onFlagSelected: (VodFlag) -> Unit = {}
) {
    if (flags.isNotEmpty()) {
        Card(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color.DarkGray
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "播放线路",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    flags.forEach { flag ->
                        FilterChip(
                            onClick = { onFlagSelected(flag) },
                            label = { Text(flag.getSummary()) },
                            selected = currentFlag == flag,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White,
                                containerColor = Color.Gray,
                                labelColor = Color.White
                            )
                        )
                    }
                }
            }
        }
    }
}

/**
 * 剧集选择器
 */
@Composable
fun EpisodeSelector(
    flag: VodFlag?,
    currentEpisode: VodEpisode?,
    modifier: Modifier = Modifier,
    onEpisodeSelected: (VodEpisode) -> Unit = {}
) {
    if (flag != null) {
        val episodes = flag.createEpisodes()
        
        if (episodes.isNotEmpty()) {
            Card(
                modifier = modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.DarkGray
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "选集播放",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    // 简单的网格布局
                    val chunkedEpisodes = episodes.chunked(6)
                    chunkedEpisodes.forEach { rowEpisodes ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowEpisodes.forEach { episode ->
                                FilterChip(
                                    onClick = { onEpisodeSelected(episode) },
                                    label = { 
                                        Text(
                                            text = episode.getDisplayName(),
                                            fontSize = 12.sp
                                        ) 
                                    },
                                    selected = currentEpisode == episode,
                                    modifier = Modifier.weight(1f),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = Color.White,
                                        containerColor = Color.Gray,
                                        labelColor = Color.White
                                    )
                                )
                            }
                            
                            // 填充剩余空间
                            repeat(6 - rowEpisodes.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                        
                        if (rowEpisodes != chunkedEpisodes.last()) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

/**
 * 播放速度选择器
 */
@Composable
fun PlaybackSpeedSelector(
    currentSpeed: Float,
    modifier: Modifier = Modifier,
    onSpeedSelected: (Float) -> Unit = {}
) {
    val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
    
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color.DarkGray
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "播放速度",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                speeds.forEach { speed ->
                    FilterChip(
                        onClick = { onSpeedSelected(speed) },
                        label = { 
                            Text(
                                text = "${speed}x",
                                fontSize = 12.sp
                            ) 
                        },
                        selected = currentSpeed == speed,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White,
                            containerColor = Color.Gray,
                            labelColor = Color.White
                        )
                    )
                }
            }
        }
    }
}

/**
 * 格式化时间
 */
private fun formatTime(timeMs: Long): String {
    val seconds = timeMs / 1000
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    
    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, secs)
    } else {
        String.format("%02d:%02d", minutes, secs)
    }
}
