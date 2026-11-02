package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.BotAutoReplyRule
import com.example.data.model.DeploymentLog
import com.example.data.model.DnsMultiRecordReport
import com.example.data.model.SurgeHostedProject
import com.example.data.model.TelegramBot
import com.example.data.model.TelegramChatInspection
import com.example.data.model.TelegramWebhookInfo
import com.example.data.model.WebSiteProject
import com.example.data.model.WebsiteCodeAuditReport
import com.example.data.network.RealNetworkEngines
import com.example.data.repository.TeleHostRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

class TeleHostViewModel(
    private val repository: TeleHostRepository,
    private val currentUserId: String,
    private val networkEngines: RealNetworkEngines = RealNetworkEngines()
) : ViewModel() {

    val botsState: StateFlow<UiState<List<TelegramBot>>> = repository.observeBots(currentUserId)
        .map<List<TelegramBot>, UiState<List<TelegramBot>>> { list ->
            syncActiveBotEngines(list)
            UiState.Success(list)
        }
        .catch { emit(UiState.Error(it.message ?: "تعذر تحميل قائمة البوتات من الخادم")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    val sitesState: StateFlow<UiState<List<WebSiteProject>>> = repository.observeSites(currentUserId)
        .map<List<WebSiteProject>, UiState<List<WebSiteProject>>> { UiState.Success(it) }
        .catch { emit(UiState.Error(it.message ?: "تعذر تحميل مشاريع المواقع")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    val logsState: StateFlow<UiState<List<DeploymentLog>>> = repository.observeLogs(currentUserId)
        .map<List<DeploymentLog>, UiState<List<DeploymentLog>>> { UiState.Success(it) }
        .catch { emit(UiState.Error(it.message ?: "تعذر تحميل السجلات السحابية")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _dnsReports = MutableStateFlow<List<DnsMultiRecordReport>>(emptyList())
    val dnsReports: StateFlow<List<DnsMultiRecordReport>> = _dnsReports.asStateFlow()

    private val _runtimeVercelToken = MutableStateFlow("")
    val runtimeVercelToken: StateFlow<String> = _runtimeVercelToken.asStateFlow()

    private val _runtimeNetlifyToken = MutableStateFlow("")
    val runtimeNetlifyToken: StateFlow<String> = _runtimeNetlifyToken.asStateFlow()

    private val _runtimeGithubPat = MutableStateFlow("")
    val runtimeGithubPat: StateFlow<String> = _runtimeGithubPat.asStateFlow()

    private val _surgeLiveDomains = MutableStateFlow<List<SurgeHostedProject>>(emptyList())
    val surgeLiveDomains: StateFlow<List<SurgeHostedProject>> = _surgeLiveDomains.asStateFlow()

    private val _webhookDiagnostic = MutableStateFlow<TelegramWebhookInfo?>(null)
    val webhookDiagnostic: StateFlow<TelegramWebhookInfo?> = _webhookDiagnostic.asStateFlow()

    private val _chatInspection = MutableStateFlow<TelegramChatInspection?>(null)
    val chatInspection: StateFlow<TelegramChatInspection?> = _chatInspection.asStateFlow()

    private val activePollingJobs = mutableMapOf<String, Job>()

    fun clearBanner() {
        _statusBanner.value = null
    }

    // =========================================================================
    // 1. REAL TELEGRAM BOT HOSTING & ADVANCED API OPERATIONS
    // =========================================================================

    fun verifyAndAddTelegramBot(
        customName: String,
        botToken: String,
        welcomeMessage: String
    ) {
        if (botToken.isBlank()) {
            _statusBanner.value = "يرجى إدخال توكن البوت المستخرج من BotFather"
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            _statusBanner.value = "جاري الاتصال بخادم api.telegram.org للتحقق من التوكن..."
            val verifyResult = networkEngines.verifyTelegramBotToken(botToken)
            verifyResult.onSuccess { profile ->
                val resolvedName = customName.trim().ifBlank { profile.firstName }
                val defaultRules = networkEngines.serializeAutoReplyRules(
                    listOf(
                        BotAutoReplyRule("/help", "قائمة الأوامر النشطة عبر خادم TeleHost Cloud."),
                        BotAutoReplyRule("/status", "البوت يعمل بأقصى كفاءة على الخادم السحابي الدائم.")
                    )
                )
                val createRes = repository.createBot(
                    name = resolvedName,
                    username = profile.username,
                    botToken = botToken.trim(),
                    welcomeMessage = welcomeMessage.trim().ifBlank { "مرحبا بك في بوت $resolvedName" },
                    autoReplyRulesJson = defaultRules
                )
                createRes.onSuccess {
                    repository.appendLog(
                        targetName = "@${profile.username}",
                        category = "bot_engine",
                        message = "تم التحقق من البوت عبر Telegram API (ID: ${profile.id}) وإضافته للخادم السحابي."
                    )
                    _statusBanner.value = "تم ربط البوت الحقيقي @${profile.username} وحفظه في الخادم السحابي بنجاح"
                }.onFailure { err ->
                    _statusBanner.value = "فشل حفظ البوت في قاعدة البيانات: ${err.localizedMessage}"
                }
            }.onFailure { err ->
                _statusBanner.value = "فشل التحقق من التوكن الحقيقي: ${err.localizedMessage}"
            }
            _isBusy.value = false
        }
    }

    fun toggleBotRuntimeState(bot: TelegramBot) {
        viewModelScope.launch {
            val nextStatus = if (bot.status == "running") "stopped" else "running"
            val res = repository.updateBot(bot.copy(status = nextStatus))
            res.onSuccess {
                val actionMsg = if (nextStatus == "running") {
                    "تم تشغيل محرك الاستطلاع الفوري (Long-Polling) للبوت @${bot.username}"
                } else {
                    "تم إيقاف محرك البوت @${bot.username}"
                }
                repository.appendLog(
                    targetName = "@${bot.username}",
                    category = "bot_engine",
                    message = actionMsg
                )
                _statusBanner.value = actionMsg
            }.onFailure { err ->
                _statusBanner.value = "تعذر تحديث حالة البوت: ${err.localizedMessage}"
            }
        }
    }

    fun fetchWebhookDiagnostics(bot: TelegramBot) {
        viewModelScope.launch {
            _isBusy.value = true
            networkEngines.getTelegramWebhookDiagnostics(bot.botToken).onSuccess { info ->
                _webhookDiagnostic.value = info
                repository.appendLog(
                    targetName = "@${bot.username}",
                    category = "bot_engine",
                    message = "فحص getWebhookInfo: التحديثات المعلقة=${info.pendingUpdateCount}, الرابط=${info.url.ifBlank { "غير مفعل" }}"
                )
                _statusBanner.value = "تم جلب تشخيص الـ Webhook الحي من خادم تلجرام"
            }.onFailure { err ->
                _statusBanner.value = "تعذر جلب تشخيص الـ Webhook: ${err.localizedMessage}"
            }
            _isBusy.value = false
        }
    }

    fun configureBotWebhook(bot: TelegramBot, webhookUrl: String, dropPending: Boolean = false) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = networkEngines.setTelegramWebhook(bot.botToken, webhookUrl, dropPending)
            res.onSuccess { desc ->
                val newStatus = if (webhookUrl.isBlank()) "stopped" else "webhook_active"
                repository.updateBot(
                    bot.copy(
                        webhookUrl = webhookUrl.trim(),
                        status = newStatus
                    )
                )
                repository.appendLog(
                    targetName = "@${bot.username}",
                    category = "bot_engine",
                    message = "تحديث Webhook عبر Telegram API: $desc ($webhookUrl)"
                )
                _statusBanner.value = desc
                fetchWebhookDiagnostics(bot)
            }.onFailure { err ->
                _statusBanner.value = "خطأ من خادم تلجرام: ${err.localizedMessage}"
            }
            _isBusy.value = false
        }
    }

    fun syncNativeTelegramMenuCommands(bot: TelegramBot, rules: List<BotAutoReplyRule>) {
        viewModelScope.launch {
            _isBusy.value = true
            networkEngines.syncTelegramBotCommandMenu(bot.botToken, rules).onSuccess { msg ->
                repository.appendLog(
                    targetName = "@${bot.username}",
                    category = "bot_engine",
                    message = msg
                )
                _statusBanner.value = msg
            }.onFailure { err ->
                _statusBanner.value = "تعذر مزامنة قائمة الأوامر: ${err.localizedMessage}"
            }
            _isBusy.value = false
        }
    }

    fun updateBotBioOnTelegram(bot: TelegramBot, fullDescription: String, shortBio: String) {
        viewModelScope.launch {
            _isBusy.value = true
            networkEngines.updateTelegramBotProfileBio(bot.botToken, fullDescription, shortBio).onSuccess { msg ->
                repository.appendLog(
                    targetName = "@${bot.username}",
                    category = "bot_engine",
                    message = msg
                )
                _statusBanner.value = msg
            }.onFailure { err ->
                _statusBanner.value = "تعذر تحديث بروفايل البوت: ${err.localizedMessage}"
            }
            _isBusy.value = false
        }
    }

    fun inspectChannelOrChat(bot: TelegramBot, chatId: String) {
        if (chatId.isBlank()) {
            _statusBanner.value = "يرجى إدخال معرف القناة أو المحادثة (@channel أو Chat ID)"
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            networkEngines.inspectTelegramChat(bot.botToken, chatId).onSuccess { info ->
                _chatInspection.value = info
                repository.appendLog(
                    targetName = "@${bot.username}",
                    category = "bot_engine",
                    message = "فحص قناة/محادثة (${info.title}): عدد الأعضاء=${info.memberCount}, النوع=${info.type}"
                )
                _statusBanner.value = "تم جلب بيانات القناة/المحادثة (${info.title}) بنجاح"
            }.onFailure { err ->
                _statusBanner.value = "تعذر فحص المحادثة: ${err.localizedMessage}"
            }
            _isBusy.value = false
        }
    }

    fun updateBotOfficialNameOnTelegram(bot: TelegramBot, newOfficialName: String) {
        if (newOfficialName.isBlank()) {
            _statusBanner.value = "يرجى كتابة الاسم الرسمي الجديد للبوت"
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            networkEngines.updateTelegramBotOfficialName(bot.botToken, newOfficialName).onSuccess { msg ->
                repository.updateBot(bot.copy(name = newOfficialName.trim()))
                repository.appendLog(
                    targetName = "@${bot.username}",
                    category = "bot_engine",
                    message = msg
                )
                _statusBanner.value = msg
            }.onFailure { err ->
                _statusBanner.value = "تعذر تحديث اسم البوت: ${err.localizedMessage}"
            }
            _isBusy.value = false
        }
    }

    fun sendTelegramPoll(
        bot: TelegramBot,
        chatId: String,
        question: String,
        options: List<String>
    ) {
        if (chatId.isBlank() || question.isBlank()) {
            _statusBanner.value = "يرجى إدخال معرف القناة/المحادثة وسؤال الاستطلاع"
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            networkEngines.sendTelegramPoll(bot.botToken, chatId, question, options).onSuccess { msgId ->
                repository.appendLog(
                    targetName = "@${bot.username}",
                    category = "bot_engine",
                    message = "تم إرسال استطلاع رأي تفاعلي (sendPoll) إلى ($chatId) برقم رسالة #$msgId"
                )
                _statusBanner.value = "تم نشر استطلاع الرأي بنجاح على تلجرام (Message ID: $msgId)"
            }.onFailure { err ->
                _statusBanner.value = "فشل إرسال استطلاع الرأي: ${err.localizedMessage}"
            }
            _isBusy.value = false
        }
    }

    fun sendAndPinTelegramAnnouncement(
        bot: TelegramBot,
        chatId: String,
        messageText: String
    ) {
        if (chatId.isBlank() || messageText.isBlank()) {
            _statusBanner.value = "يرجى إدخال معرف القناة/المحادثة ونص الإعلان المراد تثبيته"
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            networkEngines.sendAndPinTelegramMessage(bot.botToken, chatId, messageText).onSuccess { msgId ->
                repository.appendLog(
                    targetName = "@${bot.username}",
                    category = "bot_engine",
                    message = "تم إرسال وتثبيت إعلان رسمي (pinChatMessage) في ($chatId) برقم #$msgId"
                )
                _statusBanner.value = "تم إرسال الرسالة وتثبيتها أعلى المحادثة بنجاح (#$msgId)"
            }.onFailure { err ->
                _statusBanner.value = "تنبيه التثبيت: ${err.localizedMessage}"
            }
            _isBusy.value = false
        }
    }

    fun sendDirectTelegramMessage(
        bot: TelegramBot,
        chatId: String,
        messageText: String,
        buttonTitle: String = "",
        buttonUrl: String = ""
    ) {
        if (chatId.isBlank() || messageText.isBlank()) {
            _statusBanner.value = "يرجى إدخال معرف المحادثة (Chat ID) ونص الرسالة"
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            val res = networkEngines.sendTelegramMessage(
                botToken = bot.botToken,
                chatId = chatId,
                text = messageText,
                inlineButtonText = buttonTitle,
                inlineButtonUrl = buttonUrl
            )
            res.onSuccess { msgId ->
                repository.appendLog(
                    targetName = "@${bot.username}",
                    category = "bot_engine",
                    message = "تم إرسال رسالة مباشرة إلى Chat ID ($chatId) برقم تسلسلي #$msgId"
                )
                _statusBanner.value = "تم إرسال الرسالة عبر Telegram API بنجاح (Message ID: $msgId)"
            }.onFailure { err ->
                _statusBanner.value = "تعذر الإرسال: ${err.localizedMessage}"
            }
            _isBusy.value = false
        }
    }

    fun updateBotRulesAndWelcome(
        bot: TelegramBot,
        welcomeMessage: String,
        rules: List<BotAutoReplyRule>
    ) {
        viewModelScope.launch {
            val rulesJson = networkEngines.serializeAutoReplyRules(rules)
            val res = repository.updateBot(
                bot.copy(
                    welcomeMessage = welcomeMessage.trim(),
                    autoReplyRulesJson = rulesJson
                )
            )
            res.onSuccess {
                repository.appendLog(
                    targetName = "@${bot.username}",
                    category = "bot_engine",
                    message = "تم تحديث قواعد الرد الآلي (${rules.size} أمر) ورسالة الترحيب."
                )
                _statusBanner.value = "تم حفظ إعدادات وأوامر البوت في الخادم السحابي"
            }
        }
    }

    fun deleteBot(bot: TelegramBot) {
        viewModelScope.launch {
            activePollingJobs.remove(bot.id)?.cancel()
            repository.deleteBot(bot.id).onSuccess {
                repository.appendLog(
                    targetName = "@${bot.username}",
                    category = "bot_engine",
                    message = "تم إزالة البوت من الخادم السحابي."
                )
                _statusBanner.value = "تم حذف البوت @${bot.username}"
            }
        }
    }

    fun parseRules(bot: TelegramBot): List<BotAutoReplyRule> {
        return networkEngines.parseAutoReplyRules(bot.autoReplyRulesJson)
    }

    private fun syncActiveBotEngines(bots: List<TelegramBot>) {
        val runningIds = bots.filter { it.status == "running" }.map { it.id }.toSet()

        val toStop = activePollingJobs.keys.filter { it !in runningIds }
        toStop.forEach { id ->
            activePollingJobs.remove(id)?.cancel()
        }

        bots.filter { it.status == "running" && !activePollingJobs.containsKey(it.id) }.forEach { bot ->
            val job = viewModelScope.launch {
                var currentOffset = bot.lastUpdateId
                var currentCount = bot.processedUpdatesCount
                while (isActive) {
                    val pollResult = networkEngines.pollAndProcessTelegramUpdates(
                        botToken = bot.botToken,
                        lastUpdateId = currentOffset,
                        welcomeMessage = bot.welcomeMessage,
                        rulesJson = bot.autoReplyRulesJson
                    )
                    pollResult.onSuccess { events ->
                        if (events.isNotEmpty()) {
                            val maxId = events.maxOf { it.updateId }
                            currentOffset = maxId
                            currentCount += events.size
                            repository.updateBot(
                                bot.copy(
                                    lastUpdateId = currentOffset,
                                    processedUpdatesCount = currentCount
                                )
                            )
                            events.forEach { ev ->
                                val replyLog = if (ev.autoRepliedWith != null) {
                                    " -> تم الرد تلقائياً: ${ev.autoRepliedWith}"
                                } else {
                                    ""
                                }
                                repository.appendLog(
                                    targetName = "@${bot.username}",
                                    category = "bot_engine",
                                    message = "رسالة واردة من ${ev.senderName} (${ev.chatId}): ${ev.text}$replyLog"
                                )
                            }
                        }
                    }
                    delay(4000L)
                }
            }
            activePollingJobs[bot.id] = job
        }
    }

    // =========================================================================
    // 2. ENTERPRISE CLOUD VAULT (Vercel Edge / Netlify CDN / GitHub Pages)
    // =========================================================================

    fun updateCloudProviderTokens(
        vercelToken: String,
        netlifyToken: String,
        githubPat: String
    ) {
        val cleanVercel = vercelToken.trim()
            .substringBefore("nfp_")
            .substringBefore("ghp_")
            .trim()
        val cleanNetlify = netlifyToken.trim()
            .substringBefore("vcp_")
            .substringBefore("ghp_")
            .trim()
        val cleanGithub = githubPat.trim()
            .substringBefore("vcp_")
            .substringBefore("nfp_")
            .trim()

        _runtimeVercelToken.value = cleanVercel
        _runtimeNetlifyToken.value = cleanNetlify
        _runtimeGithubPat.value = cleanGithub
        _statusBanner.value = "تم تنظيف وحفظ وتفعيل مفاتيح الخوادم السحابية (Vercel / Netlify / GitHub) بنجاح"
    }

    fun refreshVercelDeployments() {
        viewModelScope.launch {
            _isBusy.value = true
            networkEngines.listVercelAccountDeployments(_runtimeVercelToken.value).onSuccess { list ->
                _surgeLiveDomains.value = list
                _statusBanner.value = "تم جلب ${list.size} موقع منشور على شبكة Vercel Edge CDN"
            }.onFailure { err ->
                _statusBanner.value = "تعذر جلب مواقع Vercel: ${err.localizedMessage}"
            }
            _isBusy.value = false
        }
    }

    fun refreshNetlifySites() {
        viewModelScope.launch {
            _isBusy.value = true
            networkEngines.listNetlifyAccountSites(_runtimeNetlifyToken.value).onSuccess { list ->
                _surgeLiveDomains.value = list
                _statusBanner.value = "تم جلب ${list.size} موقع منشور على شبكة Netlify Global CDN"
            }.onFailure { err ->
                _statusBanner.value = "تعذر جلب مواقع Netlify: ${err.localizedMessage}"
            }
            _isBusy.value = false
        }
    }

    fun createNewWebsiteProject(
        projectName: String,
        subdomainSlug: String,
        provider: String,
        templateKey: String = "CORPORATE"
    ) {
        if (projectName.isBlank() || subdomainSlug.isBlank()) {
            _statusBanner.value = "يرجى إدخال اسم المشروع والمعرف الفرعي للنطاق (Subdomain Slug)"
            return
        }
        val (html, css, js) = when (templateKey) {
            "ECOMMERCE" -> Triple(
                WebSiteProject.ECOMMERCE_HTML_TEMPLATE,
                WebSiteProject.ECOMMERCE_CSS_TEMPLATE,
                WebSiteProject.ECOMMERCE_JS_TEMPLATE
            )
            "STATUS_PORTAL" -> Triple(
                WebSiteProject.STATUS_PORTAL_HTML_TEMPLATE,
                WebSiteProject.STATUS_PORTAL_CSS_TEMPLATE,
                WebSiteProject.STATUS_PORTAL_JS_TEMPLATE
            )
            "PORTFOLIO" -> Triple(
                WebSiteProject.PORTFOLIO_HTML_TEMPLATE,
                WebSiteProject.PORTFOLIO_CSS_TEMPLATE,
                WebSiteProject.PORTFOLIO_JS_TEMPLATE
            )
            else -> Triple(
                WebSiteProject.DEFAULT_HTML_TEMPLATE,
                WebSiteProject.DEFAULT_CSS_TEMPLATE,
                WebSiteProject.DEFAULT_JS_TEMPLATE
            )
        }
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.createSiteProject(
                projectName = projectName,
                subdomainSlug = subdomainSlug,
                provider = provider,
                htmlContent = html,
                cssContent = css,
                jsContent = js
            )
            res.onSuccess {
                repository.appendLog(
                    targetName = projectName,
                    category = "site_deploy",
                    message = "تم إنشاء مستودع ملفات الموقع ($subdomainSlug) بقالب $templateKey في الخادم السحابي."
                )
                _statusBanner.value = "تم إنشاء مشروع الموقع $projectName بنجاح"
            }.onFailure { err ->
                _statusBanner.value = "تعذر إنشاء المشروع: ${err.localizedMessage}"
            }
            _isBusy.value = false
        }
    }

    fun saveAndDeployWebsite(
        site: WebSiteProject,
        htmlContent: String,
        cssContent: String,
        jsContent: String,
        provider: String
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            _statusBanner.value = "جاري بناء حزمة الموقع (HTML/CSS/JS) ورفعها على النطاق الحي..."
            val updatedDraft = site.copy(
                htmlContent = htmlContent,
                cssContent = cssContent,
                jsContent = jsContent,
                provider = provider
            )
            repository.updateSiteProject(updatedDraft)

            val deployResult = networkEngines.deployWebsiteToLiveDomain(
                projectName = site.projectName,
                subdomainSlug = site.subdomainSlug,
                provider = provider,
                htmlContent = htmlContent,
                cssContent = cssContent,
                jsContent = jsContent,
                runtimeVercelToken = _runtimeVercelToken.value,
                runtimeNetlifyToken = _runtimeNetlifyToken.value,
                runtimeGithubPat = _runtimeGithubPat.value
            )

            deployResult.onSuccess { (liveUrl, buildLog) ->
                repository.updateSiteProject(
                    updatedDraft.copy(
                        liveDomainUrl = liveUrl,
                        buildStatus = "deployed",
                        lastBuildLog = buildLog
                    )
                )
                repository.appendLog(
                    targetName = site.projectName,
                    category = "site_deploy",
                    message = "تم بناء ورفع الموقع على الرابط الحقيقي: $liveUrl"
                )
                _statusBanner.value = "تم نشر الموقع بنجاح على الرابط الحي: $liveUrl"
            }.onFailure { err ->
                val failLog = "فشل الرفع: ${err.localizedMessage}"
                repository.updateSiteProject(
                    updatedDraft.copy(
                        buildStatus = "failed",
                        lastBuildLog = failLog
                    )
                )
                repository.appendLog(
                    targetName = site.projectName,
                    category = "site_deploy",
                    message = failLog
                )
                _statusBanner.value = failLog
            }
            _isBusy.value = false
        }
    }

    fun deleteWebsiteProject(site: WebSiteProject) {
        viewModelScope.launch {
            repository.deleteSiteProject(site.id).onSuccess {
                repository.appendLog(
                    targetName = site.projectName,
                    category = "site_deploy",
                    message = "تم حذف مشروع الموقع من الخادم السحابي."
                )
                _statusBanner.value = "تم حذف الموقع ${site.projectName}"
            }
        }
    }

    fun compilePreviewHtml(site: WebSiteProject): String {
        return networkEngines.compileSingleFileWebsiteBundle(
            projectName = site.projectName,
            htmlContent = site.htmlContent,
            cssContent = site.cssContent,
            jsContent = site.jsContent
        )
    }

    fun fetchWebsiteHtmlFromLiveUrl(targetUrl: String, onLoaded: (String) -> Unit) {
        if (targetUrl.isBlank()) {
            _statusBanner.value = "يرجى إدخال رابط الموقع المراد سحب الكود المصدري منه"
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            _statusBanner.value = "جاري الاتصال بالرابط وسحب الكود المصدري الحقيقي..."
            networkEngines.fetchLiveWebsiteHtmlSource(targetUrl).onSuccess { fetchedHtml ->
                onLoaded(fetchedHtml)
                repository.appendLog(
                    targetName = targetUrl.trim(),
                    category = "site_deploy",
                    message = "تم سحب واستيراد كود HTML الحي (${fetchedHtml.length} حرف) بنجاح."
                )
                _statusBanner.value = "تم استيراد كود HTML الحقيقي (${fetchedHtml.length} حرف) إلى محرر الموقع بنجاح"
            }.onFailure { err ->
                _statusBanner.value = "فشل استيراد الكود من الرابط: ${err.localizedMessage}"
            }
            _isBusy.value = false
        }
    }

    fun auditWebsiteCode(html: String, css: String, js: String): WebsiteCodeAuditReport {
        return networkEngines.auditWebsiteCode(html, css, js)
    }

    // =========================================================================
    // 3. MULTI-RECORD DNS-OVER-HTTPS & SSL/HTTPS INSPECTOR
    // =========================================================================

    fun checkRealDomain(domainQuery: String) {
        if (domainQuery.isBlank()) {
            _statusBanner.value = "يرجى كتابة النطاق المراد فحصه"
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            _statusBanner.value = "جاري فحص سجلات A, AAAA, CNAME, MX, TXT, NS وشهادة SSL عبر خادم dns.google..."
            val candidates = if (!domainQuery.contains(".")) {
                val base = domainQuery.trim().lowercase().replace(Regex("[^a-z0-9\\-]"), "")
                listOf(
                    "$base.vercel.app",
                    "$base.netlify.app",
                    "$base.github.io",
                    "$base.pages.dev"
                )
            } else {
                listOf(domainQuery.trim())
            }

            val newReports = mutableListOf<DnsMultiRecordReport>()
            for (candidate in candidates) {
                networkEngines.checkFullDomainAndSslReport(candidate).onSuccess { rep ->
                    newReports.add(rep)
                }.onFailure { err ->
                    _statusBanner.value = "خطأ أثناء فحص $candidate: ${err.localizedMessage}"
                }
            }

            if (newReports.isNotEmpty()) {
                _dnsReports.value = newReports + _dnsReports.value
                val availableCount = newReports.count { it.isAvailableForClaim }
                repository.appendLog(
                    targetName = domainQuery.trim(),
                    category = "domain_dns",
                    message = "فحص DNS شامل لـ ${newReports.size} نطاق: وجد $availableCount نطاق غير مستخدم ومتاح."
                )
                _statusBanner.value = "اكتمل الفحص الشامل: $availableCount من أصل ${newReports.size} نطاق غير مستخدم ومتاح"
            }
            _isBusy.value = false
        }
    }

    override fun onCleared() {
        super.onCleared()
        activePollingJobs.values.forEach { it.cancel() }
        activePollingJobs.clear()
    }
}
