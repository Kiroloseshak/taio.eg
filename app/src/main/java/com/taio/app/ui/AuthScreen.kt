package com.taio.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

private fun authErrorMessage(code: String?): String = when (code) {
    "ERROR_INVALID_EMAIL" -> "البريد الإلكتروني غير صحيح"
    "ERROR_USER_NOT_FOUND" -> "مفيش حساب بالبريد ده"
    "ERROR_WRONG_PASSWORD", "ERROR_INVALID_CREDENTIAL" -> "البريد أو كلمة المرور غلط"
    "ERROR_EMAIL_ALREADY_IN_USE" -> "البريد ده مسجل بالفعل، جرب تسجيل الدخول"
    "ERROR_WEAK_PASSWORD" -> "كلمة المرور لازم تكون 6 حروف/أرقام على الأقل"
    "ERROR_TOO_MANY_REQUESTS" -> "محاولات كتير غلط، جرب تاني بعد شوية"
    else -> "حصل خطأ، حاول تاني"
}

@Composable
fun AuthScreen(onLoggedIn: () -> Unit) {
    var mode by remember { mutableStateOf("login") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            Modifier.fillMaxWidth(0.9f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("TAIO", fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(24.dp))
            Row(Modifier.fillMaxWidth()) {
                TabButton("دخول", mode == "login", Modifier.weight(1f)) { mode = "login"; error = null }
                Spacer(Modifier.width(6.dp))
                TabButton("حساب جديد", mode == "register", Modifier.weight(1f)) { mode = "register"; error = null }
            }
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = email, onValueChange = { email = it },
                label = { Text("البريد الإلكتروني") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = password, onValueChange = { password = it },
                label = { Text("كلمة المرور") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth()
            )
            if (error != null) {
                Spacer(Modifier.height(10.dp))
                Text(error!!, color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(16.dp))
            Button(
                enabled = !loading,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    if (email.isBlank() || password.isBlank()) {
                        error = "اكتب البريد الإلكتروني وكلمة المرور"; return@Button
                    }
                    loading = true; error = null
                    val auth = FirebaseAuth.getInstance()
                    val task = if (mode == "login")
                        auth.signInWithEmailAndPassword(email.trim(), password)
                    else
                        auth.createUserWithEmailAndPassword(email.trim(), password)
                    task.addOnSuccessListener { loading = false; onLoggedIn() }
                        .addOnFailureListener {
                            loading = false
                            error = authErrorMessage((it as? com.google.firebase.auth.FirebaseAuthException)?.errorCode)
                        }
                }
            ) {
                Text(if (mode == "login") "دخول" else "إنشاء الحساب")
            }
        }
    }
}

@Composable
private fun TabButton(text: String, active: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    if (active) {
        Button(onClick = onClick, modifier = modifier) { Text(text) }
    } else {
        OutlinedButton(onClick = onClick, modifier = modifier) { Text(text) }
    }
}
