package com.taio.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.taio.app.data.AppRepository
import kotlinx.coroutines.launch

@Composable
fun DashboardScreen(repo: AppRepository) {
    var stats by remember { mutableStateOf(listOf<Pair<String, String>>()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch {
            val products = repo.listProducts()
            val orders = repo.listOrders()
            val totalStock = products.sumOf { it.stock }
            val lowStock = products.count { com.taio.app.data.isLowStock(it.stock) }
            val outStock = products.count { com.taio.app.data.isOutOfStock(it.stock) }
            val newOrders = orders.count { it.status == "new" }
            val delivered = orders.count { it.status == "delivered" }
            val totalSales = orders.filter { it.status != "cancelled" && it.status != "returned" }.sumOf { it.grandTotal }

            stats = listOf(
                "عدد المنتجات" to products.size.toString(),
                "إجمالي المخزون" to totalStock.toString(),
                "منتجات قاربت تخلص" to lowStock.toString(),
                "منتجات نفدت" to outStock.toString(),
                "عدد الأوردرات" to orders.size.toString(),
                "أوردرات جديدة" to newOrders.toString(),
                "أوردرات متسلمة" to delivered.toString(),
                "إجمالي المبيعات" to "${totalSales.toInt()} ج.م"
            )
        }
    }

    LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.fillMaxSize().padding(14.dp)) {
        items(stats) { (label, value) ->
            Card(Modifier.padding(6.dp).fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(4.dp))
                    Text(label, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
