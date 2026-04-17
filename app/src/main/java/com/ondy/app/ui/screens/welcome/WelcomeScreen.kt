package com.ondy.app.ui.screens.welcome

import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

private fun isNotificationListenerEnabled(context: Context): Boolean {
    val pkgName = context.packageName
    val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
    return flat?.contains(pkgName) == true
}

private fun isIgnoringBatteryOptimizations(context: Context): Boolean {
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    return powerManager.isIgnoringBatteryOptimizations(context.packageName)
}

private fun openNotificationListenerSettings(context: Context) {
    val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}

private fun requestIgnoreBatteryOptimization(context: Context) {
    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
    intent.data = android.net.Uri.parse("package:${context.packageName}")
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        val fallbackIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        fallbackIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(fallbackIntent)
    }
}

private fun checkPermissions(context: Context): Pair<Boolean, Boolean> {
    return Pair(
        isNotificationListenerEnabled(context),
        isIgnoringBatteryOptimizations(context)
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WelcomeScreen(
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var hasNotificationAccess by remember { mutableStateOf(false) }
    var hasBatteryExemption by remember { mutableStateOf(false) }

    // Initial load
    LaunchedEffect(Unit) {
        val (notifAccess, batteryExempt) = checkPermissions(context)
        hasNotificationAccess = notifAccess
        hasBatteryExemption = batteryExempt
    }

    // Periodic check every 500ms while on permission pages
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(500)
            val (notifAccess, batteryExempt) = checkPermissions(context)
            if (notifAccess != hasNotificationAccess || batteryExempt != hasBatteryExemption) {
                hasNotificationAccess = notifAccess
                hasBatteryExemption = batteryExempt
            }
        }
    }

    val pagerState = rememberPagerState(initialPage = 0) { 4 }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
            key = { it }
        ) { page ->
            WelcomePageContent(pageIndex = page)
        }

        PageIndicator(
            pageCount = 4,
            currentPage = pagerState.currentPage,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Action buttons based on current page
        when (pagerState.currentPage) {
            1 -> {
                val isGranted = hasNotificationAccess
                OutlinedButton(
                    onClick = { openNotificationListenerSettings(context) },
                    enabled = !isGranted,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (isGranted) "Access Granted ✓" else "Grant Access",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
            2 -> {
                val isGranted = hasBatteryExemption
                OutlinedButton(
                    onClick = { requestIgnoreBatteryOptimization(context) },
                    enabled = !isGranted,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (isGranted) "Already Enabled ✓" else "Disable Optimization",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        Button(
            onClick = {
                if (pagerState.currentPage < 3) {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                    }
                } else {
                    if (hasNotificationAccess && hasBatteryExemption) {
                        onComplete()
                    }
                }
            },
            enabled = pagerState.currentPage != 3 || (hasNotificationAccess && hasBatteryExemption),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text(
                text = if (pagerState.currentPage == 3) {
                    if (hasNotificationAccess && hasBatteryExemption) "Get Started" else "Grant Permissions"
                } else "Next",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun WelcomePageContent(pageIndex: Int) {
    when (pageIndex) {
        0 -> WelcomePageLayout(
            icon = Icons.Default.RocketLaunch,
            title = "Welcome to Ondy",
            description = "Notifications On-Demand.\n\nOndy intercepts notifications from your selected apps and stores them for you to review later. Schedule summary notifications to stay on top of your alerts."
        )
        1 -> WelcomePageLayout(
            icon = Icons.Default.Notifications,
            title = "Notification Access",
            description = "Ondy needs permission to access your notifications to intercept and store them.\n\nTap the button below to enable notification access in system settings."
        )
        2 -> WelcomePageLayout(
            icon = Icons.Default.BatteryAlert,
            title = "Battery Optimization",
            description = "To ensure Ondy works reliably, please disable battery optimization.\n\n⚠️ Note: Some phone manufacturers (Samsung, Xiaomi, OnePlus, Oppo, Vivo, Huawei, etc.) have additional restrictions. You may need to manually allow background activity in your device's app settings."
        )
        else -> WelcomePageLayout(
            icon = Icons.Default.Security,
            title = "Privacy First",
            description = "Your data stays on your device. We don't collect any data - your privacy is our priority.\n\n• All data stored locally\n• No internet access required\n• No analytics or tracking"
        )
    }
}

@Composable
private fun WelcomePageLayout(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

@Composable
private fun PageIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(pageCount) { index ->
            val width = if (index == currentPage) 24f else 8f
            val animatedWidth by animateFloatAsState(
                targetValue = width,
                animationSpec = tween(300),
                label = "width"
            )

            Box(
                modifier = Modifier
                    .height(8.dp)
                    .width(animatedWidth.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        if (index == currentPage) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        }
                    )
            )
        }
    }
}