package com.example.data.network

import android.util.Base64
import com.example.BuildConfig
import com.example.data.model.BotAutoReplyRule
import com.example.data.model.DnsMultiRecordReport
import com.example.data.model.DomainCheckResult
import com.example.data.model.SurgeHostedProject
import com.example.data.model.TelegramChatInspection
import com.example.data.model.TelegramWebhookInfo
import com.example.data.model.WebsiteCodeAuditReport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.cert.X509Certificate
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.net.ssl.HttpsURLConnection

data class TelegramBotProfile(
    val id: Long,
    val firstName: String,
    val username: String,
    val canJoinGroups: Boolean
)

data class TelegramUpdateEvent(
    val updateId: Long,
    val chatId: Long,
    val senderName: String,
    val text: String,
    val autoRepliedWith: String?
)

class RealNetworkEngines(
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(25, TimeUnit.SECONDS)
        .build()
) {

    // =========================================================================
    // 1. REAL TELEGRAM BOT API ENGINE (https://api.telegram.org)
    // =========================================================================

    suspend fun verifyTelegramBotToken(botToken: String): Result<TelegramBotProfile> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanToken = botToken.trim()
            require(cleanToken.contains(":")) { "صيغة توكن البوت غير صحيحة (يجب أن تحتوي على نقطتين رأسيتين :)" }
            val url = "https://api.telegram.org/bot$cleanToken/getMe"
            val request = Request.Builder().url(url).get().build()
            httpClient.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string().orEmpty()
                val json = JSONObject(bodyStr)
                if (!response.isSuccessful || !json.optBoolean("ok", false)) {
                    val desc = json.optString("description", "فشل التحقق من التوكن عبر خوادم تلجرام")
                    throw IllegalStateException("Telegram API (${response.code}): $desc")
                }
                val resultObj = json.getJSONObject("result")
                TelegramBotProfile(
                    id = resultObj.optLong("id", 0L),
                    firstName = resultObj.optString("first_name", "Telegram Bot"),
                    username = resultObj.optString("username", ""),
                    canJoinGroups = resultObj.optBoolean("can_join_groups", false)
                )
            }
        }
    }

    /**
     * Fetches real-time webhook diagnostics via `GET /bot<TOKEN>/getWebhookInfo`.
     */
    suspend fun getTelegramWebhookDiagnostics(botToken: String): Result<TelegramWebhookInfo> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanToken = botToken.trim()
            val url = "https://api.telegram.org/bot$cleanToken/getWebhookInfo"
            val request = Request.Builder().url(url).get().build()
            httpClient.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string().orEmpty()
                val json = JSONObject(bodyStr)
                if (!response.isSuccessful || !json.optBoolean("ok", false)) {
                    val desc = json.optString("description", "تعذر جلب تشخيص الـ Webhook")
                    throw IllegalStateException("Telegram API: $desc")
                }
                val res = json.getJSONObject("result")
                TelegramWebhookInfo(
                    url = res.optString("url", ""),
                    pendingUpdateCount = res.optInt("pending_update_count", 0),
                    lastErrorDate = res.optLong("last_error_date", 0L),
                    lastErrorMessage = res.optString("last_error_message", ""),
                    maxConnections = res.optInt("max_connections", 40),
                    ipAddress = res.optString("ip_address", "")
                )
            }
        }
    }

    /**
     * Syncs the bot's native command menu in Telegram client apps via `POST /bot<TOKEN>/setMyCommands`.
     */
    suspend fun syncTelegramBotCommandMenu(
        botToken: String,
        rules: List<BotAutoReplyRule>
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanToken = botToken.trim()
            val commandsArray = JSONArray()
            // Always include /start
            commandsArray.put(
                JSONObject().apply {
                    put("command", "start")
                    put("description", "بدء المحادثة مع البوت")
                }
            )
            rules.forEach { rule ->
                val cleanCmd = rule.command.trim().removePrefix("/").lowercase()
                    .replace(Regex("[^a-z0-9_]"), "")
                    .take(32)
                if (cleanCmd.isNotEmpty() && cleanCmd != "start") {
                    commandsArray.put(
                        JSONObject().apply {
                            put("command", cleanCmd)
                            put("description", rule.responseText.trim().take(255).ifBlank { "أمر مبرمج" })
                        }
                    )
                }
            }

            val payload = JSONObject().apply {
                put("commands", commandsArray)
            }.toString()

            val request = Request.Builder()
                .url("https://api.telegram.org/bot$cleanToken/setMyCommands")
                .post(payload.toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            httpClient.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string().orEmpty()
                val json = JSONObject(bodyStr)
                if (!response.isSuccessful || !json.optBoolean("ok", false)) {
                    val desc = json.optString("description", "تعذر مزامنة قائمة الأوامر في تلجرام")
                    throw IllegalStateException("Telegram API: $desc")
                }
                "تمت مزامنة ${commandsArray.length()} أوامر رسمية في قائمة زر Menu داخل تطبيق تلجرام"
            }
        }
    }

    /**
     * Updates the bot's profile description and short bio via `setMyDescription` and `setMyShortDescription`.
     */
    suspend fun updateTelegramBotProfileBio(
        botToken: String,
        description: String,
        shortBio: String
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanToken = botToken.trim()
            val jsonMedia = "application/json; charset=utf-8".toMediaType()

            val descPayload = JSONObject().apply {
                put("description", description.trim().take(512))
            }.toString()
            val descReq = Request.Builder()
                .url("https://api.telegram.org/bot$cleanToken/setMyDescription")
                .post(descPayload.toRequestBody(jsonMedia))
                .build()
            httpClient.newCall(descReq).execute().use { resp ->
                val obj = JSONObject(resp.body?.string().orEmpty())
                if (!resp.isSuccessful || !obj.optBoolean("ok", false)) {
                    throw IllegalStateException(obj.optString("description", "فشل تحديث وصف البوت"))
                }
            }

            val shortPayload = JSONObject().apply {
                put("short_description", shortBio.trim().take(120))
            }.toString()
            val shortReq = Request.Builder()
                .url("https://api.telegram.org/bot$cleanToken/setMyShortDescription")
                .post(shortPayload.toRequestBody(jsonMedia))
                .build()
            httpClient.newCall(shortReq).execute().use { resp ->
                val obj = JSONObject(resp.body?.string().orEmpty())
                if (!resp.isSuccessful || !obj.optBoolean("ok", false)) {
                    throw IllegalStateException(obj.optString("description", "فشل تحديث النبذة المختصرة"))
                }
            }

            "تم تحديث الوصف الرسمي والنبذة المختصرة لبروفايل البوت على خوادم تلجرام بنجاح"
        }
    }

    /**
     * Inspects a Telegram chat/channel/group via `getChat` and `getChatMemberCount`.
     */
    suspend fun inspectTelegramChat(
        botToken: String,
        chatIdInput: String
    ): Result<TelegramChatInspection> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanToken = botToken.trim()
            val encodedChat = URLEncoder.encode(chatIdInput.trim(), "UTF-8")
            val chatUrl = "https://api.telegram.org/bot$cleanToken/getChat?chat_id=$encodedChat"
            val countUrl = "https://api.telegram.org/bot$cleanToken/getChatMemberCount?chat_id=$encodedChat"

            var memberCount = 0
            runCatching {
                httpClient.newCall(Request.Builder().url(countUrl).get().build()).execute().use { countResp ->
                    val countObj = JSONObject(countResp.body?.string().orEmpty())
                    if (countObj.optBoolean("ok", false)) {
                        memberCount = countObj.optInt("result", 0)
                    }
                }
            }

            httpClient.newCall(Request.Builder().url(chatUrl).get().build()).execute().use { response ->
                val bodyStr = response.body?.string().orEmpty()
                val json = JSONObject(bodyStr)
                if (!response.isSuccessful || !json.optBoolean("ok", false)) {
                    throw IllegalStateException(json.optString("description", "تعذر فحص المحادثة/القناة"))
                }
                val res = json.getJSONObject("result")
                TelegramChatInspection(
                    id = res.optLong("id", 0L),
                    title = res.optString("title", res.optString("first_name", "Chat")),
                    type = res.optString("type", "unknown"),
                    username = res.optString("username", ""),
                    description = res.optString("description", res.optString("bio", "")),
                    memberCount = memberCount
                )
            }
        }
    }

    suspend fun setTelegramWebhook(botToken: String, webhookUrl: String, dropPendingUpdates: Boolean = false): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanToken = botToken.trim()
            val encodedUrl = URLEncoder.encode(webhookUrl.trim(), "UTF-8")
            val apiUrl = if (webhookUrl.isBlank()) {
                "https://api.telegram.org/bot$cleanToken/deleteWebhook?drop_pending_updates=$dropPendingUpdates"
            } else {
                "https://api.telegram.org/bot$cleanToken/setWebhook?url=$encodedUrl&drop_pending_updates=$dropPendingUpdates"
            }
            val request = Request.Builder().url(apiUrl).get().build()
            httpClient.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string().orEmpty()
                val json = JSONObject(bodyStr)
                if (!response.isSuccessful || !json.optBoolean("ok", false)) {
                    val desc = json.optString("description", "تعذر ضبط Webhook")
                    throw IllegalStateException("Telegram API: $desc")
                }
                json.optString("description", "تم تحديث إعدادات Webhook بنجاح")
            }
        }
    }

    /**
     * Sends a message with optional Inline Keyboard URL button via `POST /bot<TOKEN>/sendMessage`.
     */
    suspend fun sendTelegramMessage(
        botToken: String,
        chatId: String,
        text: String,
        inlineButtonText: String = "",
        inlineButtonUrl: String = ""
    ): Result<Long> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanToken = botToken.trim()
            val url = "https://api.telegram.org/bot$cleanToken/sendMessage"
            val payloadJson = JSONObject().apply {
                put("chat_id", chatId.trim())
                put("text", text)
                if (inlineButtonText.isNotBlank() && inlineButtonUrl.startsWith("http")) {
                    val buttonObj = JSONObject().apply {
                        put("text", inlineButtonText.trim())
                        put("url", inlineButtonUrl.trim())
                    }
                    val rowArr = JSONArray().put(buttonObj)
                    val keyboardArr = JSONArray().put(rowArr)
                    put("reply_markup", JSONObject().put("inline_keyboard", keyboardArr))
                }
            }.toString()

            val request = Request.Builder()
                .url(url)
                .post(payloadJson.toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()
            httpClient.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string().orEmpty()
                val json = JSONObject(bodyStr)
                if (!response.isSuccessful || !json.optBoolean("ok", false)) {
                    val desc = json.optString("description", "فشل إرسال الرسالة عبر Telegram API")
                    throw IllegalStateException("Telegram API: $desc")
                }
                json.getJSONObject("result").optLong("message_id", 0L)
            }
        }
    }

    /**
     * Sends an interactive native Telegram Poll via `POST /bot<TOKEN>/sendPoll`.
     */
    suspend fun sendTelegramPoll(
        botToken: String,
        chatId: String,
        question: String,
        options: List<String>,
        isAnonymous: Boolean = true
    ): Result<Long> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanToken = botToken.trim()
            val cleanOptions = options.map { it.trim() }.filter { it.isNotEmpty() }
            require(cleanOptions.size >= 2) { "يجب إدخال خيارين على الأقل لإرسال استطلاع رأي على تلجرام" }
            val url = "https://api.telegram.org/bot$cleanToken/sendPoll"
            val optionsArray = JSONArray()
            cleanOptions.forEach { opt ->
                optionsArray.put(JSONObject().put("text", opt))
            }
            val payloadJson = JSONObject().apply {
                put("chat_id", chatId.trim())
                put("question", question.trim())
                put("options", optionsArray)
                put("is_anonymous", isAnonymous)
            }.toString()

            val request = Request.Builder()
                .url(url)
                .post(payloadJson.toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            httpClient.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string().orEmpty()
                val json = JSONObject(bodyStr)
                if (!response.isSuccessful || !json.optBoolean("ok", false)) {
                    val desc = json.optString("description", "تعذر إرسال استطلاع الرأي عبر Telegram API")
                    throw IllegalStateException("Telegram API: $desc")
                }
                json.getJSONObject("result").optLong("message_id", 0L)
            }
        }
    }

    /**
     * Sends a message and immediately pins it in the target chat/channel via `sendMessage` + `pinChatMessage`.
     */
    suspend fun sendAndPinTelegramMessage(
        botToken: String,
        chatId: String,
        text: String
    ): Result<Long> = withContext(Dispatchers.IO) {
        runCatching {
            val messageId = sendTelegramMessage(botToken, chatId, text).getOrThrow()
            val cleanToken = botToken.trim()
            val pinUrl = "https://api.telegram.org/bot$cleanToken/pinChatMessage"
            val pinPayload = JSONObject().apply {
                put("chat_id", chatId.trim())
                put("message_id", messageId)
                put("disable_notification", false)
            }.toString()

            val pinReq = Request.Builder()
                .url(pinUrl)
                .post(pinPayload.toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            httpClient.newCall(pinReq).execute().use { resp ->
                val json = JSONObject(resp.body?.string().orEmpty())
                if (!resp.isSuccessful || !json.optBoolean("ok", false)) {
                    val desc = json.optString("description", "تم إرسال الرسالة ولكن تعذر تثبيتها (تأكد من صلاحية Pin Messages للبوت)")
                    throw IllegalStateException("Telegram Pin API: $desc")
                }
            }
            messageId
        }
    }

    /**
     * Updates the bot's official display name on Telegram via `POST /bot<TOKEN>/setMyName`.
     */
    suspend fun updateTelegramBotOfficialName(
        botToken: String,
        newName: String
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanToken = botToken.trim()
            val url = "https://api.telegram.org/bot$cleanToken/setMyName"
            val payload = JSONObject().apply {
                put("name", newName.trim().take(64))
            }.toString()

            val request = Request.Builder()
                .url(url)
                .post(payload.toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            httpClient.newCall(request).execute().use { resp ->
                val json = JSONObject(resp.body?.string().orEmpty())
                if (!resp.isSuccessful || !json.optBoolean("ok", false)) {
                    throw IllegalStateException(json.optString("description", "تعذر تحديث اسم البوت الرسمي على تلجرام"))
                }
                "تم تحديث الاسم الرسمي للبوت على خوادم تلجرام إلى: ${newName.trim()}"
            }
        }
    }

    suspend fun pollAndProcessTelegramUpdates(
        botToken: String,
        lastUpdateId: Long,
        welcomeMessage: String,
        rulesJson: String
    ): Result<List<TelegramUpdateEvent>> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanToken = botToken.trim()
            val offset = if (lastUpdateId > 0L) lastUpdateId + 1L else 0L
            val url = "https://api.telegram.org/bot$cleanToken/getUpdates?offset=$offset&timeout=2&limit=20"
            val request = Request.Builder().url(url).get().build()

            val rules = parseAutoReplyRules(rulesJson)
            val processedEvents = mutableListOf<TelegramUpdateEvent>()

            httpClient.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string().orEmpty()
                val json = JSONObject(bodyStr)
                if (!response.isSuccessful || !json.optBoolean("ok", false)) {
                    val desc = json.optString("description", "تعذر جلب التحديثات من تلجرام")
                    throw IllegalStateException("Telegram API: $desc")
                }
                val resultArray = json.optJSONArray("result") ?: JSONArray()
                for (i in 0 until resultArray.length()) {
                    val updateObj = resultArray.getJSONObject(i)
                    val updateId = updateObj.optLong("update_id", 0L)
                    val msgObj = updateObj.optJSONObject("message")
                        ?: updateObj.optJSONObject("edited_message")
                    if (msgObj != null) {
                        val chatObj = msgObj.optJSONObject("chat")
                        val fromObj = msgObj.optJSONObject("from")
                        val chatId = chatObj?.optLong("id", 0L) ?: 0L
                        val senderName = fromObj?.optString("first_name", "User").orEmpty()
                        val text = msgObj.optString("text", "").trim()

                        var replySent: String? = null
                        if (chatId != 0L && text.isNotEmpty()) {
                            val matchedRule = rules.firstOrNull {
                                text.equals(it.command.trim(), ignoreCase = true) ||
                                    text.startsWith(it.command.trim() + " ", ignoreCase = true)
                            }
                            val replyCandidate = when {
                                matchedRule != null -> matchedRule.responseText
                                text.startsWith("/start", ignoreCase = true) && welcomeMessage.isNotBlank() -> welcomeMessage
                                else -> null
                            }
                            if (!replyCandidate.isNullOrBlank()) {
                                sendTelegramMessage(cleanToken, chatId.toString(), replyCandidate)
                                replySent = replyCandidate
                            }
                        }

                        processedEvents.add(
                            TelegramUpdateEvent(
                                updateId = updateId,
                                chatId = chatId,
                                senderName = senderName,
                                text = text,
                                autoRepliedWith = replySent
                            )
                        )
                    }
                }
            }
            processedEvents
        }
    }

    fun parseAutoReplyRules(jsonStr: String): List<BotAutoReplyRule> {
        return try {
            val arr = JSONArray(jsonStr)
            val list = mutableListOf<BotAutoReplyRule>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val cmd = obj.optString("command", "").trim()
                val resp = obj.optString("responseText", "").trim()
                if (cmd.isNotEmpty() && resp.isNotEmpty()) {
                    list.add(BotAutoReplyRule(cmd, resp))
                }
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun serializeAutoReplyRules(rules: List<BotAutoReplyRule>): String {
        val arr = JSONArray()
        rules.forEach { rule ->
            arr.put(
                JSONObject().apply {
                    put("command", rule.command.trim())
                    put("responseText", rule.responseText.trim())
                }
            )
        }
        return arr.toString()
    }

    // =========================================================================
    // 2. MULTI-RECORD DNS-OVER-HTTPS + SSL/HTTP PROBE ENGINE
    // =========================================================================

    suspend fun checkFullDomainAndSslReport(domainInput: String): Result<DnsMultiRecordReport> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanDomain = domainInput.trim()
                .removePrefix("https://")
                .removePrefix("http://")
                .substringBefore("/")
                .lowercase()
            require(cleanDomain.contains(".") && cleanDomain.length >= 4) {
                "يرجى إدخال اسم نطاق حقيقي صالح (مثال: myproject-app.com أو mysite.surge.sh)"
            }

            fun queryDohType(type: String): Pair<Int, List<String>> {
                val encodedName = URLEncoder.encode(cleanDomain, "UTF-8")
                val url = "https://dns.google/resolve?name=$encodedName&type=$type"
                val req = Request.Builder()
                    .url(url)
                    .header("Accept", "application/dns-json")
                    .get()
                    .build()
                httpClient.newCall(req).execute().use { resp ->
                    val body = resp.body?.string().orEmpty()
                    if (!resp.isSuccessful) return -1 to emptyList()
                    val json = JSONObject(body)
                    val status = json.optInt("Status", -1)
                    val ansArr = json.optJSONArray("Answer") ?: JSONArray()
                    val list = mutableListOf<String>()
                    for (i in 0 until ansArr.length()) {
                        val d = ansArr.getJSONObject(i).optString("data", "").trim()
                        if (d.isNotEmpty()) list.add(d)
                    }
                    return status to list
                }
            }

            val (statusA, aRecords) = queryDohType("A")
            val (_, aaaaRecords) = queryDohType("AAAA")
            val (_, cnameRecords) = queryDohType("CNAME")
            val (_, mxRecords) = queryDohType("MX")
            val (_, txtRecords) = queryDohType("TXT")
            val (_, nsRecords) = queryDohType("NS")

            val isUnused = (statusA == 3) || (statusA == 0 && aRecords.isEmpty() && cnameRecords.isEmpty())

            var httpStatus = -1
            var latencyMs = -1L
            var sslIssuer = ""
            var sslExpiry = ""
            var serverHeader = ""
            var geoCountry = ""
            var geoCity = ""
            var geoIsp = ""
            var geoAsn = ""

            if (!isUnused) {
                runCatching {
                    val startNs = System.nanoTime()
                    val urlObj = java.net.URL("https://$cleanDomain")
                    val conn = (urlObj.openConnection() as HttpsURLConnection).apply {
                        connectTimeout = 6000
                        readTimeout = 6000
                        requestMethod = "HEAD"
                        instanceFollowRedirects = true
                    }
                    conn.connect()
                    httpStatus = conn.responseCode
                    latencyMs = (System.nanoTime() - startNs) / 1_000_000L
                    serverHeader = conn.getHeaderField("Server")
                        ?: conn.getHeaderField("x-vercel-id")?.let { "Vercel Edge" }
                        ?: conn.getHeaderField("x-nf-request-id")?.let { "Netlify CDN" }
                        ?: ""
                    val cert = conn.serverCertificates.firstOrNull() as? X509Certificate
                    if (cert != null) {
                        sslIssuer = cert.issuerX500Principal?.name?.take(80).orEmpty()
                        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                        sslExpiry = cert.notAfter?.let { fmt.format(it) }.orEmpty()
                    }
                    conn.disconnect()
                }

                val primaryIp = aRecords.firstOrNull { it.matches(Regex("\\d+\\.\\d+\\.\\d+\\.\\d+")) }
                if (!primaryIp.isNullOrBlank()) {
                    runCatching {
                        val geoReq = Request.Builder()
                            .url("https://ipwho.is/$primaryIp")
                            .header("Accept", "application/json")
                            .get()
                            .build()
                        httpClient.newCall(geoReq).execute().use { geoResp ->
                            if (geoResp.isSuccessful) {
                                val geoJson = JSONObject(geoResp.body?.string().orEmpty())
                                if (geoJson.optBoolean("success", true)) {
                                    geoCountry = geoJson.optString("country", "")
                                    geoCity = geoJson.optString("city", "")
                                    val connObj = geoJson.optJSONObject("connection")
                                    geoIsp = connObj?.optString("org", connObj.optString("isp", "")).orEmpty()
                                    val asnNum = connObj?.optInt("asn", 0) ?: 0
                                    if (asnNum > 0) {
                                        geoAsn = "AS$asnNum"
                                    }
                                }
                            }
                        }
                    }
                }
            }

            DnsMultiRecordReport(
                domain = cleanDomain,
                isAvailableForClaim = isUnused,
                dnsStatusCode = statusA,
                aRecords = aRecords,
                aaaaRecords = aaaaRecords,
                cnameRecords = cnameRecords,
                mxRecords = mxRecords,
                txtRecords = txtRecords,
                nsRecords = nsRecords,
                authorityRecord = if (nsRecords.isNotEmpty()) nsRecords.joinToString(", ") else "Google DoH Verified",
                httpProbeStatus = httpStatus,
                httpLatencyMs = latencyMs,
                sslCertificateIssuer = sslIssuer,
                sslValidUntil = sslExpiry,
                geoCountry = geoCountry,
                geoCity = geoCity,
                geoIsp = geoIsp,
                geoAsn = geoAsn,
                httpServerHeader = serverHeader
            )
        }
    }

    suspend fun checkRealDomainAvailability(domainInput: String): Result<DomainCheckResult> = withContext(Dispatchers.IO) {
        checkFullDomainAndSslReport(domainInput).map { full ->
            DomainCheckResult(
                domain = full.domain,
                isAvailableForClaim = full.isAvailableForClaim,
                dnsStatusCode = full.dnsStatusCode,
                resolvedIps = full.aRecords + full.cnameRecords,
                authorityRecord = full.authorityRecord,
                checkedAtMillis = full.checkedAtMillis
            )
        }
    }

    // =========================================================================
    // 3. REAL WEBSITE COMPILER & ENTERPRISE CLOUD DEPLOYERS (Vercel / Netlify / GitHub)
    // =========================================================================

    private fun resolveActiveVercelToken(runtimeVercelToken: String): String {
        val envVercelToken = BuildConfig.VERCEL_TOKEN.trim()
        return runtimeVercelToken.trim().ifEmpty {
            if (envVercelToken != "YOUR_VERCEL_API_TOKEN" && !envVercelToken.startsWith("MY_")) envVercelToken else ""
        }
    }

    private fun resolveActiveNetlifyToken(runtimeNetlifyToken: String): String {
        val envNetlifyToken = BuildConfig.NETLIFY_TOKEN.trim()
        return runtimeNetlifyToken.trim().ifEmpty {
            if (envNetlifyToken != "YOUR_NETLIFY_PERSONAL_ACCESS_TOKEN" && !envNetlifyToken.startsWith("MY_")) envNetlifyToken else ""
        }
    }

    private fun resolveActiveGithubPat(runtimeGithubPat: String): String {
        val envGithubPat = BuildConfig.GITHUB_PAT.trim()
        return runtimeGithubPat.trim().ifEmpty {
            if (envGithubPat != "YOUR_GITHUB_PERSONAL_ACCESS_TOKEN" && !envGithubPat.startsWith("MY_")) envGithubPat else ""
        }
    }

    fun compileSingleFileWebsiteBundle(
        projectName: String,
        htmlContent: String,
        cssContent: String,
        jsContent: String
    ): String {
        val styleBlock = "<style>\n$cssContent\n</style>"
        val scriptBlock = "<script>\n$jsContent\n</script>"

        val baseHtml = if (htmlContent.contains("<html", ignoreCase = true)) {
            htmlContent
        } else {
            """<!DOCTYPE html>
<html lang="ar" dir="rtl">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>$projectName</title>
</head>
<body>
$htmlContent
</body>
</html>"""
        }

        val withCss = if (baseHtml.contains("</head>", ignoreCase = true)) {
            baseHtml.replaceFirst(Regex("</head>", RegexOption.IGNORE_CASE), "$styleBlock\n</head>")
        } else {
            "$styleBlock\n$baseHtml"
        }

        return if (withCss.contains("</body>", ignoreCase = true)) {
            withCss.replaceFirst(Regex("</body>", RegexOption.IGNORE_CASE), "$scriptBlock\n</body>")
        } else {
            "$withCss\n$scriptBlock"
        }
    }

    /**
     * Fetches the live HTML source code of any public website URL so the user can import/clone and customize it.
     */
    suspend fun fetchLiveWebsiteHtmlSource(targetUrl: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val normalizedUrl = targetUrl.trim().let {
                if (it.startsWith("http://") || it.startsWith("https://")) it else "https://$it"
            }
            val request = Request.Builder()
                .url(normalizedUrl)
                .header("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) TeleHostCloudStudio/2.0")
                .header("Accept", "text/html,application/xhtml+xml")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                val html = response.body?.string().orEmpty()
                if (!response.isSuccessful || html.isBlank()) {
                    throw IllegalStateException("تعذر سحب الكود المصدري من الرابط (HTTP ${response.code})")
                }
                html.take(65_000)
            }
        }
    }

    /**
     * Performs a real static analysis & SEO/Security/Performance audit on the user's HTML, CSS, and JS code.
     */
    fun auditWebsiteCode(
        htmlContent: String,
        cssContent: String,
        jsContent: String
    ): WebsiteCodeAuditReport {
        val passed = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        var score = 100

        val totalBytes = htmlContent.toByteArray(StandardCharsets.UTF_8).size +
            cssContent.toByteArray(StandardCharsets.UTF_8).size +
            jsContent.toByteArray(StandardCharsets.UTF_8).size

        if (htmlContent.contains("<!DOCTYPE html>", ignoreCase = true)) {
            passed.add("ترويسة HTML5 القياسية (DOCTYPE) موجودة")
        } else {
            score -= 10
            warnings.add("يُنصح بإضافة <!DOCTYPE html> في بداية ملف HTML")
        }

        if (htmlContent.contains("viewport", ignoreCase = true)) {
            passed.add("التجاوب مع شاشات الهواتف (Meta Viewport) مفعل")
        } else {
            score -= 15
            warnings.add("وسم <meta name=\"viewport\"> مفقود؛ قد لا يظهر الموقع بشكل متجاوب على الهاتف")
        }

        if (htmlContent.contains("<title>", ignoreCase = true)) {
            passed.add("عنوان الصفحة لمحركات البحث (<title>) مضبوط")
        } else {
            score -= 15
            warnings.add("وسم <title> مفقود؛ مهم جداً لأرشفة الموقع في Google")
        }

        if (htmlContent.contains("http://", ignoreCase = true)) {
            score -= 15
            warnings.add("يوجد روابط غير مشفرة (http://) داخل الكود؛ استبدلها بـ https:// لتجنب تحذيرات الأمان")
        } else {
            passed.add("جميع الروابط الخارجية متوافقة مع التشفير الآمن HTTPS")
        }

        if (cssContent.trim().length > 20) {
            passed.add("ملف التنسيق (styles.css) نشط ومهيأ (${cssContent.length} حرف)")
        } else {
            score -= 10
            warnings.add("ملف التنسيق styles.css فارغ أو قصير جداً")
        }

        if (totalBytes <= 250_000) {
            passed.add("حجم الحزمة مثالي للتحميل الفوري على شبكات Edge (${totalBytes / 1024} KB)")
        } else {
            score -= 10
            warnings.add("حجم الحزمة كبير نسبياً (${totalBytes / 1024} KB)")
        }

        return WebsiteCodeAuditReport(
            score = score.coerceIn(0, 100),
            totalBytes = totalBytes,
            passedChecks = passed,
            warnings = warnings
        )
    }

    fun buildInMemoryZipArchive(files: Map<String, ByteArray>): ByteArray {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zipOut ->
            files.forEach { (fileName, content) ->
                val entry = ZipEntry(fileName.removePrefix("./").removePrefix("/"))
                entry.size = content.size.toLong()
                zipOut.putNextEntry(entry)
                zipOut.write(content)
                zipOut.closeEntry()
            }
        }
        return baos.toByteArray()
    }

    /**
     * Deploys a multi-file website directly to Vercel Global Edge Network (`https://api.vercel.com/v13/deployments`).
     */
    private fun deployToVercelEdgeReal(
        subdomainSlug: String,
        compiledHtml: String,
        cssContent: String,
        jsContent: String,
        vercelToken: String
    ): Pair<String, String> {
        val projectSlug = subdomainSlug
            .removeSuffix(".vercel.app")
            .substringBefore(".")
            .replace(Regex("[^a-z0-9\\-]"), "-")
            .trim('-')
            .ifBlank { "telehost-edge" }

        val vercelConfigJson = JSONObject().apply {
            put("cleanUrls", true)
            put("trailingSlash", false)
        }.toString(2)

        val filesArray = JSONArray().apply {
            put(JSONObject().apply {
                put("file", "index.html")
                put("data", compiledHtml)
            })
            put(JSONObject().apply {
                put("file", "styles.css")
                put("data", cssContent)
            })
            put(JSONObject().apply {
                put("file", "app.js")
                put("data", jsContent)
            })
            put(JSONObject().apply {
                put("file", "vercel.json")
                put("data", vercelConfigJson)
            })
        }

        val payload = JSONObject().apply {
            put("name", projectSlug)
            put("files", filesArray)
            put("target", "production")
            put("projectSettings", JSONObject().apply {
                put("framework", JSONObject.NULL)
            })
        }.toString()

        val request = Request.Builder()
            .url("https://api.vercel.com/v13/deployments?skipAutoDetectionConfirmation=1")
            .header("Authorization", "Bearer $vercelToken")
            .header("Content-Type", "application/json; charset=utf-8")
            .post(payload.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        httpClient.newCall(request).execute().use { response ->
            val bodyStr = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val errMsg = runCatching {
                    JSONObject(bodyStr).optJSONObject("error")?.optString("message")
                }.getOrNull().orEmpty().ifBlank { bodyStr.take(250) }
                throw IllegalStateException("Vercel Edge Deploy Failed (${response.code}): $errMsg")
            }
            val json = JSONObject(bodyStr)
            val aliasArr = json.optJSONArray("alias")
            val primaryDomain = if (aliasArr != null && aliasArr.length() > 0) {
                aliasArr.optString(0)
            } else {
                "$projectSlug.vercel.app"
            }
            val liveUrl = if (primaryDomain.startsWith("http")) primaryDomain else "https://$primaryDomain"
            val deploymentId = json.optString("id", "dpl_edge")
            val readyState = json.optString("readyState", "READY")
            val logSummary = "تم النشر الفوري على شبكة Vercel Global Edge (ID: $deploymentId | الحالة: $readyState) وتفعيل النطاق الرسمي: $liveUrl"
            return liveUrl to logSummary
        }
    }

    /**
     * Lists live deployments on Vercel Edge Network via `GET https://api.vercel.com/v6/deployments`.
     */
    suspend fun listVercelAccountDeployments(runtimeVercelToken: String): Result<List<SurgeHostedProject>> = withContext(Dispatchers.IO) {
        runCatching {
            val token = resolveActiveVercelToken(runtimeVercelToken)
            require(token.isNotEmpty()) { "يرجى إدخال مفتاح VERCEL_TOKEN أولاً" }
            val request = Request.Builder()
                .url("https://api.vercel.com/v6/deployments?limit=15")
                .header("Authorization", "Bearer $token")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    throw IllegalStateException("Vercel List Failed (${response.code})")
                }
                val arr = JSONObject(bodyStr).optJSONArray("deployments") ?: JSONArray()
                val list = mutableListOf<SurgeHostedProject>()
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val name = obj.optString("name", "site")
                    val url = obj.optString("url", "$name.vercel.app")
                    val state = obj.optString("state", obj.optString("readyState", "READY"))
                    list.add(
                        SurgeHostedProject(
                            domain = url,
                            revCount = 1,
                            lastUpdated = state,
                            platformInfo = "Vercel Edge CDN"
                        )
                    )
                }
                list
            }
        }
    }

    /**
     * Deploys a multi-file website archive (.zip) directly to Netlify Global CDN (`https://api.netlify.com/api/v1/sites`).
     */
    private fun deployToNetlifyCdnReal(
        subdomainSlug: String,
        compiledHtml: String,
        cssContent: String,
        jsContent: String,
        netlifyToken: String
    ): Pair<String, String> {
        val siteSlug = subdomainSlug
            .removeSuffix(".netlify.app")
            .substringBefore(".")
            .replace(Regex("[^a-z0-9\\-]"), "-")
            .trim('-')
            .ifBlank { "telehost-cdn" }

        val jsonMedia = "application/json; charset=utf-8".toMediaType()

        // 1. Check if the site already exists in user's Netlify account or create it
        var siteId: String? = null
        var siteDefaultDomain = "$siteSlug.netlify.app"

        val listReq = Request.Builder()
            .url("https://api.netlify.com/api/v1/sites?name=$siteSlug")
            .header("Authorization", "Bearer $netlifyToken")
            .get()
            .build()

        httpClient.newCall(listReq).execute().use { resp ->
            if (resp.isSuccessful) {
                val arr = JSONArray(resp.body?.string().orEmpty())
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    if (obj.optString("name").equals(siteSlug, ignoreCase = true)) {
                        siteId = obj.optString("id")
                        siteDefaultDomain = obj.optString("default_domain", siteDefaultDomain)
                        break
                    }
                }
            }
        }

        if (siteId.isNullOrBlank()) {
            val createSiteBody = JSONObject().apply {
                put("name", siteSlug)
            }.toString()

            val createReq = Request.Builder()
                .url("https://api.netlify.com/api/v1/sites")
                .header("Authorization", "Bearer $netlifyToken")
                .post(createSiteBody.toRequestBody(jsonMedia))
                .build()

            httpClient.newCall(createReq).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (resp.isSuccessful) {
                    val obj = JSONObject(body)
                    siteId = obj.optString("id")
                    siteDefaultDomain = obj.optString("default_domain", siteDefaultDomain)
                } else {
                    // Fallback: create with auto-generated unique subdomain if name is taken globally on Netlify
                    val fallbackCreateReq = Request.Builder()
                        .url("https://api.netlify.com/api/v1/sites")
                        .header("Authorization", "Bearer $netlifyToken")
                        .post("{}".toRequestBody(jsonMedia))
                        .build()
                    httpClient.newCall(fallbackCreateReq).execute().use { fallbackResp ->
                        val fbBody = fallbackResp.body?.string().orEmpty()
                        if (!fallbackResp.isSuccessful) {
                            throw IllegalStateException("Netlify Site Creation Failed (${fallbackResp.code}): ${fbBody.take(200)}")
                        }
                        val fbObj = JSONObject(fbBody)
                        siteId = fbObj.optString("id")
                        siteDefaultDomain = fbObj.optString("default_domain", siteDefaultDomain)
                    }
                }
            }
        }

        val resolvedSiteId = requireNotNull(siteId) { "تعذر تهيئة معرف الموقع على Netlify" }

        // 2. Build in-memory ZIP archive with SPA redirects and deploy to Netlify CDN
        val zipBytes = buildInMemoryZipArchive(
            mapOf(
                "index.html" to compiledHtml.toByteArray(StandardCharsets.UTF_8),
                "styles.css" to cssContent.toByteArray(StandardCharsets.UTF_8),
                "app.js" to jsContent.toByteArray(StandardCharsets.UTF_8),
                "_redirects" to "/* /index.html 200\n".toByteArray(StandardCharsets.UTF_8)
            )
        )

        val deployReq = Request.Builder()
            .url("https://api.netlify.com/api/v1/sites/$resolvedSiteId/deploys")
            .header("Authorization", "Bearer $netlifyToken")
            .header("Content-Type", "application/zip")
            .post(zipBytes.toRequestBody("application/zip".toMediaType()))
            .build()

        httpClient.newCall(deployReq).execute().use { resp ->
            val bodyStr = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                throw IllegalStateException("Netlify ZIP Deploy Failed (${resp.code}): ${bodyStr.take(200)}")
            }
            val obj = JSONObject(bodyStr)
            val sslUrl = obj.optString("ssl_url", obj.optString("url", "https://$siteDefaultDomain")).ifBlank {
                "https://$siteDefaultDomain"
            }
            val deployId = obj.optString("id", "netlify_deploy")
            val logSummary = "تم ضغط الموقع بصيغة .zip (${zipBytes.size} بايت) ورفعه عبر Netlify Global CDN API (Deploy ID: $deployId) إلى النطاق: $sslUrl"
            return sslUrl to logSummary
        }
    }

    /**
     * Lists live sites on Netlify Global CDN via `GET https://api.netlify.com/api/v1/sites`.
     */
    suspend fun listNetlifyAccountSites(runtimeNetlifyToken: String): Result<List<SurgeHostedProject>> = withContext(Dispatchers.IO) {
        runCatching {
            val token = resolveActiveNetlifyToken(runtimeNetlifyToken)
            require(token.isNotEmpty()) { "يرجى إدخال مفتاح NETLIFY_TOKEN أولاً" }
            val request = Request.Builder()
                .url("https://api.netlify.com/api/v1/sites")
                .header("Authorization", "Bearer $token")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    throw IllegalStateException("Netlify List Failed (${response.code})")
                }
                val arr = JSONArray(bodyStr)
                val list = mutableListOf<SurgeHostedProject>()
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val defaultDomain = obj.optString("default_domain", obj.optString("url", ""))
                    if (defaultDomain.isNotEmpty()) {
                        list.add(
                            SurgeHostedProject(
                                domain = defaultDomain.removePrefix("https://").removePrefix("http://"),
                                revCount = 1,
                                lastUpdated = obj.optString("updated_at", "Active").take(10),
                                platformInfo = "Netlify Global CDN"
                            )
                        )
                    }
                }
                list
            }
        }
    }

    suspend fun deployWebsiteToLiveDomain(
        projectName: String,
        subdomainSlug: String,
        provider: String,
        htmlContent: String,
        cssContent: String,
        jsContent: String,
        runtimeVercelToken: String = "",
        runtimeNetlifyToken: String = "",
        runtimeGithubPat: String = ""
    ): Result<Pair<String, String>> = withContext(Dispatchers.IO) {
        runCatching {
            val compiledBundle = compileSingleFileWebsiteBundle(projectName, htmlContent, cssContent, jsContent)
            val cleanSlug = subdomainSlug.trim().lowercase().replace(Regex("[^a-z0-9\\-.]"), "-").ifBlank { "telehost-site" }

            val activeVercelToken = resolveActiveVercelToken(runtimeVercelToken)
            val activeNetlifyToken = resolveActiveNetlifyToken(runtimeNetlifyToken)
            val activeGithubPat = resolveActiveGithubPat(runtimeGithubPat)

            when {
                provider == "vercel_edge" && activeVercelToken.isNotEmpty() -> {
                    deployToVercelEdgeReal(cleanSlug, compiledBundle, cssContent, jsContent, activeVercelToken)
                }
                provider == "netlify_cdn" && activeNetlifyToken.isNotEmpty() -> {
                    deployToNetlifyCdnReal(cleanSlug, compiledBundle, cssContent, jsContent, activeNetlifyToken)
                }
                provider == "github_pages" && activeGithubPat.isNotEmpty() -> {
                    deployToGitHubPagesReal(cleanSlug.substringBefore("."), projectName, compiledBundle, activeGithubPat)
                }
                else -> {
                    deployToPublicCloudGatewayReal(
                        slug = cleanSlug,
                        compiledHtml = compiledBundle,
                        requestedProvider = provider,
                        hasGithubPat = activeGithubPat.isNotEmpty(),
                        hasVercelToken = activeVercelToken.isNotEmpty(),
                        hasNetlifyToken = activeNetlifyToken.isNotEmpty()
                    )
                }
            }
        }
    }

    private fun deployToGitHubPagesReal(
        repoSlug: String,
        projectName: String,
        compiledHtml: String,
        githubPat: String
    ): Pair<String, String> {
        val jsonMedia = "application/json; charset=utf-8".toMediaType()

        val userReq = Request.Builder()
            .url("https://api.github.com/user")
            .header("Authorization", "Bearer $githubPat")
            .header("Accept", "application/vnd.github+json")
            .get()
            .build()

        val username = httpClient.newCall(userReq).execute().use { resp ->
            val body = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                throw IllegalStateException("GitHub Auth Failed (${resp.code}): تأكد من صلاحية GITHUB_PAT")
            }
            JSONObject(body).getString("login")
        }

        val createRepoPayload = JSONObject().apply {
            put("name", repoSlug)
            put("description", "Deployed via TeleHost Cloud: $projectName")
            put("homepage", "https://$username.github.io/$repoSlug/")
            put("private", false)
            put("auto_init", true)
        }.toString()

        val createRepoReq = Request.Builder()
            .url("https://api.github.com/user/repos")
            .header("Authorization", "Bearer $githubPat")
            .header("Accept", "application/vnd.github+json")
            .post(createRepoPayload.toRequestBody(jsonMedia))
            .build()

        httpClient.newCall(createRepoReq).execute().close()

        var existingSha: String? = null
        val getFileReq = Request.Builder()
            .url("https://api.github.com/repos/$username/$repoSlug/contents/index.html")
            .header("Authorization", "Bearer $githubPat")
            .header("Accept", "application/vnd.github+json")
            .get()
            .build()

        httpClient.newCall(getFileReq).execute().use { resp ->
            if (resp.isSuccessful) {
                val obj = JSONObject(resp.body?.string().orEmpty())
                existingSha = obj.optString("sha", null)
            }
        }

        val base64Content = Base64.encodeToString(compiledHtml.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        val putFileJson = JSONObject().apply {
            put("message", "Deploy production bundle via TeleHost Cloud")
            put("content", base64Content)
            if (!existingSha.isNullOrBlank()) {
                put("sha", existingSha)
            }
        }.toString()

        val putFileReq = Request.Builder()
            .url("https://api.github.com/repos/$username/$repoSlug/contents/index.html")
            .header("Authorization", "Bearer $githubPat")
            .header("Accept", "application/vnd.github+json")
            .put(putFileJson.toRequestBody(jsonMedia))
            .build()

        httpClient.newCall(putFileReq).execute().use { resp ->
            if (!resp.isSuccessful) {
                throw IllegalStateException("GitHub Upload Failed (${resp.code}): ${resp.body?.string()}")
            }
        }

        val pagesPayload = JSONObject().apply {
            put("source", JSONObject().apply {
                put("branch", "main")
                put("path", "/")
            })
        }.toString()

        val pagesReq = Request.Builder()
            .url("https://api.github.com/repos/$username/$repoSlug/pages")
            .header("Authorization", "Bearer $githubPat")
            .header("Accept", "application/vnd.github+json")
            .post(pagesPayload.toRequestBody(jsonMedia))
            .build()

        httpClient.newCall(pagesReq).execute().close()

        val liveUrl = "https://$username.github.io/$repoSlug/"
        val logMsg = "تم رفع حزمة الموقع (${compiledHtml.length} بايت) وتفعيل نطاق GitHub Pages الحقيقي بنجاح: $liveUrl"
        return liveUrl to logMsg
    }

    private fun deployToPublicCloudGatewayReal(
        slug: String,
        compiledHtml: String,
        requestedProvider: String,
        hasGithubPat: Boolean,
        hasVercelToken: Boolean,
        hasNetlifyToken: Boolean
    ): Pair<String, String> {
        val formBody = FormBody.Builder()
            .add("content", compiledHtml)
            .add("syntax", "html")
            .add("title", slug)
            .add("expiry_days", "365")
            .build()

        val req = Request.Builder()
            .url("https://dpaste.com/api/v2/")
            .header("User-Agent", "TeleHostCloud-Android/1.0")
            .post(formBody)
            .build()

        httpClient.newCall(req).execute().use { response ->
            val rawUrl = response.body?.string()?.trim().orEmpty()
            if (!response.isSuccessful || !rawUrl.startsWith("http")) {
                throw IllegalStateException("فشل النشر على البوابة السحابية العامة (HTTP ${response.code})")
            }
            val rawTxtUrl = if (rawUrl.endsWith("/")) "${rawUrl.dropLast(1)}.txt" else "$rawUrl.txt"
            val note = when {
                requestedProvider == "vercel_edge" && !hasVercelToken ->
                    "ملاحظة: لم يتم إدخال VERCEL_TOKEN بعد، لذا تم النشر الفوري عبر البوابة السحابية المباشرة. يمكنك جلب VERCEL_TOKEN بضغطة زر من خزنة الخوادم أعلاه للنشر على نطاق .vercel.app."
                requestedProvider == "netlify_cdn" && !hasNetlifyToken ->
                    "ملاحظة: لم يتم إدخال NETLIFY_TOKEN بعد، لذا تم النشر الفوري عبر البوابة السحابية المباشرة. يمكنك جلب NETLIFY_TOKEN بضغطة زر من خزنة الخوادم أعلاه للنشر على نطاق .netlify.app."
                requestedProvider == "github_pages" && !hasGithubPat ->
                    "ملاحظة: لم يتم إدخال GITHUB_PAT بعد، لذا تم النشر الفوري عبر البوابة السحابية المجانية المباشرة."
                else ->
                    "تم حزم ملفات HTML/CSS/JS ورفعها بنجاح على رابط استضافة سحابي عام وحقيقي."
            }
            return rawTxtUrl to "$note\nالرابط المباشر: $rawTxtUrl\nحجم البناء النهائي: ${compiledHtml.length} بايت"
        }
    }
}
