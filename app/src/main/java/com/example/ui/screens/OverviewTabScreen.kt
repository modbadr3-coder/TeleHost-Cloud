package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.DeploymentLog
import com.example.data.model.TelegramBot
import com.example.data.model.WebSiteProject
import com.example.ui.components.RainbowDeveloperContactCard
import com.example.ui.theme.JetBrainsMonoFontFamily
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun OverviewTabScreen(
    bots: List<TelegramBot>,
    sites: List<WebSiteProject>,
    logs: List<DeploymentLog>,
    onNavigateToBots: () -> Unit,
    onNavigateToSites: () -> Unit,
    onNavigateToDomains: () -> Unit
) {
    val activeBotsCount = bots.count { it.status == "running" || it.status == "webhook_active" }
    val totalProcessedUpdates = bots.sumOf { it.processedUpdatesCount }
    val deployedSitesCount = sites.count { it.buildStatus == "deployed" }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("overview_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
                border = BorderStroke(1.dp, Color(0xFF1F2937))
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_datacenter_banner),
                            contentDescription = "عنقود الخوادم السحابية",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(155.dp),
                            contentScale = ContentScale.Crop
                        )
                        Surface(
                            color = Color(0xFF10B981).copy(alpha = 0.2f),
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, Color(0xFF10B981)),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(Color(0xFF10B981))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "CLOUD CLUSTER ONLINE",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFF10B981)
                                )
                            }
                        }
                    }

                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "مركز قيادة الخوادم السحابية الدائمة",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color(0xFFF9FAFB)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "جميع البوتات والمواقع متصلة مباشرة بقاعدة البيانات السحابية الدائمة (Cloud Firestore Enterprise) مع محركات اتصال حقيقي بـ Telegram Bot API وبوابات النشر وفحص Google DoH DNS.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF9CA3AF)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = onNavigateToBots,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("quick_add_bot_btn"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF06B6D4),
                                    contentColor = Color(0xFF0B0F19)
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.SmartToy, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("إدارة البوتات", fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = onNavigateToSites,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("quick_deploy_site_btn"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF10B981),
                                    contentColor = Color(0xFF0B0F19)
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("رفع موقع حي", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        item {
            RainbowDeveloperContactCard()
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricStatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.SmartToy,
                    label = "البوتات النشطة",
                    value = "$activeBotsCount / ${bots.size}",
                    accentColor = Color(0xFF06B6D4)
                )
                MetricStatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.PlayArrow,
                    label = "الرسائل المعالجة",
                    value = totalProcessedUpdates.toString(),
                    accentColor = Color(0xFF10B981)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricStatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.CloudQueue,
                    label = "المواقع المنشورة",
                    value = "$deployedSitesCount / ${sites.size}",
                    accentColor = Color(0xFFF59E0B)
                )
                MetricStatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Storage,
                    label = "سجلات العمليات",
                    value = logs.size.toString(),
                    accentColor = Color(0xFF06B6D4)
                )
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
                border = BorderStroke(1.dp, Color(0xFF1F2937))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "فاحص النطاقات الحرة وغير المستخدمة (Google DNS)",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFFF9FAFB)
                        )
                        Text(
                            text = "تحقق من توافر النطاقات الحقيقية عبر استعلام DNS-over-HTTPS الفوري",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF9CA3AF)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = onNavigateToDomains,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1F2937),
                            contentColor = Color(0xFF06B6D4)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("open_dns_checker_btn")
                    ) {
                        Icon(Icons.Default.Dns, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("فحص نطاق")
                    }
                }
            }
        }

        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = Color(0xFF06B6D4),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "السجل اللحظي لعمليات الخادم (Live Cloud Audit Logs)",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFFF9FAFB)
                )
            }
        }

        if (logs.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
                    border = BorderStroke(1.dp, Color(0xFF1F2937))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "لا توجد عمليات مسجلة بعد. قم بإضافة بوت تلجرام حقيقي أو نشر موقع ويب لبدء التسجيل الفوري.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF9CA3AF)
                        )
                    }
                }
            }
        } else {
            items(logs.take(25), key = { it.id.ifBlank { it.hashCode().toString() } }) { logItem ->
                LogEntryRowCard(logItem)
            }
        }
    }
}

@Composable
private fun MetricStatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    accentColor: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
        border = BorderStroke(1.dp, Color(0xFF1F2937))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = accentColor.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF9CA3AF)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontFamily = JetBrainsMonoFontFamily,
                color = Color(0xFFF9FAFB),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun LogEntryRowCard(log: DeploymentLog) {
    val formatter = SimpleDateFormat("HH:mm:ss", Locale.US)
    val timeStr = log.createdAt?.toDate()?.let { formatter.format(it) } ?: "NOW"
    val badgeColor = when (log.category) {
        "bot_engine" -> Color(0xFF06B6D4)
        "site_deploy" -> Color(0xFF10B981)
        else -> Color(0xFFF59E0B)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
        border = BorderStroke(1.dp, Color(0xFF1F2937))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = badgeColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, badgeColor)
                    ) {
                        Text(
                            text = log.category.uppercase(Locale.US),
                            style = MaterialTheme.typography.labelMedium,
                            color = badgeColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = log.targetName,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFFF9FAFB)
                    )
                }
                Text(
                    text = timeStr,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF9CA3AF)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = log.message,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = JetBrainsMonoFontFamily,
                color = Color(0xFFD1D5DB)
            )
        }
    }
}
