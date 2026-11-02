package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class TelegramBot(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val username: String = "",
    val botToken: String = "",
    val status: String = "stopped", // "running", "stopped", "webhook_active"
    val webhookUrl: String = "",
    val autoReplyRulesJson: String = "[]",
    val welcomeMessage: String = "مرحبا بك في البوت السحابي الرسمي.",
    val processedUpdatesCount: Long = 0L,
    val lastUpdateId: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class BotAutoReplyRule(
    val command: String = "/help",
    val responseText: String = "قائمة الأوامر المتاحة في الخادم."
)

data class TelegramWebhookInfo(
    val url: String = "",
    val pendingUpdateCount: Int = 0,
    val lastErrorDate: Long = 0L,
    val lastErrorMessage: String = "",
    val maxConnections: Int = 40,
    val ipAddress: String = ""
)

data class TelegramChatInspection(
    val id: Long = 0L,
    val title: String = "",
    val type: String = "",
    val username: String = "",
    val description: String = "",
    val memberCount: Int = 0
)

data class SurgeHostedProject(
    val domain: String,
    val revCount: Int = 0,
    val lastUpdated: String = "",
    val platformInfo: String = "Surge CDN"
)

data class DnsMultiRecordReport(
    val domain: String,
    val isAvailableForClaim: Boolean,
    val dnsStatusCode: Int,
    val aRecords: List<String>,
    val aaaaRecords: List<String>,
    val cnameRecords: List<String>,
    val mxRecords: List<String>,
    val txtRecords: List<String>,
    val nsRecords: List<String>,
    val authorityRecord: String,
    val httpProbeStatus: Int = -1,
    val httpLatencyMs: Long = -1L,
    val sslCertificateIssuer: String = "",
    val sslValidUntil: String = "",
    val geoCountry: String = "",
    val geoCity: String = "",
    val geoIsp: String = "",
    val geoAsn: String = "",
    val httpServerHeader: String = "",
    val checkedAtMillis: Long = System.currentTimeMillis()
)

data class WebsiteCodeAuditReport(
    val score: Int,
    val totalBytes: Int,
    val passedChecks: List<String>,
    val warnings: List<String>
)

@IgnoreExtraProperties
data class WebSiteProject(
    @DocumentId val id: String = "",
    val userId: String = "",
    val projectName: String = "",
    val subdomainSlug: String = "",
    val provider: String = "vercel_edge", // "vercel_edge", "netlify_cdn", "github_pages", "paste_gateway"
    val liveDomainUrl: String = "",
    val htmlContent: String = DEFAULT_HTML_TEMPLATE,
    val cssContent: String = DEFAULT_CSS_TEMPLATE,
    val jsContent: String = DEFAULT_JS_TEMPLATE,
    val buildStatus: String = "draft", // "draft", "deployed", "failed"
    val lastBuildLog: String = "جاهز للبناء والرفع المباشر",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    companion object {
        const val DEFAULT_HTML_TEMPLATE = """<!DOCTYPE html>
<html lang="ar" dir="rtl">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>بوابة الخدمات الرقمية</title>
</head>
<body>
    <header class="hero">
        <div class="container">
            <span class="badge">استضافة سحابية حقيقية</span>
            <h1>مرحبا بك في موقعنا الرسمي</h1>
            <p>تم بناء هذا الموقع ونشره مباشرة عبر منصة TeleHost Cloud على نطاق عام.</p>
            <button id="actionBtn" class="btn-primary">تواصل معنا الآن</button>
            <div id="outputBox" class="status-box">الحالة: متصل بالخادم بنجاح</div>
        </div>
    </header>
</body>
</html>"""

        const val DEFAULT_CSS_TEMPLATE = """* {
    box-sizing: border-box;
    margin: 0;
    padding: 0;
    font-family: system-ui, -apple-system, sans-serif;
}
body {
    background-color: #0B0F19;
    color: #F9FAFB;
    min-height: 100vh;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 24px;
}
.hero {
    max-width: 680px;
    width: 100%;
    background: #111827;
    border: 1px solid #1F2937;
    border-radius: 16px;
    padding: 40px 32px;
    text-align: center;
    box-shadow: 0 20px 50px rgba(0, 0, 0, 0.5);
}
.badge {
    display: inline-block;
    background: rgba(6, 182, 212, 0.15);
    color: #06B6D4;
    border: 1px solid #06B6D4;
    padding: 6px 14px;
    border-radius: 999px;
    font-size: 13px;
    font-weight: 700;
    margin-bottom: 16px;
}
h1 {
    font-size: 32px;
    margin-bottom: 14px;
    color: #FFFFFF;
}
p {
    color: #9CA3AF;
    line-height: 1.7;
    margin-bottom: 28px;
}
.btn-primary {
    background: #06B6D4;
    color: #0B0F19;
    border: none;
    padding: 14px 28px;
    border-radius: 10px;
    font-size: 16px;
    font-weight: 700;
    cursor: pointer;
}
.status-box {
    margin-top: 24px;
    padding: 12px;
    background: #0B0F19;
    border: 1px solid #10B981;
    color: #10B981;
    border-radius: 8px;
    font-family: monospace;
    font-size: 14px;
}"""

        const val DEFAULT_JS_TEMPLATE = """document.addEventListener('DOMContentLoaded', function() {
    var btn = document.getElementById('actionBtn');
    var box = document.getElementById('outputBox');
    if (btn && box) {
        btn.addEventListener('click', function() {
            var now = new Date().toISOString();
            box.textContent = 'تم استلام الطلب بنجاح في التوقيت: ' + now;
        });
    }
});"""

        // Template 2: E-Commerce & Digital Services Storefront
        const val ECOMMERCE_HTML_TEMPLATE = """<!DOCTYPE html>
<html lang="ar" dir="rtl">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>متجر الحلول الرقمية</title>
</head>
<body>
    <main class="store-shell">
        <header class="store-header">
            <span class="badge">متجر إلكتروني متكامل</span>
            <h1>باقات الاستضافة والحلول السحابية</h1>
            <p>اختر الباقة المناسبة لمشروعك مع تفعيل فوري ودعم فني على مدار الساعة.</p>
        </header>
        <section class="grid">
            <article class="plan-card">
                <h3>باقة البوتات القياسية</h3>
                <div class="price">19$ / شهرياً</div>
                <p>تشغيل مستمر مع Webhook سريع وسجلات فورية.</p>
                <button class="buy-btn" data-plan="باقة البوتات القياسية">اطلب الباقة الآن</button>
            </article>
            <article class="plan-card featured">
                <h3>الباقة المؤسسية الشاملة</h3>
                <div class="price">49$ / شهرياً</div>
                <p>بوتات غير محدودة + استضافة مواقع مع شهادة SSL ونطاق مخصص.</p>
                <button class="buy-btn" data-plan="الباقة المؤسسية الشاملة">اطلب الباقة الآن</button>
            </article>
        </section>
        <div id="orderStatus" class="status-box">اختر أي باقة أعلاه لتأكيد الطلب الفوري</div>
    </main>
</body>
</html>"""

        const val ECOMMERCE_CSS_TEMPLATE = """* { box-sizing: border-box; margin: 0; padding: 0; font-family: system-ui, sans-serif; }
body { background: #0B0F19; color: #F9FAFB; padding: 28px; }
.store-shell { max-width: 820px; margin: 0 auto; }
.store-header { text-align: center; margin-bottom: 32px; }
.badge { background: rgba(16,185,129,0.15); color: #10B981; border: 1px solid #10B981; padding: 5px 14px; border-radius: 99px; font-size: 13px; display: inline-block; margin-bottom: 12px; }
.grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(260px, 1fr)); gap: 18px; }
.plan-card { background: #111827; border: 1px solid #1F2937; border-radius: 16px; padding: 24px; text-align: center; }
.plan-card.featured { border-color: #06B6D4; }
.price { font-size: 28px; font-weight: 800; color: #06B6D4; margin: 14px 0; }
.plan-card p { color: #9CA3AF; font-size: 14px; margin-bottom: 20px; line-height: 1.6; }
.buy-btn { width: 100%; background: #10B981; color: #0B0F19; border: none; padding: 12px; border-radius: 10px; font-weight: 700; cursor: pointer; }
.status-box { margin-top: 24px; padding: 14px; background: #111827; border: 1px solid #06B6D4; color: #06B6D4; border-radius: 10px; text-align: center; font-family: monospace; }"""

        const val ECOMMERCE_JS_TEMPLATE = """document.querySelectorAll('.buy-btn').forEach(function(btn) {
    btn.addEventListener('click', function() {
        var plan = btn.getAttribute('data-plan');
        var box = document.getElementById('orderStatus');
        if (box) {
            box.textContent = 'تم تسجيل طلب الاشتراك في: ' + plan + ' بنجاح!';
        }
    });
});"""

        // Template 3: Real-time System Status & API Docs Portal
        const val STATUS_PORTAL_HTML_TEMPLATE = """<!DOCTYPE html>
<html lang="ar" dir="rtl">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>لوحة حالة الخوادم والـ API</title>
</head>
<body>
    <div class="status-container">
        <div class="header-bar">
            <h2>جميع الأنظمة السحابية تعمل بكفاءة 100%</h2>
            <button id="refreshPing" class="refresh-btn">فحص زمن الاستجابة</button>
        </div>
        <div class="service-list">
            <div class="service-row"><span>عنقود بوتات التلجرام (Telegram API Node)</span><strong class="ok">ONLINE - 24ms</strong></div>
            <div class="service-row"><span>شبكة توزيع المحتوى (Surge & GitHub CDN)</span><strong class="ok">ONLINE - 18ms</strong></div>
            <div class="service-row"><span>خوادم الأسماء (Google DoH DNS)</span><strong class="ok">ONLINE - 12ms</strong></div>
        </div>
        <p id="pingReport" class="footer-note">آخر تحديث فوري من الخادم</p>
    </div>
</body>
</html>"""

        const val STATUS_PORTAL_CSS_TEMPLATE = """* { box-sizing: border-box; margin: 0; padding: 0; font-family: system-ui, sans-serif; }
body { background: #0B0F19; color: #F9FAFB; padding: 28px; display: flex; justify-content: center; }
.status-container { max-width: 680px; width: 100%; background: #111827; border: 1px solid #1F2937; border-radius: 16px; padding: 28px; }
.header-bar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; gap: 12px; flex-wrap: wrap; }
.header-bar h2 { font-size: 20px; color: #10B981; }
.refresh-btn { background: #06B6D4; color: #0B0F19; border: none; padding: 10px 18px; border-radius: 8px; font-weight: 700; cursor: pointer; }
.service-list { display: flex; flex-direction: column; gap: 12px; }
.service-row { background: #0B0F19; border: 1px solid #1F2937; padding: 16px; border-radius: 10px; display: flex; justify-content: space-between; align-items: center; }
.ok { color: #10B981; font-family: monospace; }
.footer-note { margin-top: 20px; color: #9CA3AF; font-size: 13px; text-align: center; font-family: monospace; }"""

        const val STATUS_PORTAL_JS_TEMPLATE = """document.getElementById('refreshPing')?.addEventListener('click', function() {
    var el = document.getElementById('pingReport');
    if (el) {
        el.textContent = 'تم فحص جميع العقد السحابية بنجاح في: ' + new Date().toLocaleTimeString();
    }
});"""

        const val PORTFOLIO_HTML_TEMPLATE = """<!DOCTYPE html>
<html lang="ar" dir="rtl">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="description" content="البورتفوليو الرسمي للمطور وأنظمة الخوادم السحابية">
    <title>بورتفوليو المطور الرسمي</title>
</head>
<body>
    <main class="portfolio-card">
        <div class="tag">FULL-STACK & TELEGRAM CLOUD ARCHITECT</div>
        <h1>مرحبا، أنا مطور أنظمة سحابية وبوتات تلجرام</h1>
        <p class="bio">تصميم وتطوير البنية التحتية للخوادم، بوتات التلجرام فائقة السرعة، وتطبيقات الويب الحديثة.</p>
        <div class="skills">
            <span>Kotlin</span>
            <span>Telegram Bot API</span>
            <span>Cloud Firestore</span>
            <span>Edge CDN</span>
        </div>
        <div class="actions">
            <a href="https://t.me/SOLO_dev1" target="_blank" class="tg-btn">تواصل عبر تلجرام (@SOLO_dev1)</a>
            <button id="copyEmailBtn" class="sec-btn">عرض حالة التفرغ للمشاريع</button>
        </div>
        <p id="availabilityMsg" class="avail"></p>
    </main>
</body>
</html>"""

        const val PORTFOLIO_CSS_TEMPLATE = """* { box-sizing: border-box; margin: 0; padding: 0; font-family: system-ui, sans-serif; }
body { background: #0B0F19; color: #F9FAFB; min-height: 100vh; display: flex; align-items: center; justify-content: center; padding: 24px; }
.portfolio-card { max-width: 660px; width: 100%; background: #111827; border: 1px solid #06B6D4; border-radius: 18px; padding: 36px; text-align: center; }
.tag { display: inline-block; background: rgba(6, 182, 212, 0.16); color: #06B6D4; padding: 6px 14px; border-radius: 50px; font-size: 12px; font-weight: 700; margin-bottom: 16px; }
h1 { font-size: 26px; margin-bottom: 12px; }
.bio { color: #9CA3AF; line-height: 1.7; margin-bottom: 20px; }
.skills { display: flex; flex-wrap: wrap; gap: 8px; justify-content: center; margin-bottom: 24px; }
.skills span { background: #1F2937; color: #10B981; padding: 6px 14px; border-radius: 8px; font-size: 13px; font-family: monospace; }
.actions { display: flex; flex-wrap: wrap; gap: 12px; justify-content: center; }
.tg-btn { background: linear-gradient(90deg, #FF0055, #00B0FF, #00E676); color: #FFFFFF; text-decoration: none; padding: 12px 24px; border-radius: 10px; font-weight: 700; }
.sec-btn { background: #1F2937; color: #F9FAFB; border: 1px solid #374151; padding: 12px 20px; border-radius: 10px; cursor: pointer; font-weight: 600; }
.avail { margin-top: 16px; color: #10B981; font-weight: 700; }"""

        const val PORTFOLIO_JS_TEMPLATE = """document.getElementById('copyEmailBtn')?.addEventListener('click', function() {
    var msg = document.getElementById('availabilityMsg');
    if (msg) {
        msg.textContent = 'متاح حالياً لاستقبال مشاريع البوتات والمواقع السحابية عبر @SOLO_dev1';
    }
});"""
    }
}

@IgnoreExtraProperties
data class DeploymentLog(
    @DocumentId val id: String = "",
    val userId: String = "",
    val targetName: String = "",
    val category: String = "bot_engine", // "bot_engine", "site_deploy", "domain_dns"
    val message: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

data class DomainCheckResult(
    val domain: String,
    val isAvailableForClaim: Boolean,
    val dnsStatusCode: Int,
    val resolvedIps: List<String>,
    val authorityRecord: String,
    val checkedAtMillis: Long = System.currentTimeMillis()
)
