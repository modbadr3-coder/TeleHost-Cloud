package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.model.DeploymentLog
import com.example.data.model.TelegramBot
import com.example.data.model.WebSiteProject
import com.example.data.repository.TeleHostRepository
import com.example.ui.auth.AuthGateScreen
import com.example.ui.auth.signOut
import com.example.ui.components.RainbowTelegramTopBarPill
import com.example.ui.screens.DomainsDnsTabScreen
import com.example.ui.screens.OverviewTabScreen
import com.example.ui.screens.TelegramBotsTabScreen
import com.example.ui.screens.WebsitesBuilderTabScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.TeleHostViewModel
import com.example.ui.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    TeleHostAppRoot()
                }
            }
        }
    }
}

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

@Composable
fun TeleHostAppRoot(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        AuthGateScreen(onAuthSuccess = {})
    } else {
        TeleHostMainConsole(
            currentUserId = user.uid,
            userEmail = user.email.orEmpty()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeleHostMainConsole(
    currentUserId: String,
    userEmail: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val viewModel: TeleHostViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                TeleHostViewModel(
                    repository = TeleHostRepository(db),
                    currentUserId = currentUserId
                )
            }
        }
    )

    val botsUiState by viewModel.botsState.collectAsStateWithLifecycle()
    val sitesUiState by viewModel.sitesState.collectAsStateWithLifecycle()
    val logsUiState by viewModel.logsState.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    val dnsReports by viewModel.dnsReports.collectAsStateWithLifecycle()
    val runtimeVercelToken by viewModel.runtimeVercelToken.collectAsStateWithLifecycle()
    val runtimeNetlifyToken by viewModel.runtimeNetlifyToken.collectAsStateWithLifecycle()
    val runtimeGithubPat by viewModel.runtimeGithubPat.collectAsStateWithLifecycle()
    val surgeLiveDomains by viewModel.surgeLiveDomains.collectAsStateWithLifecycle()
    val webhookDiagnostic by viewModel.webhookDiagnostic.collectAsStateWithLifecycle()
    val chatInspection by viewModel.chatInspection.collectAsStateWithLifecycle()

    val bots: List<TelegramBot> = (botsUiState as? UiState.Success)?.data ?: emptyList()
    val sites: List<WebSiteProject> = (sitesUiState as? UiState.Success)?.data ?: emptyList()
    val logs: List<DeploymentLog> = (logsUiState as? UiState.Success)?.data ?: emptyList()

    var currentTab by rememberSaveable { mutableStateOf("OVERVIEW") } // "OVERVIEW", "BOTS", "SITES", "DOMAINS"

    if (currentTab != "OVERVIEW") {
        BackHandler {
            currentTab = "OVERVIEW"
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = Color(0xFF0B0F19),
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF111827),
                    titleContentColor = Color(0xFFF9FAFB)
                ),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = null,
                            tint = Color(0xFF06B6D4),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "TeleHost Cloud",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF9FAFB)
                            )
                            if (userEmail.isNotBlank()) {
                                Text(
                                    text = userEmail,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFF10B981)
                                )
                            }
                        }
                    }
                },
                actions = {
                    RainbowTelegramTopBarPill()
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = {
                            signOut(
                                context = context,
                                credentialManager = credentialManager,
                                onSignOutComplete = {},
                                scope = scope
                            )
                        },
                        modifier = Modifier.testTag("sign_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "تسجيل الخروج",
                            tint = Color(0xFFEF4444)
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF111827),
                contentColor = Color(0xFFF9FAFB)
            ) {
                val navItems = listOf(
                    Triple("OVERVIEW", "مركز الخادم", Icons.Default.Dashboard),
                    Triple("BOTS", "بوتات تلجرام", Icons.Default.SmartToy),
                    Triple("SITES", "بناء المواقع", Icons.Default.Language),
                    Triple("DOMAINS", "فحص النطاقات", Icons.Default.Dns)
                )
                navItems.forEach { (key, label, icon) ->
                    NavigationBarItem(
                        selected = currentTab == key,
                        onClick = { currentTab = key },
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label) },
                        modifier = Modifier.testTag("nav_tab_$key"),
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF0B0F19),
                            selectedTextColor = Color(0xFF06B6D4),
                            indicatorColor = Color(0xFF06B6D4),
                            unselectedIconColor = Color(0xFF9CA3AF),
                            unselectedTextColor = Color(0xFF9CA3AF)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (statusBanner != null) {
                Surface(
                    color = Color(0xFF06B6D4).copy(alpha = 0.16f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFF06B6D4)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = statusBanner.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFF9FAFB),
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { viewModel.clearBanner() }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "إغلاق التنبيه",
                                tint = Color(0xFF06B6D4),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                when (currentTab) {
                    "OVERVIEW" -> OverviewTabScreen(
                        bots = bots,
                        sites = sites,
                        logs = logs,
                        onNavigateToBots = { currentTab = "BOTS" },
                        onNavigateToSites = { currentTab = "SITES" },
                        onNavigateToDomains = { currentTab = "DOMAINS" }
                    )

                    "BOTS" -> TelegramBotsTabScreen(
                        bots = bots,
                        isBusy = isBusy,
                        webhookDiagnostic = webhookDiagnostic,
                        chatInspection = chatInspection,
                        parseRules = { viewModel.parseRules(it) },
                        onVerifyAndAddBot = { name, token, welcome ->
                            viewModel.verifyAndAddTelegramBot(name, token, welcome)
                        },
                        onToggleBotState = { viewModel.toggleBotRuntimeState(it) },
                        onFetchWebhookDiagnostics = { bot ->
                            viewModel.fetchWebhookDiagnostics(bot)
                        },
                        onConfigureWebhook = { bot, url, dropPending ->
                            viewModel.configureBotWebhook(bot, url, dropPending)
                        },
                        onSyncNativeCommandMenu = { bot, rules ->
                            viewModel.syncNativeTelegramMenuCommands(bot, rules)
                        },
                        onUpdateBotProfileBio = { bot, fullDesc, shortBio ->
                            viewModel.updateBotBioOnTelegram(bot, fullDesc, shortBio)
                        },
                        onUpdateBotOfficialName = { bot, newName ->
                            viewModel.updateBotOfficialNameOnTelegram(bot, newName)
                        },
                        onInspectChat = { bot, chatId ->
                            viewModel.inspectChannelOrChat(bot, chatId)
                        },
                        onSendDirectMessage = { bot, chatId, msg, btnLabel, btnUrl ->
                            viewModel.sendDirectTelegramMessage(bot, chatId, msg, btnLabel, btnUrl)
                        },
                        onSendAndPinMessage = { bot, chatId, msg, _, _ ->
                            viewModel.sendAndPinTelegramAnnouncement(bot, chatId, msg)
                        },
                        onSendPoll = { bot, chatId, question, options ->
                            viewModel.sendTelegramPoll(bot, chatId, question, options)
                        },
                        onSaveRulesAndWelcome = { bot, welcome, rules ->
                            viewModel.updateBotRulesAndWelcome(bot, welcome, rules)
                        },
                        onDeleteBot = { viewModel.deleteBot(it) }
                    )

                    "SITES" -> WebsitesBuilderTabScreen(
                        sites = sites,
                        surgeLiveDomains = surgeLiveDomains,
                        isBusy = isBusy,
                        runtimeVercelToken = runtimeVercelToken,
                        runtimeNetlifyToken = runtimeNetlifyToken,
                        runtimeGithubPat = runtimeGithubPat,
                        compilePreviewHtml = { viewModel.compilePreviewHtml(it) },
                        auditWebsiteCode = { html, css, js ->
                            viewModel.auditWebsiteCode(html, css, js)
                        },
                        onFetchLiveUrlHtml = { targetUrl, onHtmlLoaded ->
                            viewModel.fetchWebsiteHtmlFromLiveUrl(targetUrl, onHtmlLoaded)
                        },
                        onUpdateCloudTokens = { vercel, netlify, github ->
                            viewModel.updateCloudProviderTokens(vercel, netlify, github)
                        },
                        onRefreshVercelDomains = {
                            viewModel.refreshVercelDeployments()
                        },
                        onRefreshNetlifyDomains = {
                            viewModel.refreshNetlifySites()
                        },
                        onCreateProject = { name, slug, prov, templateKey ->
                            viewModel.createNewWebsiteProject(name, slug, prov, templateKey)
                        },
                        onSaveAndDeploy = { site, html, css, js, prov ->
                            viewModel.saveAndDeployWebsite(site, html, css, js, prov)
                        },
                        onDeleteProject = { viewModel.deleteWebsiteProject(it) }
                    )

                    "DOMAINS" -> DomainsDnsTabScreen(
                        dnsReports = dnsReports,
                        isBusy = isBusy,
                        onCheckDomain = { viewModel.checkRealDomain(it) },
                        onClaimDomainForNewSite = { claimedSlug ->
                            viewModel.createNewWebsiteProject(
                                projectName = "موقع $claimedSlug",
                                subdomainSlug = claimedSlug,
                                provider = "vercel_edge",
                                templateKey = "CORPORATE"
                            )
                            currentTab = "SITES"
                        }
                    )
                }
            }
        }
    }
}
