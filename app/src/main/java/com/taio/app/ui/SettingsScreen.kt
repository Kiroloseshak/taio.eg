package com.taio.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.taio.app.data.AppRepository
import com.taio.app.data.AppSettings
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(repo: AppRepository) {
    var packaging by remember { mutableStateOf("30") }
    var profit by remember { mutableStateOf("30") }
    var cost by remember { mutableStateOf("") }
    var saved by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val s = repo.getSettings()
        packaging = s.packaging.toInt().toString()
        profit = s.profit.toInt().toString()
    }

    val costVal = cost.toDoubleOrNull() ?: 0.0
    val packagingVal = packaging.toDoubleOrNull() ?: 0.0
    val profitPct = profit.toDoubleOrNull() ?: 0.0
    val subtotal = costVal + packagingVal
    val profitVal = subtotal * (profitPct / 100.0)
    val finalPrice = subtotal + profitVal

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("حاسبة التسعير", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(cost, { cost = it }, label = { Text("سعر تكلفة المنتج") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(14.dp))
        if (costVal > 0) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    Text("التكلفة: ${costVal.toInt()} ج.م")
                    Text("التغليف: ${packagingVal.toInt()} ج.م")
                    Text("المكسب (${profit}%): ${profitVal.toInt()} ج.م")
                    Spacer(Modifier.height(6.dp))
                    Text("السعر النهائي: ${kotlin.math.ceil(finalPrice).toInt()} ج.م", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                }
            }
        }

        Spacer(Modifier.height(28.dp))
        Divider()
        Spacer(Modifier.height(14.dp))
        Text("الإعدادات العامة", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(packaging, { packaging = it; saved = false }, label = { Text("مصاريف التغليف الافتراضية") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(profit, { profit = it; saved = false }, label = { Text("نسبة الربح الافتراضية %") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(14.dp))
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                scope.launch {
                    repo.saveSettings(AppSettings(packagingVal, profitPct))
                    saved = true
                }
            }
        ) { Text(if (saved) "✓ اتحفظ" else "حفظ الإعدادات") }
    }
}
