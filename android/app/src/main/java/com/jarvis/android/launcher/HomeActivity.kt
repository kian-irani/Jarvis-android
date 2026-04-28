package com.jarvis.android.launcher

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.android.network.JarvisClient
import kotlinx.coroutines.launch

val JarvisBlue = Color(0xFF00D4FF)
val JarvisGreen = Color(0xFF00FF88)
val JarvisDark = Color(0xFF0A0A0F)
val JarvisCard = Color(0xFF0D1117)

class HomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            JarvisLauncherScreen()
        }
    }

    override fun onBackPressed() {
        // بلاک بک — لانچر باید بمونه
    }

    @Composable
    fun JarvisLauncherScreen() {
        val scope = rememberCoroutineScope()
        var chatInput by remember { mutableStateOf("") }
        var chatResponse by remember { mutableStateOf("JARVIS آماده‌ست...") }
        var nodesCount by remember { mutableStateOf(0) }
        var isLoading by remember { mutableStateOf(false) }
        val apps = remember { getInstalledApps() }

        LaunchedEffect(Unit) {
            try {
                val health = JarvisClient.getHealth()
                nodesCount = health.nodes_online
            } catch(e: Exception) {}
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(JarvisDark)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.horizontalGradient(listOf(Color(0xFF001A2E), Color(0xFF0A0A1A))))
                        .padding(16.dp)
                ) {
                    Column {
                        Text(
                            "⚡ J.A.R.V.I.S",
                            color = JarvisBlue,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 3.sp
                        )
                        Text(
                            "● $nodesCount Node آنلاین",
                            color = JarvisGreen,
                            fontSize = 12.sp
                        )
                    }
                }

                // Chat Box
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = JarvisCard)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(chatResponse, color = JarvisBlue, fontSize = 13.sp, modifier = Modifier.fillMaxWidth().heightIn(min = 40.dp, max = 100.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = chatInput,
                                onValueChange = { chatInput = it },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("پیام...", color = Color(0xFF6688AA), fontSize = 12.sp) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = JarvisBlue,
                                    unfocusedBorderColor = Color(0xFF334455),
                                    focusedTextColor = JarvisBlue,
                                    unfocusedTextColor = JarvisBlue
                                ),
                                singleLine = true
                            )
                            Button(
                                onClick = {
                                    if (chatInput.isNotBlank()) {
                                        val msg = chatInput
                                        chatInput = ""
                                        isLoading = true
                                        chatResponse = "در حال پردازش..."
                                        scope.launch {
                                            try {
                                                val r = JarvisClient.chat(msg)
                                                chatResponse = r
                                            } catch(e: Exception) {
                                                chatResponse = "خطا: ${e.message}"
                                            }
                                            isLoading = false
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF003366))
                            ) {
                                Text("ارسال", color = JarvisBlue, fontSize = 12.sp)
                            }
                        }
                    }
                }

                // Apps Grid
                Text(
                    "اپ‌ها",
                    color = Color(0xFF6688AA),
                    fontSize = 11.sp,
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(apps) { app ->
                        AppItem(app)
                    }
                }
            }
        }
    }

    @Composable
    fun AppItem(app: AppInfo) {
        Column(
            modifier = Modifier
                .background(JarvisCard, RoundedCornerShape(10.dp))
                .clickable {
                    try {
                        val intent = packageManager.getLaunchIntentForPackage(app.packageName)
                        intent?.let { startActivity(it) }
                    } catch(e: Exception) {}
                }
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("📱", fontSize = 28.sp)
            Text(
                app.name,
                color = JarvisBlue,
                fontSize = 9.sp,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }
    }

    data class AppInfo(val name: String, val packageName: String)

    private fun getInstalledApps(): List<AppInfo> {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        return packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            .filter { it.activityInfo.packageName != packageName }
            .map { AppInfo(it.loadLabel(packageManager).toString(), it.activityInfo.packageName) }
            .sortedBy { it.name }
    }
}
