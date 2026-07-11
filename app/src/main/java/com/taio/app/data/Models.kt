package com.taio.app.data

import org.json.JSONArray
import org.json.JSONObject

data class Product(
    val id: String = "",
    val name: String = "",
    val sku: String = "",
    val cost: Double = 0.0,
    val price: Double = 0.0,
    val stock: Int = 0,
    val image: String? = null
) {
    fun toJson(): String = JSONObject().apply {
        put("id", id); put("name", name); put("sku", sku)
        put("cost", cost); put("price", price); put("stock", stock)
        put("image", image ?: JSONObject.NULL)
    }.toString()

    companion object {
        fun fromJson(s: String): Product {
            val o = JSONObject(s)
            return Product(
                id = o.optString("id"),
                name = o.optString("name"),
                sku = o.optString("sku"),
                cost = o.optDouble("cost", 0.0),
                price = o.optDouble("price", 0.0),
                stock = o.optInt("stock", 0),
                image = if (o.isNull("image")) null else o.optString("image").ifBlank { null }
            )
        }
        fun generateSku(): String = "PRD-" + (1000..9999).random()
    }
}

data class OrderItem(
    val productId: String,
    val name: String,
    val sku: String,
    val qty: Int,
    val price: Double
) {
    val lineTotal: Double get() = qty * price
}

data class Order(
    val orderNo: String = "",
    val items: List<OrderItem> = emptyList(),
    val itemsTotal: Double = 0.0,
    val shipping: Double = 0.0,
    val grandTotal: Double = 0.0,
    val name: String = "",
    val phone: String = "",
    val address: String = "",
    val city: String = "",
    val notes: String = "",
    val status: String = "new",
    val savedAt: String = ""
) {
    val productName: String get() = items.joinToString("، ") { it.name }

    fun toJson(): String = JSONObject().apply {
        put("orderNo", orderNo)
        put("items", JSONArray().apply {
            items.forEach { it2 ->
                put(JSONObject().apply {
                    put("productId", it2.productId); put("name", it2.name); put("sku", it2.sku)
                    put("qty", it2.qty); put("price", it2.price); put("lineTotal", it2.lineTotal)
                })
            }
        })
        put("itemsTotal", itemsTotal); put("shipping", shipping); put("grandTotal", grandTotal)
        put("name", name); put("phone", phone); put("address", address); put("city", city)
        put("notes", notes); put("productName", productName); put("status", status); put("savedAt", savedAt)
    }.toString()

    companion object {
        fun fromJson(s: String): Order {
            val o = JSONObject(s)
            val itemsArr = o.optJSONArray("items") ?: JSONArray()
            val items = (0 until itemsArr.length()).map { i ->
                val it2 = itemsArr.getJSONObject(i)
                OrderItem(
                    productId = it2.optString("productId"),
                    name = it2.optString("name"),
                    sku = it2.optString("sku"),
                    qty = it2.optInt("qty", 1),
                    price = it2.optDouble("price", 0.0)
                )
            }
            return Order(
                orderNo = o.optString("orderNo"),
                items = items,
                itemsTotal = o.optDouble("itemsTotal", 0.0),
                shipping = o.optDouble("shipping", 0.0),
                grandTotal = o.optDouble("grandTotal", 0.0),
                name = o.optString("name"),
                phone = o.optString("phone"),
                address = o.optString("address"),
                city = o.optString("city"),
                notes = o.optString("notes"),
                status = o.optString("status", "new"),
                savedAt = o.optString("savedAt")
            )
        }

        fun generateOrderNo(): String {
            val d = java.util.Calendar.getInstance()
            val y = (d.get(java.util.Calendar.YEAR) % 100).toString().padStart(2, '0')
            val m = (d.get(java.util.Calendar.MONTH) + 1).toString().padStart(2, '0')
            val day = d.get(java.util.Calendar.DAY_OF_MONTH).toString().padStart(2, '0')
            val rnd = (100..999).random()
            return "TAIO-$y$m$day-$rnd"
        }
    }
}

data class AppSettings(
    val packaging: Double = 30.0,
    val profit: Double = 30.0
) {
    fun toJson(): String = JSONObject().apply { put("packaging", packaging); put("profit", profit) }.toString()
    companion object {
        fun fromJson(s: String?): AppSettings {
            if (s.isNullOrBlank()) return AppSettings()
            val o = JSONObject(s)
            return AppSettings(o.optDouble("packaging", 30.0), o.optDouble("profit", 30.0))
        }
    }
}

val ORDER_STATUSES = listOf(
    "new" to "جديد",
    "shipping" to "قيد الشحن",
    "delivered" to "تم التسليم",
    "returned" to "مرتجع",
    "cancelled" to "ملغي"
)

fun statusLabel(key: String) = ORDER_STATUSES.firstOrNull { it.first == key }?.second ?: "جديد"

const val DEFAULT_SHIPPING = 65.0

fun isOutOfStock(stock: Int) = stock <= 0
fun isLowStock(stock: Int) = stock in 1..3

val GOVERNORATES = listOf(
    "القاهرة","الجيزة","الإسكندرية","الدقهلية","البحر الأحمر","البحيرة","الفيوم","الغربية",
    "الإسماعيلية","المنوفية","المنيا","القليوبية","الوادي الجديد","السويس","أسوان","أسيوط",
    "بني سويف","بورسعيد","دمياط","الشرقية","جنوب سيناء","كفر الشيخ","مطروح","الأقصر","قنا",
    "شمال سيناء","سوهاج"
)
