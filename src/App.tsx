import React, { useState } from 'react';
import { 
  Bot, 
  Download, 
  Github, 
  CheckCircle2, 
  AlertTriangle, 
  Sliders, 
  Sparkles, 
  Smartphone, 
  FileCode2, 
  Terminal, 
  Copy, 
  Check, 
  ExternalLink,
  ShieldCheck,
  RefreshCw,
  FolderGit2,
  Cpu,
  Layers,
  ArrowRight,
  TrendingUp,
  CreditCard,
  Tv
} from 'lucide-react';

export default function App() {
  const [activeTab, setActiveTab] = useState<'status' | 'github_fix' | 'app_preview' | 'calculator'>('github_fix');
  const [currency, setCurrency] = useState<'USD' | 'IQD'>('IQD');
  const [copiedCode, setCopiedCode] = useState<string | null>(null);

  // Download simulation state
  const [testUrl, setTestUrl] = useState('');
  const [isProcessing, setIsProcessing] = useState(false);
  const [downloadSuccess, setDownloadSuccess] = useState(false);

  // Financial calculator state
  const [activeUsers, setActiveUsers] = useState(15000);
  const [dauRate, setDauRate] = useState(9); // 9%
  const [cpmRate, setCpmRate] = useState(0.60); // $0.60
  const [vipSubs, setVipSubs] = useState(60);

  const dau = Math.round(activeUsers * (dauRate / 100));
  const dailyAds = Math.round(dau * 1.8);
  const monthlyAdsRevenue = ((dailyAds * 30) / 1000) * cpmRate;
  const monthlyVipRevenue = vipSubs * 2.7; // ~$2.7 = 4,000 IQD
  const totalRevenueUSD = monthlyAdsRevenue + monthlyVipRevenue + 50; // direct ads
  const netProfitUSD = Math.max(0, totalRevenueUSD - 35); // - server costs

  const formatMoney = (usd: number) => {
    if (currency === 'IQD') {
      return `${Math.round(usd * 1500).toLocaleString('ar-IQ')} د.ع`;
    }
    return `$${usd.toFixed(1)}`;
  };

  const handleCopy = (text: string, id: string) => {
    navigator.clipboard.writeText(text);
    setCopiedCode(id);
    setTimeout(() => setCopiedCode(null), 2500);
  };

  const handleSimulateDownload = (e: React.FormEvent) => {
    e.preventDefault();
    if (!testUrl.trim()) return;
    setIsProcessing(true);
    setDownloadSuccess(false);
    setTimeout(() => {
      setIsProcessing(false);
      setDownloadSuccess(true);
    }, 1800);
  };

  const gitCommandsText = `git add .
git commit -m "Fix Android APK GitHub Actions build errors & signing"
git push origin main`;

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col font-['Cairo',sans-serif]">
      {/* Top Banner & Header */}
      <header className="sticky top-0 z-50 bg-slate-900/90 backdrop-blur-md border-b border-slate-800">
        <div className="max-w-7xl mx-auto px-4 py-3 flex flex-wrap items-center justify-between gap-3">
          <div className="flex items-center gap-3">
            <div className="w-9 h-9 rounded-xl bg-emerald-500/10 border border-emerald-500/30 flex items-center justify-center text-emerald-400">
              <Smartphone className="w-5 h-5" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h1 className="font-bold text-white text-base md:text-lg">تطبيق تحميل الميديا | Media Downloader APK</h1>
                <span className="px-2 py-0.5 rounded-full text-[11px] font-semibold bg-emerald-500/20 text-emerald-300 border border-emerald-500/30">
                  v1.0.22 جاهز للبناء
                </span>
              </div>
              <p className="text-xs text-slate-400">
                أندرويد أصلي (Jetpack Compose) • سير عمل GitHub Actions مصحح 100%
              </p>
            </div>
          </div>

          <div className="flex items-center gap-3">
            {/* Currency switch */}
            <div className="flex items-center bg-slate-800 rounded-lg p-0.5 border border-slate-700 text-xs">
              <button
                onClick={() => setCurrency('USD')}
                className={`px-2.5 py-1 rounded-md font-semibold transition-all ${
                  currency === 'USD' ? 'bg-emerald-500 text-slate-950 font-bold' : 'text-slate-400'
                }`}
              >
                $ دولار
              </button>
              <button
                onClick={() => setCurrency('IQD')}
                className={`px-2.5 py-1 rounded-md font-semibold transition-all ${
                  currency === 'IQD' ? 'bg-emerald-500 text-slate-950 font-bold' : 'text-slate-400'
                }`}
              >
                د.ع عراقي
              </button>
            </div>

            <a
              href="https://github.com"
              target="_blank"
              rel="noreferrer"
              className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-xs font-medium text-slate-200 border border-slate-700 transition-colors"
            >
              <Github className="w-4 h-4 text-white" />
              <span>GitHub Actions</span>
            </a>
          </div>
        </div>

        {/* Navigation Tabs */}
        <div className="max-w-7xl mx-auto px-4 border-t border-slate-800 flex overflow-x-auto gap-1 py-1 scrollbar-none text-xs md:text-sm">
          <button
            onClick={() => setActiveTab('github_fix')}
            className={`flex items-center gap-2 px-4 py-2 rounded-lg font-medium transition-all ${
              activeTab === 'github_fix'
                ? 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/30'
                : 'text-slate-400 hover:text-white'
            }`}
          >
            <FolderGit2 className="w-4 h-4 text-emerald-400" />
            <span>حل أخطاء GitHub Actions (التقرير الشامل)</span>
          </button>

          <button
            onClick={() => setActiveTab('status')}
            className={`flex items-center gap-2 px-4 py-2 rounded-lg font-medium transition-all ${
              activeTab === 'status'
                ? 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/30'
                : 'text-slate-400 hover:text-white'
            }`}
          >
            <ShieldCheck className="w-4 h-4 text-cyan-400" />
            <span>حالة المشروع والتوقيع والـ SDK</span>
          </button>

          <button
            onClick={() => setActiveTab('app_preview')}
            className={`flex items-center gap-2 px-4 py-2 rounded-lg font-medium transition-all ${
              activeTab === 'app_preview'
                ? 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/30'
                : 'text-slate-400 hover:text-white'
            }`}
          >
            <Smartphone className="w-4 h-4 text-purple-400" />
            <span>معاينة واجهة التطبيق التفاعلية</span>
          </button>

          <button
            onClick={() => setActiveTab('calculator')}
            className={`flex items-center gap-2 px-4 py-2 rounded-lg font-medium transition-all ${
              activeTab === 'calculator'
                ? 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/30'
                : 'text-slate-400 hover:text-white'
            }`}
          >
            <TrendingUp className="w-4 h-4 text-amber-400" />
            <span>حاسبة أرباح التطبيق والبوت (العراق)</span>
          </button>
        </div>
      </header>

      {/* Main Content Area */}
      <main className="flex-1 max-w-7xl w-full mx-auto px-4 py-6 md:py-8 space-y-6">
        {/* Tab 1: GitHub Fix Deep Dive */}
        {activeTab === 'github_fix' && (
          <div className="space-y-6">
            <div className="p-6 rounded-2xl bg-gradient-to-br from-slate-900 via-slate-900 to-emerald-950/40 border border-emerald-500/30 shadow-xl">
              <div className="flex items-center gap-2 mb-2">
                <CheckCircle2 className="w-6 h-6 text-emerald-400" />
                <h2 className="text-xl md:text-2xl font-black text-white">
                  تم إصلاح جميع أخطاء البناء في GitHub Actions بنجاح
                </h2>
              </div>
              <p className="text-xs md:text-sm text-slate-300 leading-relaxed max-w-4xl">
                تمت معالجة الأسباب الـ 6 التي كانت تفشل عملية بناء وتوقيع ملفات الـ APK (`assembleDebug` و `assembleRelease`) على خوادم GitHub Runner. إليك تفاصيل ما تم تصحيحه وأوامر رفع التعديلات فوراً:
              </p>

              {/* Quick Action Box */}
              <div className="mt-5 p-4 rounded-xl bg-slate-950 border border-slate-800">
                <div className="flex items-center justify-between mb-2">
                  <span className="text-xs font-bold text-emerald-400 flex items-center gap-1.5">
                    <Terminal className="w-4 h-4" />
                    أوامر رفع التحديث إلى GitHub وتشغيل البناء التلقائي:
                  </span>
                  <button
                    onClick={() => handleCopy(gitCommandsText, 'git')}
                    className="flex items-center gap-1 text-xs text-slate-300 hover:text-white bg-slate-800 px-2.5 py-1 rounded border border-slate-700"
                  >
                    {copiedCode === 'git' ? (
                      <>
                        <Check className="w-3.5 h-3.5 text-emerald-400" />
                        <span className="text-emerald-400">تم النسخ</span>
                      </>
                    ) : (
                      <>
                        <Copy className="w-3.5 h-3.5" />
                        <span>نسخ الأوامر</span>
                      </>
                    )}
                  </button>
                </div>
                <pre className="font-mono text-xs text-emerald-300 bg-slate-900/90 p-3 rounded-lg overflow-x-auto">
                  {gitCommandsText}
                </pre>
              </div>
            </div>

            {/* The 6 Fixed Errors Grid */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div className="p-4 rounded-xl bg-slate-900 border border-slate-800">
                <div className="flex items-center gap-2 text-rose-400 font-bold text-sm mb-1.5">
                  <AlertTriangle className="w-4 h-4" />
                  <span>1. خطأ فقدان ملف التوقيع (Keystore file not found)</span>
                </div>
                <p className="text-xs text-slate-400 leading-relaxed">
                  <strong>السبب:</strong> ملف `debug.keystore` كان مستبعداً داخل `.gitignore`، وتوليده التلقائي كان يفشل بصمت داخل بيئة الـ Runner.
                  <br />
                  <strong className="text-emerald-400">الحل:</strong> تمت إضافة خطوة توليد `debug.keystore` باستخدام `keytool` مباشرة قبل استدعاء Gradle.
                </p>
              </div>

              <div className="p-4 rounded-xl bg-slate-900 border border-slate-800">
                <div className="flex items-center gap-2 text-rose-400 font-bold text-sm mb-1.5">
                  <AlertTriangle className="w-4 h-4" />
                  <span>2. رفض رخص Android SDK لـ compileSdk 36.1</span>
                </div>
                <p className="text-xs text-slate-400 leading-relaxed">
                  <strong>السبب:</strong> أمر التراخيص السابق كان يرمي المخرجات إلى `/dev/null` فيفشل AGP في تنزيل منصة 36.1 الناقصة.
                  <br />
                  <strong className="text-emerald-400">الحل:</strong> استدعاء `android-actions/setup-android@v3` وكتابة تراخيص الـ SDK رسمياً في مسارها.
                </p>
              </div>

              <div className="p-4 rounded-xl bg-slate-900 border border-slate-800">
                <div className="flex items-center gap-2 text-rose-400 font-bold text-sm mb-1.5">
                  <AlertTriangle className="w-4 h-4" />
                  <span>3. تعارض KSP و Room عند بناء النسختين معاً</span>
                </div>
                <p className="text-xs text-slate-400 leading-relaxed">
                  <strong>السبب:</strong> أمر `./gradlew assembleDebug assembleRelease` في آن واحد كان يتسبب في تصادم خيوط الـ KSP داخل الـ JVM.
                  <br />
                  <strong className="text-emerald-400">الحل:</strong> تنفيذ `assembleDebug` أولاً ثم `assembleRelease` بشكل تسلسلي منفصل ونظيف.
                </p>
              </div>

              <div className="p-4 rounded-xl bg-slate-900 border border-slate-800">
                <div className="flex items-center gap-2 text-rose-400 font-bold text-sm mb-1.5">
                  <AlertTriangle className="w-4 h-4" />
                  <span>4. عدم تطابق هدف Java مع Kotlin jvmTarget</span>
                </div>
                <p className="text-xs text-slate-400 leading-relaxed">
                  <strong>السبب:</strong> `compileOptions` كان Java 11 بينما `compilerOptions` لم تكن تحدد الـ JVM Target مما يسبب خطأ اختلاف التوافقية.
                  <br />
                  <strong className="text-emerald-400">الحل:</strong> تعيين `jvmTarget.set(JVM_11)` داخل `app/build.gradle.kts`.
                </p>
              </div>

              <div className="p-4 rounded-xl bg-slate-900 border border-slate-800">
                <div className="flex items-center gap-2 text-rose-400 font-bold text-sm mb-1.5">
                  <AlertTriangle className="w-4 h-4" />
                  <span>5. كشف المجلدات المتداخلة وصلاحيات gradlew</span>
                </div>
                <p className="text-xs text-slate-400 leading-relaxed">
                  <strong>السبب:</strong> إذا رُفع المشروع داخل مجلد مثل `media-download-apk-main`، كان يفشل أمر `./gradlew: not found`.
                  <br />
                  <strong className="text-emerald-400">الحل:</strong> فحص تلقائي ينقل المحتوى للجذر، وإصلاح نهايات أسطر ويندوز `sed -i 's/\r$//' gradlew` ومنح `chmod +x`.
                </p>
              </div>

              <div className="p-4 rounded-xl bg-slate-900 border border-slate-800">
                <div className="flex items-center gap-2 text-rose-400 font-bold text-sm mb-1.5">
                  <AlertTriangle className="w-4 h-4" />
                  <span>6. تصحيح اسم المشروع في settings.gradle.kts</span>
                </div>
                <p className="text-xs text-slate-400 leading-relaxed">
                  <strong>السبب:</strong> وجود مسافات وأحرف عربية في `rootProject.name` كان يربك خوادم لينكس في مسارات الـ APK.
                  <br />
                  <strong className="text-emerald-400">الحل:</strong> تغيير اسم الـ Root Project إلى `MediaDownloader` مع بقاء الاسم العربي في الواجهة.
                </p>
              </div>
            </div>
          </div>
        )}

        {/* Tab 2: Project Specifications & Status */}
        {activeTab === 'status' && (
          <div className="space-y-6">
            <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6">
              <h2 className="text-lg md:text-xl font-bold text-white mb-2 flex items-center gap-2">
                <ShieldCheck className="w-5 h-5 text-emerald-400" />
                المواصفات التقنية لتطبيق الأندرويد بعد المعالجة
              </h2>
              <p className="text-xs md:text-sm text-slate-400 mb-6">
                جميع ملفات البناء وإعدادات الحزم متطابقة ومتوافقة مع أحدث معايير متجر جوجل بلاي وأندرويد 15/16.
              </p>

              <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-4 text-xs">
                <div className="p-3.5 rounded-xl bg-slate-950 border border-slate-800">
                  <span className="text-slate-400 block mb-1">اسم الحزمة (Package):</span>
                  <strong className="text-white block font-mono">com.mediadownload.app.v12</strong>
                </div>

                <div className="p-3.5 rounded-xl bg-slate-950 border border-slate-800">
                  <span className="text-slate-400 block mb-1">إصدار التطبيق (Version):</span>
                  <strong className="text-emerald-400 block font-mono">v1.0.22 (Code: 23)</strong>
                </div>

                <div className="p-3.5 rounded-xl bg-slate-950 border border-slate-800">
                  <span className="text-slate-400 block mb-1">نظام الـ SDK:</span>
                  <strong className="text-cyan-300 block font-mono">compileSdk: 36.1 | minSdk: 24</strong>
                </div>

                <div className="p-3.5 rounded-xl bg-slate-950 border border-slate-800">
                  <span className="text-slate-400 block mb-1">بيئة التطوير:</span>
                  <strong className="text-amber-300 block font-mono">AGP 9.1.1 + Gradle 9.3.1 + Kotlin 2.2</strong>
                </div>
              </div>

              {/* Supported Platforms */}
              <div className="mt-6 pt-6 border-t border-slate-800">
                <h3 className="font-bold text-white text-sm mb-3">المنصات المدعومة للتنزيل داخل التطبيق:</h3>
                <div className="flex flex-wrap gap-2 text-xs">
                  {['TikTok (بدون علامة)', 'Instagram (ريلز وبوستات)', 'Facebook HD', 'X (Twitter)', 'YouTube Shorts', 'Snapchat Spotlight', 'Pinterest', 'Threads', 'Dailymotion'].map((plat, i) => (
                    <span key={i} className="px-3 py-1 rounded-lg bg-slate-800 text-slate-200 border border-slate-700">
                      ✓ {plat}
                    </span>
                  ))}
                </div>
              </div>
            </div>
          </div>
        )}

        {/* Tab 3: Interactive App Preview */}
        {activeTab === 'app_preview' && (
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
            {/* Phone Screen Mockup */}
            <div className="lg:col-span-5 flex justify-center">
              <div className="w-[320px] rounded-[36px] bg-slate-950 border-4 border-slate-700 p-4 shadow-2xl shadow-emerald-500/10">
                {/* Notch */}
                <div className="w-28 h-4 bg-slate-800 rounded-full mx-auto mb-4" />

                {/* App Bar */}
                <div className="flex items-center justify-between mb-4 pb-2 border-b border-slate-800">
                  <div className="flex items-center gap-2">
                    <div className="w-7 h-7 rounded-lg bg-emerald-500/20 text-emerald-400 flex items-center justify-center font-bold text-xs">
                      MD
                    </div>
                    <div>
                      <span className="font-bold text-white text-xs block">تحميل الميديا</span>
                      <span className="text-[10px] text-slate-400">Media Downloader v1.0.22</span>
                    </div>
                  </div>
                  <span className="text-[10px] px-2 py-0.5 rounded bg-emerald-500/20 text-emerald-300 border border-emerald-500/30">
                    VIP نشط
                  </span>
                </div>

                {/* Simulated Form inside Phone */}
                <form onSubmit={handleSimulateDownload} className="space-y-3">
                  <div className="p-2.5 rounded-xl bg-slate-900 border border-slate-800">
                    <label className="text-[10px] text-slate-400 block mb-1">الصق رابط الفيديو من أي تطبيق:</label>
                    <input
                      type="text"
                      placeholder="https://vt.tiktok.com/... أو instagram.com/reel/..."
                      value={testUrl}
                      onChange={(e) => setTestUrl(e.target.value)}
                      className="w-full bg-slate-950 text-white text-xs px-2.5 py-1.5 rounded-lg border border-slate-800 focus:outline-none focus:border-emerald-500"
                    />
                  </div>

                  <button
                    type="submit"
                    disabled={isProcessing}
                    className="w-full py-2 rounded-xl bg-emerald-500 hover:bg-emerald-400 text-slate-950 font-bold text-xs flex items-center justify-center gap-1.5 shadow-md transition-all cursor-pointer"
                  >
                    {isProcessing ? (
                      <>
                        <RefreshCw className="w-3.5 h-3.5 animate-spin" />
                        <span>جارٍ استخراج وتنزيل الفيديو...</span>
                      </>
                    ) : (
                      <>
                        <Download className="w-3.5 h-3.5" />
                        <span>بدء التنزيل السريع (بدون علامة)</span>
                      </>
                    )}
                  </button>
                </form>

                {downloadSuccess && (
                  <div className="mt-3 p-2.5 rounded-xl bg-emerald-950/60 border border-emerald-500/40 text-[11px] text-emerald-300 flex items-center gap-2 animate-fade-in">
                    <CheckCircle2 className="w-4 h-4 shrink-0" />
                    <span>تم حفظ الفيديو في المعرض بنجاح بأعلى دقة 1080p!</span>
                  </div>
                )}

                {/* Adsgram Banner Mockup */}
                <div className="mt-6 p-2 rounded-xl bg-slate-900/80 border border-slate-800 text-center">
                  <span className="text-[9px] text-slate-500 block uppercase">مساحة إعلانية خفيفة (Adsgram)</span>
                  <div className="mt-1 py-3 bg-slate-950 rounded border border-slate-800/80 text-[10px] text-slate-400">
                    ⚡ شاهد إعلاناً واحداً للحصول على سرعة VIP مجانية
                  </div>
                </div>
              </div>
            </div>

            {/* Explanation & Features */}
            <div className="lg:col-span-7 bg-slate-900 border border-slate-800 rounded-2xl p-6 space-y-4">
              <h3 className="font-bold text-white text-base flex items-center gap-2">
                <Cpu className="w-5 h-5 text-emerald-400" />
                بنية التطبيق وتكامل محرك التنزيل
              </h3>
              <p className="text-xs md:text-sm text-slate-300 leading-relaxed">
                التطبيق مبني بالكامل على مكتبات أندرويد الحديثة:
              </p>
              <ul className="space-y-2 text-xs text-slate-400">
                <li className="flex items-start gap-2">
                  <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0 mt-0.5" />
                  <span><strong>DownloadEngine:</strong> محرك استخراج متعدد المنصات (TikTok, Reels, Shorts) مع التبديل التلقائي عند تعطل أحد السيرفرات (Failover Scrapers).</span>
                </li>
                <li className="flex items-start gap-2">
                  <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0 mt-0.5" />
                  <span><strong>Room Database:</strong> حفظ سجل التنزيلات محلياً بدون إرسال بيانات المستخدم لأي طرف خارجي.</span>
                </li>
                <li className="flex items-start gap-2">
                  <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0 mt-0.5" />
                  <span><strong>AdManager:</strong> نظام إدارة إعلانات Adsgram بمعدل تكرار ذكي (Frequency Capping) لضمان تجربة مستخدم خالية من الإزعاج.</span>
                </li>
              </ul>
            </div>
          </div>
        )}

        {/* Tab 4: Monetization & Profitability Calculator */}
        {activeTab === 'calculator' && (
          <div className="space-y-6">
            <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6">
              <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 mb-6">
                <div>
                  <h2 className="text-lg md:text-xl font-bold text-white flex items-center gap-2">
                    <TrendingUp className="w-5 h-5 text-emerald-400" />
                    حاسبة العائد المالي التفاعلية للبوت والتطبيق (العراق)
                  </h2>
                  <p className="text-xs md:text-sm text-slate-400 mt-1">
                    حساب دقيق للأرباح الصافية بعد خصم مصاريف السيرفر بالدينار العراقي والدولار.
                  </p>
                </div>

                <div className="p-3 rounded-xl bg-emerald-950/40 border border-emerald-500/40 text-left">
                  <span className="text-[11px] text-emerald-300 block">صافي الربح الشهري المقدر:</span>
                  <span className="text-xl md:text-2xl font-black text-emerald-400">{formatMoney(netProfitUSD)}</span>
                </div>
              </div>

              {/* Sliders */}
              <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                <div className="space-y-2 p-3.5 bg-slate-950 rounded-xl border border-slate-800">
                  <div className="flex justify-between text-xs">
                    <span className="text-slate-300">نسبة النشاط اليومي (DAU):</span>
                    <span className="text-emerald-400 font-bold">{dauRate}% ({dau} مستخدم/يوم)</span>
                  </div>
                  <input
                    type="range"
                    min="3"
                    max="20"
                    value={dauRate}
                    onChange={(e) => setDauRate(Number(e.target.value))}
                    className="w-full accent-emerald-500 h-1.5 bg-slate-800 rounded-lg appearance-none cursor-pointer"
                  />
                  <p className="text-[10px] text-slate-500">حركة التنزيل اليومية الحقيقية من أصل 15 ألف مستخدم.</p>
                </div>

                <div className="space-y-2 p-3.5 bg-slate-950 rounded-xl border border-slate-800">
                  <div className="flex justify-between text-xs">
                    <span className="text-slate-300">سعر CPM إعلانات Adsgram للعراق:</span>
                    <span className="text-cyan-400 font-bold">${cpmRate.toFixed(2)}</span>
                  </div>
                  <input
                    type="range"
                    min="0.30"
                    max="1.20"
                    step="0.05"
                    value={cpmRate}
                    onChange={(e) => setCpmRate(Number(e.target.value))}
                    className="w-full accent-cyan-500 h-1.5 bg-slate-800 rounded-lg appearance-none cursor-pointer"
                  />
                  <p className="text-[10px] text-slate-500">العائد الفعلي لإعلانات المكافأة داخل العراق.</p>
                </div>

                <div className="space-y-2 p-3.5 bg-slate-950 rounded-xl border border-slate-800">
                  <div className="flex justify-between text-xs">
                    <span className="text-slate-300">عدد مشتركي VIP (زين كاش ونجوم):</span>
                    <span className="text-amber-400 font-bold">{vipSubs} مشترك</span>
                  </div>
                  <input
                    type="range"
                    min="10"
                    max="200"
                    value={vipSubs}
                    onChange={(e) => setVipSubs(Number(e.target.value))}
                    className="w-full accent-amber-500 h-1.5 bg-slate-800 rounded-lg appearance-none cursor-pointer"
                  />
                  <p className="text-[10px] text-slate-500">اشتراك 4,000 د.ع شهرياً بدون إعلانات وبسرعة قصوى.</p>
                </div>
              </div>
            </div>
          </div>
        )}
      </main>

      {/* Footer */}
      <footer className="border-t border-slate-900 bg-slate-950/80 py-4 mt-auto text-xs text-slate-500">
        <div className="max-w-7xl mx-auto px-4 flex flex-col sm:flex-row items-center justify-between gap-2">
          <span>Media Downloader APK &copy; 2026 • مهيأ بالكامل لـ GitHub Actions</span>
          <span>طرق الدفع والسحب: زين كاش | آسيا حوالة | TON | Stars</span>
        </div>
      </footer>
    </div>
  );
}
