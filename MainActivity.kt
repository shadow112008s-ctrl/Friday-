package com.friday.widget

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
[0;32m[1m==> [0m[1mAvailable at your primary URL https://friday-y34b.onrender.com[0m
// ---- Colors matching the Friday visual identity ----
private val Bg = Color(0xFF0F1420)
private val Surface = Color(0xFF161D2E)
private val Border = Color(0xFF1D2436)
private val Amber = Color(0xFFF2A93B)
private val TextPrimary = Color(0xFFE8EAF0)
private val TextMuted = Color(0xFF8B92A8)
[0;32m[1m==> [0m[1mAvailable at your primary URL https://friday-y34b.onrender.com[0m
private const val BACKEND_URL = "https://YOUR-BACKEND-URL.example.com"

data class ChatMessage(val role: String, val text: String)

class MainActivity : ComponentActivity() {

    private val client = OkHttpClient()
    private var pendingFlashlightAction: (() -> Unit)? = null

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) pendingFlashlightAction?.invoke() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val openQuickChat = intent.getBooleanExtra("open_quick_chat", false)

        setContent {
            FridayApp(
                openQuickChat = openQuickChat,
                client = client,
                onFlashlight = { toggleFlashlightWithPermission() },
                onWifi = { FridayQuickActions.openWifiPanel(this) },
                onBluetooth = { FridayQuickActions.openBluetoothPanel(this) },
                onOpenApp = { pkg -> FridayQuickActions.openApp(this, pkg) }
            )
        }
    }

    private fun toggleFlashlightWithPermission() {
        val hasPermission = checkSelfPermission(Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            FridayQuickActions.toggleFlashlight(this)
        } else {
            pendingFlashlightAction = { FridayQuickActions.toggleFlashlight(this) }
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FridayApp(
    openQuickChat: Boolean,
    client: OkHttpClient,
    onFlashlight: () -> Unit,
    onWifi: () -> Unit,
    onBluetooth: () -> Unit,
    onOpenApp: (String) -> Unit,
) {
    var messages by remember { mutableStateOf(listOf<ChatMessage>()) }
    var input by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    fun send() {
        if (input.isBlank() || loading) return
        val userText = input
        messages = messages + ChatMessage("user", userText)
        input = ""
        loading = true
        scope.launch {
            val reply = withContext(Dispatchers.IO) { callChat(client, userText, messages) }
            messages = messages + ChatMessage("assistant", reply)
            loading = false
            listState.animateScrollToItem((messages.size - 1).coerceAtLeast(0))
        }
    }

    MaterialTheme(colorScheme = darkColorScheme(background = Bg, surface = Surface, primary = Amber)) {
        Scaffold(
            containerColor = Bg,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("Friday", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                            Text("Quick chat", color = TextMuted, fontSize = 11.sp)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Bg)
                )
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding).fillMaxSize()) {

                // Quick actions row
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionButton(Icons.Filled.FlashOn, "Flashlight", onFlashlight)
                    QuickActionButton(Icons.Filled.Wifi, "Wi-Fi", onWifi)
                    QuickActionButton(Icons.Filled.Bluetooth, "Bluetooth", onBluetooth)
                    QuickActionButton(Icons.Filled.Apps, "Camera app") { onOpenApp("com.android.camera") }
                }

                // Chat history
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(messages) { msg -> ChatBubble(msg) }
                    if (loading) {
                        item {
                            Text("Friday is working…", color = TextMuted, fontSize = 12.sp,
                                modifier = Modifier.padding(4.dp))
                        }
                    }
                }

                // Input bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Surface, RoundedCornerShape(16.dp))
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Message Friday…", color = TextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Amber,
                            unfocusedBorderColor = Border,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                    IconButton(onClick = { send() }, enabled = input.isNotBlank() && !loading) {
                        Icon(Icons.Filled.Send, contentDescription = "Send", tint = Amber)
                    }
                }
            }
        }
    }
}

@Composable
fun QuickActionButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .background(Surface, RoundedCornerShape(12.dp))
            .padding(vertical = 10.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconButton(onClick = onClick, modifier = Modifier.size(24.dp)) {
            Icon(icon, contentDescription = label, tint = Amber)
        }
        Text(label, color = TextMuted, fontSize = 10.sp)
    }
}

@Composable
fun ChatBubble(msg: ChatMessage) {
    val isUser = msg.role == "user"
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start) {
        Box(
            modifier = Modifier
                .background(if (isUser) Amber else Surface, RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .widthIn(max = 280.dp)
        ) {
            Text(msg.text, color = if (isUser) Bg else TextPrimary, fontSize = 13.5.sp)
        }
    }
}

private fun callChat(client: OkHttpClient, message: String, history: List<ChatMessage>): String {
    return try {
        val historyJson = JSONArray()
        history.forEach {
            historyJson.put(JSONObject().put("role", it.role).put("content", it.text))
        }
        val body = JSONObject()
            .put("message", message)
            .put("history", historyJson)
            .toString()
            .toRequestBody("application/json".toMediaType())

        val req = Request.Builder().url("$BACKEND_URL/chat").post(body).build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return "Couldn't reach Friday backend (${resp.code})."
            val text = resp.body?.string() ?: return "No response."
            JSONObject(text).optString("reply", "No reply.")
        }
    } catch (e: Exception) {
        "Offline — check your connection or backend URL."
    }
}
