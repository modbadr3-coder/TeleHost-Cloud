package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.BotAutoReplyRule
import com.example.data.model.TelegramBot
import com.example.data.model.TelegramChatInspection
import com.example.data.model.TelegramWebhookInfo
import com.example.ui.theme.JetBrainsMonoFontFamily

@Composable
fun TelegramBotsTabScreen(
    bots: List<TelegramBot>,
    isBusy: Boolean,
    webhookDiagnostic: TelegramWebhookInfo?,
    chatInspection: TelegramChatInspection?,
    parseRules: (TelegramBot) -> List<BotAutoReplyRule>,
    onVerifyAndAddBot: (name: String, token: String, welcome: String) -> Unit,
    onToggleBotState: (TelegramBot) -> Unit,
    onFetchWebhookDiagnostics: (TelegramBot) -> Unit,
    onConfigureWebhook: (TelegramBot, String, Boolean) -> Unit,
    onSyncNativeCommandMenu: (TelegramBot, List<BotAutoReplyRule>) -> Unit,
    onUpdateBotProfileBio: (TelegramBot, String, String) -> Unit,
    onUpdateBotOfficialName: (TelegramBot, String) -> Unit = { _, _ -> },
    onInspectChat: (TelegramBot, String) -> Unit,
    onSendDirectMessage: (TelegramBot, String, String, String, String) -> Unit,
    onSendAndPinMessage: (TelegramBot, String, String, String, String) -> Unit = { _, _, _, _, _ -> },
    onSendPoll: (TelegramBot, String, String, List<String>) -> Unit = { _, _, _, _ -> },
    onSaveRulesAndWelcome: (TelegramBot, String, List<BotAutoReplyRule>) -> Unit,
    onDeleteBot: (TelegramBot) -> Unit
) {
    var selectedBotId by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedBot = bots.firstOrNull { it.id == selectedBotId }

    var botNameInput by rememberSaveable { mutableStateOf("") }
    var botTokenInput by rememberSaveable { mutableStateOf("") }
    var welcomeInput by rememberSaveable { mutableStateOf("مرحبا بك! هذا البوت مستضاف ويعمل مباشرة عبر خوادم TeleHost Cloud.") }

    if (selectedBot != null) {
        BackHandler {
            selectedBotId = null
        }
        BotControlDetailView(
            bot = selectedBot,
            isBusy = isBusy,
            webhookDiagnostic = webhookDiagnostic,
            chatInspection = chatInspection,
            initialRules = remember(selectedBot.id, selectedBot.autoReplyRulesJson) {
                parseRules(selectedBot)
            },
            onBack = { selectedBotId = null },
            onToggleState = { onToggleBotState(selectedBot) },
            onFetchWebhookInfo = { onFetchWebhookDiagnostics(selectedBot) },
            onSaveWebhook = { url, drop -> onConfigureWebhook(selectedBot, url, drop) },
            onSyncCommandsMenu = { rules -> onSyncNativeCommandMenu(selectedBot, rules) },
            onUpdateBio = { desc, shortBio -> onUpdateBotProfileBio(selectedBot, desc, shortBio) },
            onUpdateOfficialName = { newName -> onUpdateBotOfficialName(selectedBot, newName) },
            onInspectChat = { chatId -> onInspectChat(selectedBot, chatId) },
            onSendDirect = { chatId, msg, btnTxt, btnUrl ->
                onSendDirectMessage(selectedBot, chatId, msg, btnTxt, btnUrl)
            },
            onSendAndPin = { chatId, msg, btnTxt, btnUrl ->
                onSendAndPinMessage(selectedBot, chatId, msg, btnTxt, btnUrl)
            },
            onSendPoll = { chatId, q, opts ->
                onSendPoll(selectedBot, chatId, q, opts)
            },
            onSaveRules = { welcome, rules -> onSaveRulesAndWelcome(selectedBot, welcome, rules) },
            onDelete = {
                onDeleteBot(selectedBot)
                selectedBotId = null
            }
        )
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("bots_tab_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
                border = BorderStroke(1.dp, Color(0xFF1F2937))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = null,
                            tint = Color(0xFF06B6D4),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "ربط واستضافة بوت تلجرام جديد (Telegram API)",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color(0xFFF9FAFB)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "أدخل التوكن الحقيقي المستخرج من @BotFather ليقوم الخادم بالاتصال بـ api.telegram.org/bot<TOKEN>/getMe والتحقق من هوية البوت وتشغيله.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF9CA3AF)
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = botTokenInput,
                        onValueChange = { botTokenInput = it },
                        label = { Text("Telegram Bot Token (من BotFather)") },
                        placeholder = { Text("123456789:AAHdqTcvCH1vGWJxfSeofSAs0K5PALDsaw") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("bot_token_input"),
                        colors = darkTextFieldColors()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = botNameInput,
                        onValueChange = { botNameInput = it },
                        label = { Text("اسم تعريفي للبوت (اختياري - يجلب تلقائياً من تلجرام)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("bot_name_input"),
                        colors = darkTextFieldColors()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = welcomeInput,
                        onValueChange = { welcomeInput = it },
                        label = { Text("رسالة الرد التلقائي عند إرسال /start") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("bot_welcome_input"),
                        colors = darkTextFieldColors()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            onVerifyAndAddBot(botNameInput, botTokenInput, welcomeInput)
                            botTokenInput = ""
                            botNameInput = ""
                        },
                        enabled = !isBusy && botTokenInput.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("verify_add_bot_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF06B6D4),
                            contentColor = Color(0xFF0B0F19)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isBusy) "جاري التحقق من خادم تلجرام..." else "تحقق من التوكن وارفع البوت على الخادم",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "البوتات المستضافة على الخادم السحابي (${bots.size})",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFFF9FAFB)
            )
        }

        if (bots.isEmpty()) {
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
                            text = "لا توجد بوتات مضافة حالياً. أضف توكن البوت أعلاه لربطه وتشغيله فوراً.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF9CA3AF)
                        )
                    }
                }
            }
        } else {
            items(bots, key = { it.id }) { bot ->
                HostedBotCard(
                    bot = bot,
                    onToggleRuntime = { onToggleBotState(bot) },
                    onOpenControlPanel = { selectedBotId = bot.id }
                )
            }
        }
    }
}

@Composable
private fun HostedBotCard(
    bot: TelegramBot,
    onToggleRuntime: () -> Unit,
    onOpenControlPanel: () -> Unit
) {
    val isRunning = bot.status == "running"
    val statusColor = when (bot.status) {
        "running" -> Color(0xFF10B981)
        "webhook_active" -> Color(0xFF06B6D4)
        else -> Color(0xFFF59E0B)
    }
    val statusLabel = when (bot.status) {
        "running" -> "ONLINE POLLING"
        "webhook_active" -> "WEBHOOK ACTIVE"
        else -> "STOPPED"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("bot_card_${bot.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
        border = BorderStroke(1.dp, Color(0xFF1F2937))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = bot.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFFF9FAFB)
                    )
                    Text(
                        text = "@${bot.username}",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = JetBrainsMonoFontFamily,
                        color = Color(0xFF06B6D4)
                    )
                }
                Surface(
                    color = statusColor.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, statusColor)
                ) {
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "الرسائل المعالجة: ${bot.processedUpdatesCount}",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = JetBrainsMonoFontFamily,
                    color = Color(0xFF9CA3AF)
                )
                Text(
                    text = "آخر Update ID: ${bot.lastUpdateId}",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = JetBrainsMonoFontFamily,
                    color = Color(0xFF9CA3AF)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onToggleRuntime,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("toggle_bot_btn_${bot.id}"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRunning) Color(0xFFEF4444) else Color(0xFF10B981),
                        contentColor = Color(0xFF0B0F19)
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = if (isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isRunning) "إيقاف المحرك" else "تشغيل البوت الآن",
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = onOpenControlPanel,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("configure_bot_btn_${bot.id}"),
                    border = BorderStroke(1.dp, Color(0xFF06B6D4)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF06B6D4)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("لوحة التحكم الشاملة")
                }
            }
        }
    }
}

@Composable
private fun BotControlDetailView(
    bot: TelegramBot,
    isBusy: Boolean,
    webhookDiagnostic: TelegramWebhookInfo?,
    chatInspection: TelegramChatInspection?,
    initialRules: List<BotAutoReplyRule>,
    onBack: () -> Unit,
    onToggleState: () -> Unit,
    onFetchWebhookInfo: () -> Unit,
    onSaveWebhook: (String, Boolean) -> Unit,
    onSyncCommandsMenu: (List<BotAutoReplyRule>) -> Unit,
    onUpdateBio: (String, String) -> Unit,
    onUpdateOfficialName: (String) -> Unit,
    onInspectChat: (String) -> Unit,
    onSendDirect: (String, String, String, String) -> Unit,
    onSendAndPin: (String, String, String, String) -> Unit,
    onSendPoll: (String, String, List<String>) -> Unit,
    onSaveRules: (String, List<BotAutoReplyRule>) -> Unit,
    onDelete: () -> Unit
) {
    var activeSection by rememberSaveable { mutableStateOf("ENGINE") } // "ENGINE", "COMMANDS", "MESSAGING", "PROFILE"

    var webhookInput by rememberSaveable(bot.id) { mutableStateOf(bot.webhookUrl) }
    var dropPendingUpdates by rememberSaveable(bot.id) { mutableStateOf(false) }

    var welcomeInput by rememberSaveable(bot.id) { mutableStateOf(bot.welcomeMessage) }
    var newCmdInput by rememberSaveable(bot.id) { mutableStateOf("") }
    var newRespInput by rememberSaveable(bot.id) { mutableStateOf("") }
    val rulesList = remember(bot.id) {
        mutableStateListOf<BotAutoReplyRule>().apply { addAll(initialRules) }
    }

    var directChatId by rememberSaveable(bot.id) { mutableStateOf("") }
    var directMessage by rememberSaveable(bot.id) { mutableStateOf("") }
    var inlineBtnTitle by rememberSaveable(bot.id) { mutableStateOf("") }
    var inlineBtnUrl by rememberSaveable(bot.id) { mutableStateOf("") }

    var pollQuestion by rememberSaveable(bot.id) { mutableStateOf("") }
    var pollOption1 by rememberSaveable(bot.id) { mutableStateOf("ممتاز جداً") }
    var pollOption2 by rememberSaveable(bot.id) { mutableStateOf("جيد ويحتاج تطوير") }
    var pollOption3 by rememberSaveable(bot.id) { mutableStateOf("أقترح إضافة مميزات جديدة") }

    var officialBotNameInput by rememberSaveable(bot.id) { mutableStateOf(bot.name) }
    var botFullDescription by rememberSaveable(bot.id) {
        mutableStateOf("مرحباً بك في البوت الرسمي المدار عبر خوادم TeleHost Cloud الدائمة.")
    }
    var botShortBio by rememberSaveable(bot.id) {
        mutableStateOf("بوت رسمي متصل بخدمة TeleHost Cloud")
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("bot_detail_view"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("back_from_bot_detail")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع",
                            tint = Color(0xFFF9FAFB)
                        )
                    }
                    Column {
                        Text(
                            text = "لوحة تحكم البوت: ${bot.name}",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color(0xFFF9FAFB)
                        )
                        Text(
                            text = "@${bot.username}",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = JetBrainsMonoFontFamily,
                            color = Color(0xFF06B6D4)
                        )
                    }
                }
                IconButton(onClick = onDelete, modifier = Modifier.testTag("delete_bot_button")) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "حذف البوت",
                        tint = Color(0xFFEF4444)
                    )
                }
            }
        }

        // Sub-navigation chips for organized control
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "ENGINE" to "المحرك والـ Webhook",
                    "COMMANDS" to "الأوامر وقائمة Menu",
                    "MESSAGING" to "الإرسال وفحص القنوات",
                    "PROFILE" to "هوية البوت"
                ).forEach { (key, title) ->
                    FilterChip(
                        selected = activeSection == key,
                        onClick = { activeSection = key },
                        label = { Text(title) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF06B6D4).copy(alpha = 0.22f),
                            selectedLabelColor = Color(0xFF06B6D4)
                        )
                    )
                }
            }
        }

        when (activeSection) {
            "ENGINE" -> {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
                        border = BorderStroke(1.dp, Color(0xFF1F2937))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "محرك الاستطلاع الفوري (Long-Polling) والـ Webhook",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color(0xFFF9FAFB)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "شغّل محرك الاستطلاع المباشر للرد التلقائي على رسائل المستخدمين أو اضبط رابط HTTPS Webhook خاص بك مع فحص حالة getWebhookInfo.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF9CA3AF)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = onToggleState,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (bot.status == "running") Color(0xFFEF4444) else Color(0xFF10B981),
                                    contentColor = Color(0xFF0B0F19)
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = if (bot.status == "running") "إيقاف محرك الاستطلاع الفوري (Running)" else "تشغيل محرك الاستطلاع الفوري (Start Long-Polling)",
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            OutlinedTextField(
                                value = webhookInput,
                                onValueChange = { webhookInput = it },
                                label = { Text("HTTPS Webhook URL (اتركه فارغاً لإلغاء الـ Webhook)") },
                                placeholder = { Text("https://yourdomain.com/telegram/webhook") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = darkTextFieldColors()
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Checkbox(
                                    checked = dropPendingUpdates,
                                    onCheckedChange = { dropPendingUpdates = it }
                                )
                                Text(
                                    text = "تجاهل وحذف الرسائل المعلقة القديمة (drop_pending_updates)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFD1D5DB)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { onSaveWebhook(webhookInput, dropPendingUpdates) },
                                    enabled = !isBusy,
                                    modifier = Modifier.weight(1f),
                                    border = BorderStroke(1.dp, Color(0xFF06B6D4)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF06B6D4)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("تطبيق Webhook")
                                }

                                OutlinedButton(
                                    onClick = onFetchWebhookInfo,
                                    enabled = !isBusy,
                                    modifier = Modifier.weight(1f),
                                    border = BorderStroke(1.dp, Color(0xFF10B981)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF10B981)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("فحص getWebhookInfo")
                                }
                            }

                            if (webhookDiagnostic != null) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Surface(
                                    color = Color(0xFF0B0F19),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, Color(0xFF06B6D4)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = "تشخيص Webhook المباشر من خادم تلجرام:",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = Color(0xFF06B6D4)
                                        )
                                        Text(
                                            text = "الرابط النشط: ${webhookDiagnostic.url.ifBlank { "غير مفعل (Long-Polling Mode)" }}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontFamily = JetBrainsMonoFontFamily,
                                            color = Color(0xFFF9FAFB)
                                        )
                                        Text(
                                            text = "التحديثات المعلقة (Pending Updates): ${webhookDiagnostic.pendingUpdateCount}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontFamily = JetBrainsMonoFontFamily,
                                            color = Color(0xFF10B981)
                                        )
                                        Text(
                                            text = "أقصى اتصالات متزامنة: ${webhookDiagnostic.maxConnections} | IP: ${webhookDiagnostic.ipAddress.ifBlank { "Default" }}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontFamily = JetBrainsMonoFontFamily,
                                            color = Color(0xFF9CA3AF)
                                        )
                                        if (webhookDiagnostic.lastErrorMessage.isNotBlank()) {
                                            Text(
                                                text = "آخر خطأ مسجل: ${webhookDiagnostic.lastErrorMessage}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color(0xFFEF4444)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            "COMMANDS" -> {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
                        border = BorderStroke(1.dp, Color(0xFF1F2937))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "برمجة أوامر البوت ومزامنة زر القائمة الرسمي (setMyCommands)",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color(0xFFF9FAFB)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = welcomeInput,
                                onValueChange = { welcomeInput = it },
                                label = { Text("رسالة الترحيب عند أمر /start") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = darkTextFieldColors()
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            rulesList.forEachIndexed { idx, rule ->
                                Surface(
                                    color = Color(0xFF0B0F19),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, Color(0xFF1F2937)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = rule.command,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = Color(0xFF10B981)
                                            )
                                            Text(
                                                text = rule.responseText,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color(0xFFF9FAFB)
                                            )
                                        }
                                        IconButton(onClick = { rulesList.removeAt(idx) }) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "حذف الأمر",
                                                tint = Color(0xFFEF4444)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = newCmdInput,
                                    onValueChange = { newCmdInput = it },
                                    label = { Text("الأمر (مثال /price)") },
                                    singleLine = true,
                                    modifier = Modifier.weight(0.4f),
                                    colors = darkTextFieldColors()
                                )
                                OutlinedTextField(
                                    value = newRespInput,
                                    onValueChange = { newRespInput = it },
                                    label = { Text("الرد التلقائي") },
                                    singleLine = true,
                                    modifier = Modifier.weight(0.6f),
                                    colors = darkTextFieldColors()
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = {
                                    if (newCmdInput.isNotBlank() && newRespInput.isNotBlank()) {
                                        rulesList.add(BotAutoReplyRule(newCmdInput.trim(), newRespInput.trim()))
                                        newCmdInput = ""
                                        newRespInput = ""
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                border = BorderStroke(1.dp, Color(0xFF10B981)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF10B981)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("إضافة أمر للبوت")
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = { onSaveRules(welcomeInput, rulesList.toList()) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("save_bot_rules_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF10B981),
                                    contentColor = Color(0xFF0B0F19)
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("حفظ الأوامر في الخادم السحابي", fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { onSyncCommandsMenu(rulesList.toList()) },
                                enabled = !isBusy,
                                modifier = Modifier.fillMaxWidth(),
                                border = BorderStroke(1.dp, Color(0xFF06B6D4)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF06B6D4)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("مزامنة الأوامر مع زر القائمة (Menu) في تطبيق تلجرام")
                            }
                        }
                    }
                }
            }

            "MESSAGING" -> {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
                        border = BorderStroke(1.dp, Color(0xFF1F2937))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "إرسال رسائل تفاعلية بأزرار وفحص القنوات (getChat & Inline Keyboard)",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color(0xFFF9FAFB)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = directChatId,
                                    onValueChange = { directChatId = it },
                                    label = { Text("معرف المحادثة أو القناة (Chat ID أو @channel)") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("direct_chat_id_input"),
                                    colors = darkTextFieldColors()
                                )
                                OutlinedButton(
                                    onClick = { onInspectChat(directChatId) },
                                    enabled = !isBusy && directChatId.isNotBlank(),
                                    border = BorderStroke(1.dp, Color(0xFF06B6D4)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF06B6D4)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("فحص")
                                }
                            }

                            if (chatInspection != null) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Surface(
                                    color = Color(0xFF0B0F19),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, Color(0xFF10B981)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = "بيانات القناة / المحادثة: ${chatInspection.title}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = Color(0xFF10B981)
                                        )
                                        Text(
                                            text = "ID: ${chatInspection.id} | النوع: ${chatInspection.type} | الأعضاء: ${chatInspection.memberCount}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontFamily = JetBrainsMonoFontFamily,
                                            color = Color(0xFFF9FAFB)
                                        )
                                        if (chatInspection.description.isNotBlank()) {
                                            Text(
                                                text = "الوصف: ${chatInspection.description}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color(0xFF9CA3AF)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = directMessage,
                                onValueChange = { directMessage = it },
                                label = { Text("نص الرسالة المراد إرسالها") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("direct_message_input"),
                                colors = darkTextFieldColors()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = inlineBtnTitle,
                                    onValueChange = { inlineBtnTitle = it },
                                    label = { Text("عنوان زر شفاف (اختياري)") },
                                    placeholder = { Text("زيارة موقعنا الحي") },
                                    singleLine = true,
                                    modifier = Modifier.weight(0.45f),
                                    colors = darkTextFieldColors()
                                )
                                OutlinedTextField(
                                    value = inlineBtnUrl,
                                    onValueChange = { inlineBtnUrl = it },
                                    label = { Text("رابط الزر (https://...)") },
                                    singleLine = true,
                                    modifier = Modifier.weight(0.55f),
                                    colors = darkTextFieldColors()
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        onSendDirect(directChatId, directMessage, inlineBtnTitle, inlineBtnUrl)
                                        directMessage = ""
                                    },
                                    enabled = !isBusy && directChatId.isNotBlank() && directMessage.isNotBlank(),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("send_telegram_msg_btn"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF06B6D4),
                                        contentColor = Color(0xFF0B0F19)
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("إرسال مباشر", fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        onSendAndPin(directChatId, directMessage, inlineBtnTitle, inlineBtnUrl)
                                        directMessage = ""
                                    },
                                    enabled = !isBusy && directChatId.isNotBlank() && directMessage.isNotBlank(),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("send_and_pin_telegram_msg_btn"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF10B981),
                                        contentColor = Color(0xFF0B0F19)
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("إرسال وتثبيت بأعلى القناة", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
                        border = BorderStroke(1.dp, Color(0xFF06B6D4).copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "استوديو استطلاعات الرأي التفاعلية (Telegram Native Poll - sendPoll)",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color(0xFFF9FAFB)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "ينشئ استطلاع رأي رسمي حي داخل القناة أو المجموعة المحددة أعلاه عبر نقطة النهاية POST /bot<TOKEN>/sendPoll.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF9CA3AF)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = pollQuestion,
                                onValueChange = { pollQuestion = it },
                                label = { Text("سؤال الاستطلاع التفاعلي") },
                                placeholder = { Text("ما رأيكم في التحديثات السحابية الجديدة؟") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = darkTextFieldColors()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = pollOption1,
                                onValueChange = { pollOption1 = it },
                                label = { Text("الخيار الأول") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = darkTextFieldColors()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = pollOption2,
                                onValueChange = { pollOption2 = it },
                                label = { Text("الخيار الثاني") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = darkTextFieldColors()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = pollOption3,
                                onValueChange = { pollOption3 = it },
                                label = { Text("الخيار الثالث (اختياري)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = darkTextFieldColors()
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    onSendPoll(
                                        directChatId,
                                        pollQuestion,
                                        listOf(pollOption1, pollOption2, pollOption3).filter { it.isNotBlank() }
                                    )
                                    pollQuestion = ""
                                },
                                enabled = !isBusy && directChatId.isNotBlank() && pollQuestion.isNotBlank(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("send_telegram_poll_btn"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFF59E0B),
                                    contentColor = Color(0xFF0B0F19)
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("نشر استطلاع الرأي التفاعلي عبر sendPoll الآن", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            "PROFILE" -> {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
                        border = BorderStroke(1.dp, Color(0xFF1F2937))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF06B6D4))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "تغيير الاسم الرسمي للبوت على تلجرام (setMyName)",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color(0xFFF9FAFB)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = officialBotNameInput,
                                onValueChange = { officialBotNameInput = it },
                                label = { Text("الاسم الرسمي للبوت على خوادم تلجرام") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = darkTextFieldColors()
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = { onUpdateOfficialName(officialBotNameInput) },
                                enabled = !isBusy && officialBotNameInput.isNotBlank(),
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF06B6D4),
                                    contentColor = Color(0xFF0B0F19)
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("حفظ الاسم الجديد للبوت عبر setMyName", fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(18.dp))
                            Text(
                                text = "تحديث الوصف الرسمي والنبذة (setMyDescription & setMyShortDescription)",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color(0xFFF9FAFB)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = botFullDescription,
                                onValueChange = { botFullDescription = it },
                                label = { Text("الوصف الكامل للبوت (يظهر قبل الضغط على Start)") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = darkTextFieldColors()
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = botShortBio,
                                onValueChange = { botShortBio = it },
                                label = { Text("النبذة المختصرة (تظهر في صفحة بروفايل البوت)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = darkTextFieldColors()
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { onUpdateBio(botFullDescription, botShortBio) },
                                enabled = !isBusy,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF10B981),
                                    contentColor = Color(0xFF0B0F19)
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("تحديث بروفايل البوت على تلجرام الآن", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun darkTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color(0xFFF9FAFB),
    unfocusedTextColor = Color(0xFFF9FAFB),
    focusedBorderColor = Color(0xFF06B6D4),
    unfocusedBorderColor = Color(0xFF374151),
    focusedLabelColor = Color(0xFF06B6D4),
    unfocusedLabelColor = Color(0xFF9CA3AF),
    cursorColor = Color(0xFF06B6D4)
)
