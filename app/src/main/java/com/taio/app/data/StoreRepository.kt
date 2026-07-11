package com.taio.app.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * نفس بالظبط بنية الموقع: users/{uid}/store/{key} -> { value: <json string>, updatedAt: <millis> }
 * عشان أي داتا تتسجل من الموقع تظهر فورًا هنا والعكس.
 */
class StoreRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private fun storeCollection() = db.collection("users")
        .document(auth.currentUser?.uid ?: throw IllegalStateException("مفيش مستخدم مسجل دخول"))
        .collection("store")

    suspend fun set(key: String, value: String) {
        storeCollection().document(key).set(mapOf("value" to value, "updatedAt" to System.currentTimeMillis())).await()
    }

    suspend fun get(key: String): String? {
        val doc = storeCollection().document(key).get().await()
        return if (doc.exists()) doc.getString("value") else null
    }

    suspend fun listByPrefix(prefix: String): List<Pair<String, String>> {
        val end = prefix + "\uf8ff"
        val snap = storeCollection()
            .whereGreaterThanOrEqualTo(FieldPath.documentId(), prefix)
            .whereLessThanOrEqualTo(FieldPath.documentId(), end)
            .get().await()
        return snap.documents.mapNotNull { d -> d.getString("value")?.let { d.id to it } }
    }

    suspend fun delete(key: String) {
        storeCollection().document(key).delete().await()
    }
}

class AppRepository(private val store: StoreRepository = StoreRepository()) {

    // ---------- Products ----------
    suspend fun listProducts(): List<Product> =
        store.listByPrefix("product:").map { Product.fromJson(it.second) }
            .sortedByDescending { it.id }

    suspend fun saveProduct(product: Product) {
        store.set("product:" + product.id, product.toJson())
    }

    suspend fun deleteProduct(id: String) {
        store.delete("product:$id")
    }

    // ---------- Orders ----------
    suspend fun listOrders(): List<Order> =
        store.listByPrefix("order:").map { Order.fromJson(it.second) }
            .sortedByDescending { it.savedAt }

    suspend fun saveOrder(order: Order) {
        store.set("order:" + order.orderNo, order.toJson())
    }

    suspend fun updateOrderStatus(order: Order, newStatus: String) {
        store.set("order:" + order.orderNo, order.copy(status = newStatus).toJson())
    }

    suspend fun deleteOrder(orderNo: String) {
        store.delete("order:$orderNo")
    }

    suspend fun reduceStockForOrder(items: List<OrderItem>) {
        for (item in items) {
            val raw = store.get("product:" + item.productId) ?: continue
            val p = Product.fromJson(raw)
            val newStock = maxOf(0, p.stock - item.qty)
            store.set("product:" + p.id, p.copy(stock = newStock).toJson())
        }
    }

    // ---------- Settings ----------
    suspend fun getSettings(): AppSettings = AppSettings.fromJson(store.get("settings:app"))

    suspend fun saveSettings(settings: AppSettings) {
        store.set("settings:app", settings.toJson())
    }
}
