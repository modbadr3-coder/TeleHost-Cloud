package com.example.ui.screens

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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.DnsMultiRecordReport
import com.example.ui.theme.JetBrainsMonoFontFamily

@Composable
fun DomainsDnsTabScreen(
    dnsReports: List<DnsMultiRecordReport>,
    isBusy: Boolean,
    onCheckDomain: (String) -> Unit,
    onClaimDomainForNewSite: (String) -> Unit
) {
    var domainQuery by rememberSaveable { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("domains_tab_list"),
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
                            imageVector = Icons.Default.Dns,
                            contentDescription = null,
                            tint = Color(0xFF06B6D4),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "مختبر فحص النطاقات الشامل (DNS Records + SSL + Latency)",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color(0xFFF9FAFB)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "يفحص سجلات A, AAAA, CNAME, MX, TXT, NS مباشرة عبر Google DNS-over-HTTPS، ويقيس زمن استجابة HTTPS وشهادة الأمان SSL للنطاقات النشطة أو يكتشف النطاقات غير المستخدمة (NXDOMAIN).",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF9CA3AF)
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = domainQuery,
                        onValueChange = { domainQuery = it },
                        label = { Text("اكتب اسم المشروع أو النطاق الكامل (مثال: my-arab-cloud أو google.com)") },
                        placeholder = { Text("telehost-portal-2026") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("domain_query_input"),
                        colors = darkTextFieldColors()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { onCheckDomain(domainQuery) },
                        enabled = !isBusy && domainQuery.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("verify_dns_btn"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF06B6D4),
                            contentColor = Color(0xFF0B0F19)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isBusy) "جاري فحص سجلات DNS وشهادة SSL..." else "تشغيل الفحص الشامل للنطاق الآن",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "تقارير الفحص الشاملة (${dnsReports.size})",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFFF9FAFB)
            )
        }

        if (dnsReports.isEmpty()) {
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
                            text = "أدخل أي اسم مشروع أو نطاق أعلاه لفحص سجلات A, AAAA, CNAME, MX, TXT, NS وشهادة SSL.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF9CA3AF)
                        )
                    }
                }
            }
        } else {
            items(dnsReports, key = { it.domain + it.checkedAtMillis }) { report ->
                FullDnsReportCard(
                    report = report,
                    onClaim = {
                        val slug = report.domain.substringBefore(".")
                        onClaimDomainForNewSite(slug)
                    }
                )
            }
        }
    }
}

@Composable
private fun FullDnsReportCard(
    report: DnsMultiRecordReport,
    onClaim: () -> Unit
) {
    val isAvailable = report.isAvailableForClaim
    val accent = if (isAvailable) Color(0xFF10B981) else Color(0xFF06B6D4)
    val rcodeName = when (report.dnsStatusCode) {
        0 -> "NOERROR (0) - نطاق نشط"
        3 -> "NXDOMAIN (3) - غير مستخدم ومتاح فوراً"
        else -> "RCODE (${report.dnsStatusCode})"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("domain_result_${report.domain}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = if (isAvailable) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = report.domain,
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = JetBrainsMonoFontFamily,
                        color = Color(0xFFF9FAFB)
                    )
                }

                Surface(
                    color = accent.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, accent)
                ) {
                    Text(
                        text = if (isAvailable) "UNUSED / AVAILABLE" else "ACTIVE DOMAIN",
                        style = MaterialTheme.typography.labelMedium,
                        color = accent,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Text(
                text = "حالة DNS: $rcodeName",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = JetBrainsMonoFontFamily,
                color = Color(0xFFD1D5DB)
            )

            if (report.aRecords.isNotEmpty()) {
                DnsRecordLine("A (IPv4)", report.aRecords.joinToString(", "))
            }
            if (report.aaaaRecords.isNotEmpty()) {
                DnsRecordLine("AAAA (IPv6)", report.aaaaRecords.joinToString(", "))
            }
            if (report.cnameRecords.isNotEmpty()) {
                DnsRecordLine("CNAME", report.cnameRecords.joinToString(", "))
            }
            if (report.mxRecords.isNotEmpty()) {
                DnsRecordLine("MX (Mail)", report.mxRecords.joinToString(" | "))
            }
            if (report.nsRecords.isNotEmpty()) {
                DnsRecordLine("NS", report.nsRecords.joinToString(", "))
            }
            if (report.txtRecords.isNotEmpty()) {
                DnsRecordLine("TXT", report.txtRecords.take(2).joinToString(" | "))
            }

            if (report.geoCountry.isNotBlank() || report.geoIsp.isNotBlank()) {
                DnsRecordLine(
                    "Geo-IP Datacenter",
                    "${report.geoCountry} (${report.geoCity}) | ISP: ${report.geoIsp} ${report.geoAsn}".trim()
                )
            }

            if (report.httpProbeStatus > 0) {
                Surface(
                    color = Color(0xFF0B0F19),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFF1F2937)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "HTTPS Status: ${report.httpProbeStatus} | زمن الاستجابة: ${report.httpLatencyMs}ms",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF10B981)
                            )
                        }
                        if (report.httpServerHeader.isNotBlank()) {
                            Text(
                                text = "محرك الخادم (Server Header): ${report.httpServerHeader}",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = JetBrainsMonoFontFamily,
                                color = Color(0xFF06B6D4)
                            )
                        }
                        if (report.sslCertificateIssuer.isNotBlank()) {
                            Text(
                                text = "مُصدر شهادة SSL: ${report.sslCertificateIssuer} (صالحة حتى ${report.sslValidUntil})",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = JetBrainsMonoFontFamily,
                                color = Color(0xFF9CA3AF)
                            )
                        }
                    }
                }
            }

            if (isAvailable) {
                Spacer(modifier = Modifier.height(6.dp))
                Button(
                    onClick = onClaim,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("claim_domain_btn_${report.domain}"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF10B981),
                        contentColor = Color(0xFF0B0F19)
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.RocketLaunch, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("إنشاء مشروع موقع بهذا النطاق غير المستخدم فوراً", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun DnsRecordLine(type: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "$type: ",
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFF06B6D4)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = JetBrainsMonoFontFamily,
            color = Color(0xFF9CA3AF)
        )
    }
}
