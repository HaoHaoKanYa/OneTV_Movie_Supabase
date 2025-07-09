package top.cywin.onetv.movie.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import top.cywin.onetv.movie.data.models.VodItem

/**
 * 电影卡片组件 (参考OneMoVie卡片设计)
 */
@Composable
fun MovieCard(
    movie: VodItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = modifier
            .width(120.dp)
            .height(180.dp),
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.DarkGray)
        ) {
            // 海报图片
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
            ) {
                if (movie.vodPic.isNotEmpty()) {
                    AsyncImage(
                        model = movie.vodPic,
                        contentDescription = movie.vodName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // 占位图
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Gray),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "暂无海报",
                            color = Color.White,
                            fontSize = 10.sp
                        )
                    }
                }
                
                // 备注标签 (右上角)
                if (movie.vodRemarks.isNotEmpty()) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp),
                        color = Color.Red.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = movie.vodRemarks,
                            color = Color.White,
                            fontSize = 8.sp,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            
            // 标题和信息
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = movie.vodName,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                // 年份和地区
                if (movie.vodYear.isNotEmpty() || movie.vodArea.isNotEmpty()) {
                    Text(
                        text = "${movie.vodYear} ${movie.vodArea}".trim(),
                        color = Color.Gray,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * 网格电影卡片 (用于分类页面)
 */
@Composable
fun MovieGridCard(
    movie: VodItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.75f),
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.DarkGray)
        ) {
            // 海报图片
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
            ) {
                if (movie.vodPic.isNotEmpty()) {
                    AsyncImage(
                        model = movie.vodPic,
                        contentDescription = movie.vodName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // 占位图
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Gray),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "暂无海报",
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }
                }
                
                // 备注标签
                if (movie.vodRemarks.isNotEmpty()) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp),
                        color = Color.Red.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = movie.vodRemarks,
                            color = Color.White,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
            }
            
            // 标题和信息
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = movie.vodName,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                // 年份和地区
                if (movie.vodYear.isNotEmpty() || movie.vodArea.isNotEmpty()) {
                    Text(
                        text = "${movie.vodYear} ${movie.vodArea}".trim(),
                        color = Color.Gray,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                // 评分
                if (movie.vodScore.isNotEmpty()) {
                    Text(
                        text = "评分: ${movie.vodScore}",
                        color = Color.Yellow,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

/**
 * 列表电影卡片 (用于搜索和历史页面)
 */
@Composable
fun MovieListCard(
    movie: VodItem,
    modifier: Modifier = Modifier,
    subtitle: String = "",
    onClick: () -> Unit = {}
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.DarkGray)
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 海报
            Box(
                modifier = Modifier
                    .width(60.dp)
                    .height(80.dp)
                    .clip(RoundedCornerShape(6.dp))
            ) {
                if (movie.vodPic.isNotEmpty()) {
                    AsyncImage(
                        model = movie.vodPic,
                        contentDescription = movie.vodName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Gray),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "海报",
                            color = Color.White,
                            fontSize = 10.sp
                        )
                    }
                }
            }
            
            // 信息
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = movie.vodName,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                if (subtitle.isNotEmpty()) {
                    Text(
                        text = subtitle,
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
                
                if (movie.vodRemarks.isNotEmpty()) {
                    Text(
                        text = movie.vodRemarks,
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
                
                if (movie.vodYear.isNotEmpty() || movie.vodArea.isNotEmpty()) {
                    Text(
                        text = "${movie.vodYear} ${movie.vodArea}".trim(),
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
                
                if (movie.vodActor.isNotEmpty()) {
                    Text(
                        text = "主演: ${movie.vodActor}",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
