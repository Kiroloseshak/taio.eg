package com.taio.app.ui

import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.taio.app.data.ImageUtils
import kotlinx.coroutines.launch

/**
 * أداة الختم: بترفع صورة لوجو وصور منتجات (ممكن كذا صورة مع بعض)، وبتحط اللوجو فوقهم
 * بنفس منطق أداة الموقع (تحكم في الحجم والشفافية والمكان، بالسحب أو بالأزرار الجاهزة)،
 * وبعدين تحميل الصورة الحالية أو كل الصور دفعة واحدة على معرض الصور.
 */
@Composable
fun LogoStamperScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var logoBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var productItems by remember { mutableStateOf<List<Pair<String, Bitmap>>>(emptyList()) } // name, bitmap
    var currentIndex by remember { mutableStateOf(0) }
    var loadingImages by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }

    var scale by remember { mutableStateOf(0.28f) }
    var opacity by remember { mutableStateOf(1f) }
    var posX by remember { mutableStateOf(0.5f) }
    var posY by remember { mutableStateOf(0.88f) }

    val logoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            scope.launch { logoBitmap = ImageUtils.loadBitmap(context, uri, maxDim = 800) }
        }
    }

    val productsPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            loadingImages = true
            scope.launch {
                val loaded = uris.mapNotNull { uri ->
                    val bmp = ImageUtils.loadBitmap(context, uri) ?: return@mapNotNull null
                    val name = ImageUtils.queryDisplayName(context, uri) ?: "image_${System.currentTimeMillis()}.jpg"
                    name to bmp
                }
                productItems = loaded
                currentIndex = 0
                loadingImages = false
            }
        }
    }

    Scaffold { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("أداة الختم", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(
                    "ارفع اللوجو وصور المنتجات (ممكن كذا صورة مع بعض) وحط اللوجو في المكان اللي يناسبك",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(14.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { logoPicker.launch("image/*") }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (logoBitmap == null) "اختار اللوجو" else "غيّر اللوجو")
                    }
                    OutlinedButton(onClick = { productsPicker.launch("image/*") }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Collections, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("اختار صور المنتجات")
                    }
                }

                Spacer(Modifier.height(16.dp))

                when {
                    loadingImages -> Box(Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    productItems.isEmpty() -> Box(
                        Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("لسه معملتش اختيار صور منتجات", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    else -> {
                        StamperPreview(
                            baseBitmap = productItems[currentIndex].second,
                            logoBitmap = logoBitmap,
                            scale = scale,
                            opacity = opacity,
                            posX = posX,
                            posY = posY,
                            onDrag = { dx, dy ->
                                posX = (posX + dx).coerceIn(0f, 1f)
                                posY = (posY + dy).coerceIn(0f, 1f)
                            }
                        )

                        if (productItems.size > 1) {
                            Spacer(Modifier.height(8.dp))
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = {
                                    currentIndex = (currentIndex - 1 + productItems.size) % productItems.size
                                }) { Icon(Icons.Default.ChevronRight, contentDescription = "السابقة") }
                                Text("${currentIndex + 1} / ${productItems.size}", style = MaterialTheme.typography.bodySmall)
                                IconButton(onClick = {
                                    currentIndex = (currentIndex + 1) % productItems.size
                                }) { Icon(Icons.Default.ChevronLeft, contentDescription = "التالية") }
                            }
                        }

                        Spacer(Modifier.height(14.dp))
                        Text("حجم اللوجو: ${(scale * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
                        Slider(value = scale, onValueChange = { scale = it }, valueRange = 0.08f..0.7f)

                        Text("شفافية اللوجو: ${(opacity * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
                        Slider(value = opacity, onValueChange = { opacity = it }, valueRange = 0.1f..1f)

                        Spacer(Modifier.height(6.dp))
                        Text("أماكن جاهزة", style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(6.dp))
                        val margin = 0.1f
                        val corners = listOf(
                            "أعلى اليمين" to (margin to margin),
                            "أعلى الوسط" to (0.5f to margin),
                            "أعلى الشمال" to (1 - margin to margin),
                            "أسفل اليمين" to (margin to 1 - margin),
                            "أسفل الوسط" to (0.5f to 1 - margin),
                            "أسفل الشمال" to (1 - margin to 1 - margin)
                        )
                        CornerButtonsGrid(corners) { x, y -> posX = x; posY = y }

                        Spacer(Modifier.height(18.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                enabled = logoBitmap != null && !saving,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    val logo = logoBitmap ?: return@Button
                                    scope.launch {
                                        saving = true
                                        val (name, bmp) = productItems[currentIndex]
                                        val stamped = ImageUtils.stampLogo(bmp, logo, posX, posY, scale, opacity)
                                        val ok = ImageUtils.saveBitmapToGallery(context, stamped, "TAIO_$name")
                                        saving = false
                                        Toast.makeText(
                                            context,
                                            if (ok) "اتحفظت الصورة في المعرض" else "حصلت مشكلة في الحفظ",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            ) { Text("تحميل الصورة الحالية") }

                            Button(
                                enabled = logoBitmap != null && productItems.size > 1 && !saving,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    val logo = logoBitmap ?: return@Button
                                    scope.launch {
                                        saving = true
                                        var okCount = 0
                                        productItems.forEach { (name, bmp) ->
                                            val stamped = ImageUtils.stampLogo(bmp, logo, posX, posY, scale, opacity)
                                            if (ImageUtils.saveBitmapToGallery(context, stamped, "TAIO_$name")) okCount++
                                        }
                                        saving = false
                                        Toast.makeText(
                                            context,
                                            "اتحفظ $okCount من ${productItems.size} صورة في المعرض",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            ) { Text("تحميل الكل") }
                        }
                        if (saving) {
                            Spacer(Modifier.height(10.dp))
                            LinearProgressIndicator(Modifier.fillMaxWidth())
                        }
                        Spacer(Modifier.height(30.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun StamperPreview(
    baseBitmap: Bitmap,
    logoBitmap: Bitmap?,
    scale: Float,
    opacity: Float,
    posX: Float,
    posY: Float,
    onDrag: (dx: Float, dy: Float) -> Unit
) {
    val aspect = baseBitmap.width.toFloat() / baseBitmap.height.toFloat()
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .aspectRatio(aspect)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFF6F4EF))
            .border(1.dp, Color(0xFFE4E1D9), RoundedCornerShape(10.dp))
    ) {
        val boxWidthPx = constraints.maxWidth.toFloat()
        val boxHeightPx = constraints.maxHeight.toFloat()

        Image(
            bitmap = baseBitmap.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )

        if (logoBitmap != null) {
            val logoAspect = logoBitmap.width.toFloat() / logoBitmap.height.toFloat()
            val logoWidthDp = maxWidth * scale
            val logoHeightDp = logoWidthDp / logoAspect
            val offsetX = maxWidth * posX - logoWidthDp / 2
            val offsetY = maxHeight * posY - logoHeightDp / 2

            Image(
                bitmap = logoBitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .size(logoWidthDp, logoHeightDp)
                    .offset(x = offsetX, y = offsetY)
                    .graphicsLayer(alpha = opacity)
                    .pointerInput(boxWidthPx, boxHeightPx) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            onDrag(dragAmount.x / boxWidthPx, dragAmount.y / boxHeightPx)
                        }
                    }
            )
        }
    }
}

@Composable
private fun CornerButtonsGrid(items: List<Pair<String, Pair<Float, Float>>>, onPick: (Float, Float) -> Unit) {
    Column {
        items.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { (label, pos) ->
                    OutlinedButton(
                        onClick = { onPick(pos.first, pos.second) },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 8.dp, horizontal = 2.dp)
                    ) {
                        Text(label, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
        }
    }
}
