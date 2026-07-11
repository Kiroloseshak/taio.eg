package com.taio.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.taio.app.data.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersScreen(repo: AppRepository) {
    var tab by remember { mutableStateOf(0) } // 0 = أوردر جديد, 1 = قائمة الأوردرات
    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("أوردر جديد") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("كل الأوردرات") })
        }
        if (tab == 0) NewOrderTab(repo) { tab = 1 } else OrdersListTab(repo)
    }
}

private data class CartLine(val product: Product, var qty: Int)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewOrderTab(repo: AppRepository, onCreated: () -> Unit) {
    var products by remember { mutableStateOf(listOf<Product>()) }
    var cart by remember { mutableStateOf(listOf<CartLine>()) }
    var pickerExpanded by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var city by remember { mutableStateOf(GOVERNORATES.first()) }
    var cityExpanded by remember { mutableStateOf(false) }
    var shipping by remember { mutableStateOf("65") }
    var notes by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { products = repo.listProducts() }

    val itemsTotal = cart.sumOf { it.qty * it.product.price }
    val grandTotal = itemsTotal + (shipping.toDoubleOrNull() ?: 0.0)

    LazyColumn(Modifier.fillMaxSize().padding(14.dp)) {
        item {
            Text("المنتجات", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            ExposedDropdownMenuBox(expanded = pickerExpanded, onExpandedChange = { pickerExpanded = it }) {
                OutlinedTextField(
                    value = "دوس عشان تضيف منتج للسلة",
                    onValueChange = {}, readOnly = true,
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(expanded = pickerExpanded, onDismissRequest = { pickerExpanded = false }) {
                    products.forEach { p ->
                        DropdownMenuItem(
                            text = { Text("${p.name} — ${p.price.toInt()} ج (متاح: ${p.stock})") },
                            onClick = {
                                pickerExpanded = false
                                val existing = cart.find { it.product.id == p.id }
                                cart = if (existing != null) {
                                    cart.map { if (it.product.id == p.id) it.copy(qty = it.qty + 1) else it }
                                } else cart + CartLine(p, 1)
                            }
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
        }
        items(cart) { line ->
            Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(line.product.name, fontWeight = FontWeight.Bold)
                        Text("${line.qty} × ${line.product.price.toInt()} ج = ${(line.qty * line.product.price).toInt()} ج")
                    }
                    OutlinedTextField(
                        value = line.qty.toString(),
                        onValueChange = { v ->
                            val q = v.toIntOrNull() ?: 1
                            cart = cart.map { if (it.product.id == line.product.id) it.copy(qty = q) else it }
                        },
                        modifier = Modifier.width(64.dp)
                    )
                    IconButton(onClick = { cart = cart.filterNot { it.product.id == line.product.id } }) {
                        Icon(Icons.Default.Delete, contentDescription = "حذف")
                    }
                }
            }
        }
        item {
            Spacer(Modifier.height(10.dp))
            Text("بيانات العميل", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(name, { name = it }, label = { Text("اسم العميل") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(phone, { phone = it }, label = { Text("رقم الموبايل") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(address, { address = it }, label = { Text("العنوان") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(6.dp))
            ExposedDropdownMenuBox(expanded = cityExpanded, onExpandedChange = { cityExpanded = it }) {
                OutlinedTextField(
                    value = city, onValueChange = {}, readOnly = true,
                    label = { Text("المحافظة") },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(expanded = cityExpanded, onDismissRequest = { cityExpanded = false }) {
                    GOVERNORATES.forEach { g ->
                        DropdownMenuItem(text = { Text(g) }, onClick = { city = g; cityExpanded = false })
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(shipping, { shipping = it }, label = { Text("مصاريف الشحن") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(notes, { notes = it }, label = { Text("ملاحظات") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(14.dp))
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text("إجمالي المنتجات: ${itemsTotal.toInt()} ج.م")
                    Text("الشحن: ${(shipping.toDoubleOrNull() ?: 0.0).toInt()} ج.م")
                    Text("الإجمالي الكلي: ${grandTotal.toInt()} ج.م", fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(14.dp))
            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = cart.isNotEmpty(),
                onClick = {
                    val items = cart.map { OrderItem(it.product.id, it.product.name, it.product.sku, it.qty, it.product.price) }
                    val order = Order(
                        orderNo = Order.generateOrderNo(),
                        items = items,
                        itemsTotal = itemsTotal,
                        shipping = shipping.toDoubleOrNull() ?: 0.0,
                        grandTotal = grandTotal,
                        name = name.ifBlank { "—" },
                        phone = phone.ifBlank { "—" },
                        address = address.ifBlank { "—" },
                        city = city,
                        notes = notes,
                        status = "new",
                        savedAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(Date())
                    )
                    scope.launch {
                        repo.saveOrder(order)
                        repo.reduceStockForOrder(items)
                        cart = emptyList(); name = ""; phone = ""; address = ""; notes = ""
                        onCreated()
                    }
                }
            ) { Text("حفظ الأوردر") }
            Spacer(Modifier.height(30.dp))
        }
    }
}

@Composable
private fun OrdersListTab(repo: AppRepository) {
    var orders by remember { mutableStateOf(listOf<Order>()) }
    var loading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    fun reload() { scope.launch { loading = true; orders = repo.listOrders(); loading = false } }
    LaunchedEffect(Unit) { reload() }

    Box(Modifier.fillMaxSize()) {
        if (loading) {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
        } else if (orders.isEmpty()) {
            Text("لسه مفيش أوردرات", Modifier.align(Alignment.Center))
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(12.dp)) {
                items(orders, key = { it.orderNo }) { o ->
                    OrderRow(order = o, onStatusChange = { newStatus ->
                        scope.launch { repo.updateOrderStatus(o, newStatus); reload() }
                    })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OrderRow(order: Order, onStatusChange: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Column(Modifier.padding(12.dp)) {
            Row {
                Column(Modifier.weight(1f)) {
                    Text(order.orderNo, fontWeight = FontWeight.Bold)
                    Text(order.name + " — " + order.phone, style = MaterialTheme.typography.bodySmall)
                    Text(order.productName, style = MaterialTheme.typography.bodySmall)
                    Text("الإجمالي: ${order.grandTotal.toInt()} ج.م")
                }
            }
            Spacer(Modifier.height(8.dp))
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                OutlinedTextField(
                    value = statusLabel(order.status), onValueChange = {}, readOnly = true,
                    modifier = Modifier.menuAnchor()
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    ORDER_STATUSES.forEach { (key, label) ->
                        DropdownMenuItem(text = { Text(label) }, onClick = { expanded = false; onStatusChange(key) })
                    }
                }
            }
        }
    }
}
