package com.example.ui.screens

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.SurgeHostedProject
import com.example.data.model.WebSiteProject
import com.example.data.model.WebsiteCodeAuditReport
import com.example.ui.theme.JetBrainsMonoFontFamily

@Composable
fun WebsitesBuilderTabScreen(
    sites: List<WebSiteProject>,
    surgeLiveDomains: List<SurgeHostedProject>,
    isBusy: Boolean,
    runtimeVercelToken: String = "",
    runtimeNetlifyToken: String = "",
    runtimeGithubPat: String = "",
    compilePreviewHtml: (WebSiteProject) -> String,
    auditWebsiteCode: (String, String, String) -> WebsiteCodeAuditReport? = { _, _, _ -> null },
    onFetchLiveUrlHtml: (String, (String) -> Unit) -> Unit = { _, _ -> },
    onUpdateCloudTokens: (vercel: String, netlify: String, github: String) -> Unit = { _, _, _ -> },
    onRefreshVercelDomains: () -> Unit = {},
    onRefreshNetlifyDomains: () -> Unit = {},
    onCreateProject: (name: String, slug: String, provider: String, templateKey: String) -> Unit,
    onSaveAndDeploy: (WebSiteProject, String, String, String, String) -> Unit,
    onDeleteProject: (WebSiteProject) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("telehost_cloud_vault", Context.MODE_PRIVATE) }

    var selectedSiteId by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedSite = sites.firstOrNull { it.id == selectedSiteId }

    var projectNameInput by rememberSaveable { mutableStateOf("") }
    var slugInput by rememberSaveable { mutableStateOf("") }
    var selectedProvider by rememberSaveable { mutableStateOf("vercel_edge") }
    var selectedTemplate by rememberSaveable { mutableStateOf("CORPORATE") }

    var activeVaultTab by rememberSaveable { mutableStateOf("VERCEL") } // "VERCEL", "NETLIFY", "GITHUB"
    var vercelTokenInput by rememberSaveable(runtimeVercelToken) {
        mutableStateOf(runtimeVercelToken.ifBlank { prefs.getString("vercel_token", "").orEmpty() })
    }
    var netlifyTokenInput by rememberSaveable(runtimeNetlifyToken) {
        mutableStateOf(runtimeNetlifyToken.ifBlank { prefs.getString("netlify_token", "").orEmpty() })
    }
    var githubPatInput by rememberSaveable(runtimeGithubPat) {
        mutableStateOf(runtimeGithubPat.ifBlank { prefs.getString("github_pat", "").orEmpty() })
    }

    LaunchedEffect(Unit) {
        val savedV = prefs.getString("vercel_token", "").orEmpty()
        val savedN = prefs.getString("netlify_token", "").orEmpty()
        val savedG = prefs.getString("github_pat", "").orEmpty()
        if ((savedV.isNotBlank() || savedN.isNotBlank() || savedG.isNotBlank()) &&
            runtimeVercelToken.isBlank() && runtimeNetlifyToken.isBlank() && runtimeGithubPat.isBlank()
        ) {
            onUpdateCloudTokens(savedV, savedN, savedG)
        }
    }

    fun persistAndActivateTokens(v: String, n: String, g: String) {
        prefs.edit()
            .putString("vercel_token", v.trim())
            .putString("netlify_token", n.trim())
            .putString("github_pat", g.trim())
            .apply()
        onUpdateCloudTokens(v.trim(), n.trim(), g.trim())
    }

    if (selectedSite != null) {
        BackHandler {
            selectedSiteId = null
        }
        WebsiteStudioEditorScreen(
            site = selectedSite,
            isBusy = isBusy,
            compilePreviewHtml = compilePreviewHtml,
            auditWebsiteCode = auditWebsiteCode,
            onFetchLiveUrlHtml = onFetchLiveUrlHtml,
            onBack = { selectedSiteId = null },
            onDeploy = { html, css, js, prov ->
                onSaveAndDeploy(selectedSite, html, css, js, prov)
            },
            onDelete = {
                onDeleteProject(selectedSite)
                selectedSiteId = null
            }
        )
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("sites_tab_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Enterprise Cloud Hosting Vault (Vercel Edge, Netlify CDN, GitHub Pages)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
                border = BorderStroke(1.dp, Color(0xFF06B6D4).copy(alpha = 0.6f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = null,
                                tint = Color(0xFF06B6D4),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "خزنة الخوادم السحابية العظمى (Vercel Edge / Netlify CDN / GitHub)",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color(0xFFF9FAFB)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "استخدم أقوى شبكات الاستضافة العالمية (Vercel Edge وNetlify CDN وGitHub Pages) للنشر المباشر بالـ REST API على نطاقات رسمية مجانية فائقة السرعة.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF9CA3AF)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "VERCEL" to "Vercel Edge (.vercel.app)",
                            "NETLIFY" to "Netlify CDN (.netlify.app)",
                            "GITHUB" to "GitHub Pages (.github.io)"
                        ).forEach { (tabKey, tabLabel) ->
                            FilterChip(
                                selected = activeVaultTab == tabKey,
                                onClick = { activeVaultTab = tabKey },
                                label = { Text(tabLabel) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF06B6D4).copy(alpha = 0.22f),
                                    selectedLabelColor = Color(0xFF06B6D4)
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            val clipText = clipboard?.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString().orEmpty()
                            if (clipText.isNotBlank()) {
                                val vMatch = Regex("vcp_[A-Za-z0-9_]+").find(clipText)?.value?.substringBefore("nfp_")?.substringBefore("ghp_")
                                val nMatch = Regex("nfp_[A-Za-z0-9_]+").find(clipText)?.value?.substringBefore("vcp_")?.substringBefore("ghp_")
                                val gMatch = Regex("(ghp_[A-Za-z0-9_]+|github_pat_[A-Za-z0-9_]+)").find(clipText)?.value
                                if (!vMatch.isNullOrBlank()) vercelTokenInput = vMatch
                                if (!nMatch.isNullOrBlank()) netlifyTokenInput = nMatch
                                if (!gMatch.isNullOrBlank()) githubPatInput = gMatch
                                persistAndActivateTokens(vercelTokenInput, netlifyTokenInput, githubPatInput)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF59E0B)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("لصق ذكي وفصل تلقائي لجميع المفاتيح من الحافظة (vcp_ / nfp_ / ghp_)")
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    when (activeVaultTab) {
                        "VERCEL" -> {
                            Text(
                                text = "شبكة Vercel Global Edge ترفع ملفات موقعك (HTML/CSS/JS + vercel.json) عبر POST https://api.vercel.com/v13/deployments وتفعل نطاق https://<slug>.vercel.app فوراً.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF10B981)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse("https://vercel.com/account/tokens"))
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                border = BorderStroke(1.dp, Color(0xFF06B6D4)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF06B6D4)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("فتح رابط استخراج VERCEL_TOKEN المباشر (vercel.com/account/tokens)")
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = vercelTokenInput,
                                onValueChange = { vercelTokenInput = it },
                                label = { Text("مفتاح VERCEL_TOKEN") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("vercel_token_input"),
                                colors = darkTextFieldColors()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        persistAndActivateTokens(
                                            vercelTokenInput,
                                            netlifyTokenInput,
                                            githubPatInput
                                        )
                                    },
                                    enabled = !isBusy,
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF10B981),
                                        contentColor = Color(0xFF0B0F19)
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("حفظ وتفعيل VERCEL_TOKEN", fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = onRefreshVercelDomains,
                                    enabled = !isBusy,
                                    border = BorderStroke(1.dp, Color(0xFF06B6D4)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF06B6D4)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("مواقعي")
                                }
                            }
                        }

                        "NETLIFY" -> {
                            Text(
                                text = "شبكة Netlify Global CDN تضغط ملفات موقعك في أرشيف .zip في الذاكرة مع توجيه _redirects وترفعها مباشرة إلى https://api.netlify.com/api/v1/sites لتفعيل نطاق https://<slug>.netlify.app.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF10B981)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = {
                                    context.startActivity(
                                        Intent(
                                            Intent.ACTION_VIEW,
                                            Uri.parse("https://app.netlify.com/user/applications#personal-access-tokens")
                                        )
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                border = BorderStroke(1.dp, Color(0xFF06B6D4)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF06B6D4)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("فتح رابط استخراج NETLIFY_TOKEN المباشر (app.netlify.com)")
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = netlifyTokenInput,
                                onValueChange = { netlifyTokenInput = it },
                                label = { Text("مفتاح NETLIFY_TOKEN") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("netlify_token_input"),
                                colors = darkTextFieldColors()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        persistAndActivateTokens(
                                            vercelTokenInput,
                                            netlifyTokenInput,
                                            githubPatInput
                                        )
                                    },
                                    enabled = !isBusy,
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF10B981),
                                        contentColor = Color(0xFF0B0F19)
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("حفظ وتفعيل NETLIFY_TOKEN", fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = onRefreshNetlifyDomains,
                                    enabled = !isBusy,
                                    border = BorderStroke(1.dp, Color(0xFF06B6D4)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF06B6D4)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("مواقعي")
                                }
                            }
                        }

                        "GITHUB" -> {
                            Text(
                                text = "ينشئ مستودع GitHub عام تلقائياً، يرفع ملفات الموقع، ويفعل استضافة GitHub Pages الرسمية على النطاق https://<username>.github.io/<repo>/.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF10B981)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = {
                                    context.startActivity(
                                        Intent(
                                            Intent.ACTION_VIEW,
                                            Uri.parse("https://github.com/settings/tokens/new?scopes=repo,workflow&description=TeleHostCloud")
                                        )
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                border = BorderStroke(1.dp, Color(0xFF06B6D4)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF06B6D4)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("فتح رابط استخراج GITHUB_PAT المباشر (github.com/settings/tokens)")
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = githubPatInput,
                                onValueChange = { githubPatInput = it },
                                label = { Text("مفتاح GITHUB_PAT (Personal Access Token)") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("github_pat_input"),
                                colors = darkTextFieldColors()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    persistAndActivateTokens(
                                        vercelTokenInput,
                                        netlifyTokenInput,
                                        githubPatInput
                                    )
                                },
                                enabled = !isBusy,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF10B981),
                                    contentColor = Color(0xFF0B0F19)
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("حفظ وتفعيل GITHUB_PAT", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (surgeLiveDomains.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "النطاقات النشطة على الخادم السحابي (${surgeLiveDomains.size}):",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF06B6D4)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        surgeLiveDomains.forEach { item ->
                            Surface(
                                color = Color(0xFF0B0F19),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFF1F2937)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "https://${item.domain}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontFamily = JetBrainsMonoFontFamily,
                                            color = Color(0xFF10B981)
                                        )
                                        Text(
                                            text = "${item.platformInfo} | الحالة: ${item.lastUpdated}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF9CA3AF)
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://${item.domain}")))
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                            contentDescription = "فتح النطاق",
                                            tint = Color(0xFF06B6D4)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Create New Site Project with Ready Templates & 4 Cloud Providers
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
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "إنشاء مشروع موقع جديد مع قوالب احترافية جاهزة",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color(0xFFF9FAFB)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = projectNameInput,
                        onValueChange = { projectNameInput = it },
                        label = { Text("اسم الموقع (مثال: بوابة الخدمات الذكية)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("site_name_input"),
                        colors = darkTextFieldColors()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = slugInput,
                        onValueChange = { slugInput = it },
                        label = { Text("النطاق الفرعي (مثال: smart-portal-2026)") },
                        placeholder = { Text("smart-portal-2026") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("site_slug_input"),
                        colors = darkTextFieldColors()
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "اختر القالب البرمجي المبدئي (HTML/CSS/JS):",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF9CA3AF)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "CORPORATE" to "بوابة شركات",
                            "ECOMMERCE" to "متجر باقات",
                            "STATUS_PORTAL" to "لوحة حالة الخوادم",
                            "PORTFOLIO" to "بورتفوليو مطور"
                        ).forEach { (key, label) ->
                            FilterChip(
                                selected = selectedTemplate == key,
                                onClick = { selectedTemplate = key },
                                label = { Text(label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF06B6D4).copy(alpha = 0.2f),
                                    selectedLabelColor = Color(0xFF06B6D4)
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "اختر شبكة الاستضافة السحابية (Vercel / Netlify / GitHub):",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF9CA3AF)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "vercel_edge" to "Vercel Edge (.vercel.app)",
                            "netlify_cdn" to "Netlify CDN (.netlify.app)",
                            "github_pages" to "GitHub Pages (.github.io)",
                            "paste_gateway" to "Cloud Gateway (فوري)"
                        ).forEach { (provKey, provLabel) ->
                            FilterChip(
                                selected = selectedProvider == provKey,
                                onClick = { selectedProvider = provKey },
                                label = { Text(provLabel) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF10B981).copy(alpha = 0.2f),
                                    selectedLabelColor = Color(0xFF10B981)
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            onCreateProject(projectNameInput, slugInput, selectedProvider, selectedTemplate)
                            projectNameInput = ""
                            slugInput = ""
                        },
                        enabled = !isBusy && projectNameInput.isNotBlank() && slugInput.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("create_site_project_btn"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF10B981),
                            contentColor = Color(0xFF0B0F19)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("إنشاء مستودع الموقع بالقالب المختار", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Text(
                text = "مشاريع المواقع المستضافة (${sites.size})",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFFF9FAFB)
            )
        }

        if (sites.isEmpty()) {
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
                            text = "لا توجد مواقع بعد. أنشئ مشروع موقع أعلاه لرفع ملفاتك ونشرها على رابط عام حقيقي.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF9CA3AF)
                        )
                    }
                }
            }
        } else {
            items(sites, key = { it.id }) { site ->
                SiteProjectCard(
                    site = site,
                    onOpenEditor = { selectedSiteId = site.id }
                )
            }
        }
    }
}

@Composable
private fun SiteProjectCard(
    site: WebSiteProject,
    onOpenEditor: () -> Unit
) {
    val context = LocalContext.current
    val statusColor = when (site.buildStatus) {
        "deployed" -> Color(0xFF10B981)
        "failed" -> Color(0xFFEF4444)
        else -> Color(0xFFF59E0B)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("site_card_${site.id}"),
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
                        text = site.projectName,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFFF9FAFB)
                    )
                    Text(
                        text = "Subdomain: ${site.subdomainSlug} (${site.provider})",
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
                        text = site.buildStatus.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            if (site.liveDomainUrl.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = Color(0xFF0B0F19),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFF10B981)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = site.liveDomainUrl,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = JetBrainsMonoFontFamily,
                            color = Color(0xFF10B981),
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(site.liveDomainUrl))
                                context.startActivity(intent)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = "فتح الرابط في المتصفح",
                                tint = Color(0xFF10B981)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onOpenEditor,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("open_site_studio_${site.id}"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF06B6D4),
                    contentColor = Color(0xFF0B0F19)
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("فتح استوديو الملفات والبناء والرفع المباشر", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun WebsiteStudioEditorScreen(
    site: WebSiteProject,
    isBusy: Boolean,
    compilePreviewHtml: (WebSiteProject) -> String,
    auditWebsiteCode: (String, String, String) -> WebsiteCodeAuditReport?,
    onFetchLiveUrlHtml: (String, (String) -> Unit) -> Unit,
    onBack: () -> Unit,
    onDeploy: (String, String, String, String) -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    var activeFileTab by rememberSaveable { mutableStateOf("HTML") }
    var htmlCode by rememberSaveable(site.id) { mutableStateOf(site.htmlContent) }
    var cssCode by rememberSaveable(site.id) { mutableStateOf(site.cssContent) }
    var jsCode by rememberSaveable(site.id) { mutableStateOf(site.jsContent) }
    var provider by rememberSaveable(site.id) { mutableStateOf(site.provider.ifBlank { "vercel_edge" }) }
    var importUrlInput by rememberSaveable(site.id) { mutableStateOf("") }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }.orEmpty()
                if (text.isNotEmpty()) {
                    when (activeFileTab) {
                        "CSS" -> cssCode = text
                        "JS" -> jsCode = text
                        else -> htmlCode = text
                    }
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("website_studio_editor"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("back_from_site_studio")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع",
                            tint = Color(0xFFF9FAFB)
                        )
                    }
                    Column {
                        Text(
                            text = "استوديو بناء ونشر: ${site.projectName}",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color(0xFFF9FAFB)
                        )
                        Text(
                            text = "Subdomain: ${site.subdomainSlug}",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = JetBrainsMonoFontFamily,
                            color = Color(0xFF06B6D4)
                        )
                    }
                }
                IconButton(onClick = onDelete, modifier = Modifier.testTag("delete_site_button")) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "حذف الموقع",
                        tint = Color(0xFFEF4444)
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "HTML" to "index.html",
                    "CSS" to "styles.css",
                    "JS" to "app.js",
                    "PREVIEW" to "معاينة حية",
                    "AUDIT" to "فحص SEO والأمان"
                ).forEach { (key, title) ->
                    FilterChip(
                        selected = activeFileTab == key,
                        onClick = { activeFileTab = key },
                        label = { Text(title) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF06B6D4).copy(alpha = 0.22f),
                            selectedLabelColor = Color(0xFF06B6D4)
                        )
                    )
                }
            }
        }

        when (activeFileTab) {
            "PREVIEW" -> {
                item {
                    val previewBundle = remember(htmlCode, cssCode, jsCode) {
                        compilePreviewHtml(
                            site.copy(
                                htmlContent = htmlCode,
                                cssContent = cssCode,
                                jsContent = jsCode
                            )
                        )
                    }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(340.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0xFF06B6D4))
                    ) {
                        AndroidView(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(14.dp)),
                            factory = { ctx ->
                                WebView(ctx).apply {
                                    settings.javaScriptEnabled = true
                                    settings.domStorageEnabled = true
                                    webViewClient = WebViewClient()
                                }
                            },
                            update = { webView ->
                                webView.loadDataWithBaseURL(
                                    "https://${site.subdomainSlug}.vercel.app/",
                                    previewBundle,
                                    "text/html",
                                    "UTF-8",
                                    null
                                )
                            }
                        )
                    }
                }
            }

            "AUDIT" -> {
                item {
                    val auditReport = remember(htmlCode, cssCode, jsCode) {
                        auditWebsiteCode(htmlCode, cssCode, jsCode)
                    }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
                        border = BorderStroke(1.dp, Color(0xFF06B6D4))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF06B6D4))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "تقرير جودة الكود والأمان والـ SEO",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color(0xFFF9FAFB)
                                    )
                                }
                                if (auditReport != null) {
                                    Surface(
                                        color = Color(0xFF10B981).copy(alpha = 0.18f),
                                        shape = RoundedCornerShape(50),
                                        border = BorderStroke(1.dp, Color(0xFF10B981))
                                    ) {
                                        Text(
                                            text = "النتيجة: ${auditReport.score} / 100",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = Color(0xFF10B981),
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }

                            if (auditReport != null) {
                                Text(
                                    text = "إجمالي حجم الحزمة (HTML + CSS + JS): ${auditReport.totalBytes} Bytes",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    color = Color(0xFF06B6D4)
                                )
                                auditReport.passedChecks.forEach { check ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF10B981),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = check,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFFD1D5DB)
                                        )
                                    }
                                }
                                auditReport.warnings.forEach { warn ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = Color(0xFFF59E0B),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = warn,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFFFDE68A)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            else -> {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
                        border = BorderStroke(1.dp, Color(0xFF1F2937))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "استيراد ملفات من جهازك أو سحب كود HTML حي من أي رابط موقع:",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF06B6D4)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = {
                                    filePickerLauncher.launch(arrayOf("text/*", "application/javascript", "application/xhtml+xml"))
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("upload_local_file_btn"),
                                border = BorderStroke(1.dp, Color(0xFF06B6D4)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF06B6D4)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("رفع ملف $activeFileTab من جهازك مباشر إلى الخادم")
                            }

                            if (activeFileTab == "HTML") {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = importUrlInput,
                                        onValueChange = { importUrlInput = it },
                                        label = { Text("رابط موقع لاستيراد كوده الحي (https://...)") },
                                        placeholder = { Text("https://example.com") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        colors = darkTextFieldColors()
                                    )
                                    OutlinedButton(
                                        onClick = {
                                            onFetchLiveUrlHtml(importUrlInput) { fetchedHtml ->
                                                htmlCode = fetchedHtml
                                            }
                                        },
                                        enabled = !isBusy && importUrlInput.isNotBlank(),
                                        border = BorderStroke(1.dp, Color(0xFF10B981)),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF10B981)),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("سحب")
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    val currentValue = when (activeFileTab) {
                        "CSS" -> cssCode
                        "JS" -> jsCode
                        else -> htmlCode
                    }
                    OutlinedTextField(
                        value = currentValue,
                        onValueChange = { updated ->
                            when (activeFileTab) {
                                "CSS" -> cssCode = updated
                                "JS" -> jsCode = updated
                                else -> htmlCode = updated
                            }
                        },
                        label = { Text("محرر الكود المصدري ($activeFileTab)") },
                        textStyle = TextStyle(
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 13.sp,
                            color = Color(0xFFF9FAFB)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .testTag("code_editor_input"),
                        colors = darkTextFieldColors()
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
                border = BorderStroke(1.dp, Color(0xFF1F2937))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "بوابة النشر على النطاق العام (Vercel / Netlify / GitHub)",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFFF9FAFB)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "vercel_edge" to "Vercel Edge (.vercel.app)",
                            "netlify_cdn" to "Netlify CDN (.netlify.app)",
                            "github_pages" to "GitHub Pages (.github.io)",
                            "paste_gateway" to "Cloud Gateway (فوري)"
                        ).forEach { (provKey, provLabel) ->
                            FilterChip(
                                selected = provider == provKey,
                                onClick = { provider = provKey },
                                label = { Text(provLabel) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF10B981).copy(alpha = 0.2f),
                                    selectedLabelColor = Color(0xFF10B981)
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "سجل البناء الأخير:\n${site.lastBuildLog}",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = JetBrainsMonoFontFamily,
                        color = Color(0xFF9CA3AF)
                    )

                    if (site.liveDomainUrl.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(site.liveDomainUrl))
                                context.startActivity(intent)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(1.dp, Color(0xFF10B981)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF10B981)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("فتح الرابط المنشور الحي: ${site.liveDomainUrl}")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { onDeploy(htmlCode, cssCode, jsCode, provider) },
                        enabled = !isBusy,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("build_and_deploy_site_btn"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF10B981),
                            contentColor = Color(0xFF0B0F19)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isBusy) "جاري البناء والرفع على النطاق الحي..." else "بناء الحزمة ورفع الموقع الآن على النطاق الحقيقي",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
