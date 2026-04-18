package com.wuuee.imganalysis

import android.Manifest
import android.content.ContentUris
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.exifinterface.media.ExifInterface
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

private const val TOP_BUCKET_LIMIT = 5

data class FocalGroup(
    val label: String,
    val count: Int,
    val thumbnails: List<Uri>,
    val isOther: Boolean = false,
    val otherBreakdown: List<Pair<String, Int>> = emptyList()
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                FocalAnalyzerApp()
            }
        }
    }
}

@Composable
private fun FocalAnalyzerApp() {
    val activity = LocalContext.current as? ComponentActivity
    var hasPermission by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var totalPhotos by remember { mutableStateOf(0) }
    var focalStats by remember { mutableStateOf<List<FocalGroup>>(emptyList()) }
    var message by remember { mutableStateOf("请先授权读取相册权限。") }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        message = if (granted) "权限已授予，点击“扫描相册”。" else "未授予权限，无法读取照片。"
    }

    val permission = remember {
        if (Build.VERSION.SDK_INT >= 33) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }

    LaunchedEffect(Unit) {
        launcher.launch(permission)
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = "照片焦段统计", style = MaterialTheme.typography.headlineSmall)
            Text(text = message)

            Button(
                enabled = hasPermission && !isLoading,
                onClick = {
                    isLoading = true
                    message = "正在扫描..."
                }
            ) {
                Text(if (isLoading) "扫描中..." else "扫描相册")
            }

            if (isLoading) {
                LaunchedEffect(Unit) {
                    if (activity == null) {
                        message = "无法访问 Activity 上下文。"
                        isLoading = false
                        return@LaunchedEffect
                    }
                    val (count, stats, readWithExif) = activity.scanFocalStats()
                    totalPhotos = count
                    focalStats = stats
                    message = "已扫描 $count 张照片，其中 $readWithExif 张包含可读焦段信息。"
                    isLoading = false
                }
            }

            Text(text = "总照片数：$totalPhotos")
            if (focalStats.isNotEmpty()) {
                FocalStatsList(items = focalStats)
            }
        }
    }
}

@Composable
private fun FocalStatsList(items: List<FocalGroup>) {
    val sliceColors = listOf(
        Color(0xFF4FC3F7),
        Color(0xFF9575CD),
        Color(0xFF4DB6AC),
        Color(0xFFFFB74D),
        Color(0xFFFF8A65),
        Color(0xFF90A4AE)
    )

    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item(key = "pie-chart") {
            PieChartCard(items = items, colors = sliceColors)
        }

        items(items, key = { it.label }) { item ->
            var expanded by rememberSaveable(item.label) { mutableStateOf(false) }

            Card {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "${item.label} (${item.count})",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        TextButton(onClick = { expanded = !expanded }) {
                            Text(
                                if (expanded) "收起" else if (item.isOther) "展开其他" else "展开"
                            )
                        }
                    }

                    if (expanded) {
                        if (item.isOther && item.otherBreakdown.isNotEmpty()) {
                            Text(
                                text = "其他内焦段：${
                                    item.otherBreakdown.joinToString("，") { "${it.first}(${it.second})" }
                                }",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        if (item.thumbnails.isEmpty()) {
                            Text("暂无缩略图")
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 96.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(280.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                gridItems(item.thumbnails, key = { it.toString() }) { uri ->
                                    AsyncImage(
                                        model = uri,
                                        contentDescription = "缩略图",
                                        modifier = Modifier
                                            .size(96.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PieChartCard(items: List<FocalGroup>, colors: List<Color>) {
    val total = items.sumOf { it.count }.coerceAtLeast(1)

    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("焦段占比饼图", style = MaterialTheme.typography.titleMedium)

            Canvas(
                modifier = Modifier
                    .size(210.dp)
                    .padding(8.dp)
            ) {
                var startAngle = -90f
                items.forEachIndexed { index, group ->
                    val sweep = 360f * group.count.toFloat() / total.toFloat()
                    drawArc(
                        color = colors[index % colors.size],
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = true
                    )
                    startAngle += sweep
                }
            }

            items.forEachIndexed { index, group ->
                val ratio = (group.count * 100f / total).roundToInt()
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Canvas(modifier = Modifier.size(12.dp)) {
                        drawCircle(color = colors[index % colors.size])
                    }
                    Text("${group.label}: ${group.count} (${ratio}%)")
                }
            }
        }
    }
}

private suspend fun ComponentActivity.scanFocalStats(): Triple<Int, List<FocalGroup>, Int> =
    withContext(Dispatchers.IO) {
        val projection = arrayOf(MediaStore.Images.Media._ID)
        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val counts = linkedMapOf<String, Int>()
        val thumbnails = linkedMapOf<String, MutableList<Uri>>()
        var total = 0
        var withFocal = 0

        contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            while (cursor.moveToNext()) {
                total += 1
                val id = cursor.getLong(idIndex)
                val imageUri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id
                )

                val bucket = contentResolver.openInputStream(imageUri)?.use { stream ->
                    val exif = ExifInterface(stream)
                    val focal35mm = exif.getAttributeInt(ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM, 0)
                    if (focal35mm > 0) {
                        "${focal35mm}mm (35mm等效)"
                    } else {
                        parseFocalLength(exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH))
                    }
                }

                if (!bucket.isNullOrBlank()) {
                    withFocal += 1
                    counts[bucket] = (counts[bucket] ?: 0) + 1
                    thumbnails.getOrPut(bucket) { mutableListOf() }.add(imageUri)
                }
            }
        }

        val sorted = counts.entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })

        val top = sorted.take(TOP_BUCKET_LIMIT).map {
            FocalGroup(
                label = it.key,
                count = it.value,
                thumbnails = thumbnails[it.key].orEmpty()
            )
        }

        val rest = sorted.drop(TOP_BUCKET_LIMIT)
        val groups = top.toMutableList()
        if (rest.isNotEmpty()) {
            val otherThumbs = rest.flatMap { entry -> thumbnails[entry.key].orEmpty() }
            groups.add(
                FocalGroup(
                    label = "其他",
                    count = rest.sumOf { it.value },
                    thumbnails = otherThumbs,
                    isOther = true,
                    otherBreakdown = rest.map { it.key to it.value }
                )
            )
        }

        Triple(total, groups, withFocal)
    }

private fun parseFocalLength(raw: String?): String? {
    if (raw.isNullOrBlank()) return null
    val value = raw.trim()
    return if (value.contains("/")) {
        val pieces = value.split("/")
        if (pieces.size != 2) return null
        val n = pieces[0].toDoubleOrNull() ?: return null
        val d = pieces[1].toDoubleOrNull() ?: return null
        if (d == 0.0) return null
        "${(n / d).toInt()}mm"
    } else {
        val number = value.toDoubleOrNull() ?: return null
        "${number.toInt()}mm"
    }
}
