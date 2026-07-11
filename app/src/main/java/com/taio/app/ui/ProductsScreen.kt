package com.taio.app.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.taio.app.data.AppRepository
import com.taio.app.data.ImageUtils
import com.taio.app.data.Product
import com.taio.app.data.isLowStock
import com.taio.app.data.isOutOfStock
import kotlinx.coroutines.launch
import kotlin.math.ceil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductsScreen(repo: AppRepository) {
    var products by remember { mutableStateOf(listOf<Product>()) }
    var showSheet by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Product?>(null) }
    var loading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    fun reload() {
        scope.launch { loading = true; products = repo.listProducts(); loading = false }
    }
    LaunchedEffect(Unit) { reload() }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { editing = null; showSheet = true }) {
                Icon(Icons.Default.Add, contentDescription = "إضافة منتج")
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            if (loading) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            } else if (products.isEmpty()) {
                Text("لسه مفيش منتجات، دوس + عشان تضيف أول منتج", Modifier.align(Alignment.Center))
            } else {
                LazyColumn(Modifier.fillMaxSize().padding(12.dp)) {
                    items(products, key = { it.id }) { p ->
                        ProductRow(
                            product = p,
                            onEdit = { editing = p; showSheet = true },
                            onDelete = { scope.launch { repo.deleteProduct(p.id); reload() } }
                        )
                    }
                }
            }
        }
    }

    if (showSheet) {
        ProductEditSheet(
            initial = editing,
            repo = repo,
            onDismiss = { showSheet = false },
            onSaved = { showSheet = false; reload() }
        )
    }
}

@Composable
private fun ProductRow(product: Product, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProductThumb(product.image)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(product.name, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "SKU: ${product.sku}  •  مخزون: ${product.stock}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isOutOfStock(product.stock) || isLowStock(product.stock))
                            MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (isOutOfStock(product.stock)) {
                        Spacer(Modifier.width(6.dp))
                        StockBadge("نفد", MaterialTheme.colorScheme.error)
                    } else if (isLowStock(product.stock)) {
                        Spacer(Modifier.width(6.dp))
                        StockBadge("منخفض", MaterialTheme.colorScheme.error)
                    }
                }
                Text("تكلفة: ${product.cost.toInt()} ج  •  بيع: ${product.price.toInt()} ج  •  ربح: ${(product.price - product.cost).toInt()} ج")
            }
            IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = "تعديل") }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "حذف") }
        }
    }
}

@Composable
private fun ProductThumb(image: String?, size: androidx.compose.ui.unit.Dp = 42.dp) {
    val bitmap = remember(image) { ImageUtils.decodeDataUrl(image) }
    Box(
        Modifier
            .size(size)
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                Icons.Default.Inventory2,
                contentDescription = null,
                modifier = Modifier.size(size * 0.5f),
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun StockBadge(text: String, color: Color) {
    Box(
        Modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(text, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductEditSheet(
    initial: Product?,
    repo: AppRepository,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var sku by remember { mutableStateOf(initial?.sku ?: Product.generateSku()) }
    var cost by remember { mutableStateOf(initial?.cost?.toString() ?: "") }
    var price by remember { mutableStateOf(initial?.price?.toString() ?: "") }
    var stock by remember { mutableStateOf(initial?.stock?.toString() ?: "0") }
    var image by remember { mutableStateOf(initial?.image) }
    var settings by remember { mutableStateOf<com.taio.app.data.AppSettings?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                image = ImageUtils.compressToDataUrl(context, uri, maxDim = 400, quality = 75)
            }
        }
    }

    LaunchedEffect(Unit) { settings = repo.getSettings() }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(20.dp).fillMaxWidth()) {
            Text(if (initial == null) "إضافة منتج" else "تعديل منتج", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(70.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                        .clickable { imagePicker.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    val bitmap = remember(image) { ImageUtils.decodeDataUrl(image) }
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Default.AddAPhoto, contentDescription = "إضافة صورة", tint = MaterialTheme.colorScheme.outline)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    TextButton(onClick = { imagePicker.launch("image/*") }) {
                        Text(if (image == null) "اختار صورة المنتج" else "غيّر الصورة")
                    }
                    if (image != null) {
                        TextButton(onClick = { image = null }) { Text("شيل الصورة") }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(name, { name = it }, label = { Text("اسم المنتج") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(sku, { sku = it }, label = { Text("كود المنتج (SKU)") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                cost, {
                    cost = it
                    val c = it.toDoubleOrNull()
                    val s = settings
                    if (c != null && c > 0 && s != null) {
                        val subtotal = c + s.packaging
                        val finalPrice = subtotal + subtotal * (s.profit / 100.0)
                        price = ceil(finalPrice).toInt().toString()
                    }
                },
                label = { Text("سعر التكلفة") }, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(price, { price = it }, label = { Text("سعر البيع") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(stock, { stock = it }, label = { Text("الكمية بالمخزون") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    if (name.isBlank()) return@Button
                    val product = Product(
                        id = initial?.id ?: ("p" + System.currentTimeMillis()),
                        name = name.trim(),
                        sku = sku.trim().ifBlank { Product.generateSku() },
                        cost = cost.toDoubleOrNull() ?: 0.0,
                        price = price.toDoubleOrNull() ?: 0.0,
                        stock = stock.toIntOrNull() ?: 0,
                        image = image
                    )
                    scope.launch { repo.saveProduct(product); onSaved() }
                }
            ) { Text("حفظ") }
            Spacer(Modifier.height(20.dp))
        }
    }
}
