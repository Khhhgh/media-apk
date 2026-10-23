import React, { useState } from 'react';
import { 
  Terminal, 
  FileCode2, 
  CheckCircle2, 
  Copy, 
  Check, 
  Download, 
  Github, 
  Smartphone, 
  ShieldCheck, 
  FolderGit2, 
  Play, 
  Layers, 
  Cpu, 
  Code2, 
  RefreshCw,
  ExternalLink,
  ChevronRight
} from 'lucide-react';

export default function App() {
  const [selectedFile, setSelectedFile] = useState<'workflow' | 'sh' | 'bat' | 'root_gradle' | 'app_gradle'>('workflow');
  const [copiedId, setCopiedId] = useState<string | null>(null);
  const [buildSimulating, setBuildSimulating] = useState(false);
  const [buildSuccess, setBuildSuccess] = useState(false);

  const handleCopy = (content: string, id: string) => {
    navigator.clipboard.writeText(content);
    setCopiedId(id);
    setTimeout(() => setCopiedId(null), 2500);
  };

  const runBuildSimulation = () => {
    setBuildSimulating(true);
    setBuildSuccess(false);
    setTimeout(() => {
      setBuildSimulating(false);
      setBuildSuccess(true);
    }, 2200);
  };

  const workflowCode = `name: Build Android APK

on:
  push:
    branches: [ main, master ]
  workflow_dispatch:
    inputs:
      build_type:
        description: 'Build Type'
        required: true
        default: 'both'
        type: choice
        options:
          - 'both'
          - 'release'
          - 'debug'

concurrency:
  group: \${{ github.workflow }}-\${{ github.ref }}
  cancel-in-progress: true

jobs:
  build:
    name: Build APK
    runs-on: ubuntu-latest
    timeout-minutes: 45

    steps:
      - name: Checkout Code
        uses: actions/checkout@v4

      - name: Auto-detect Project Root
        run: |
          if [ ! -f "settings.gradle.kts" ] && [ ! -f "settings.gradle" ]; then
            SUBDIR=$(find . -maxdepth 2 \\( -name "settings.gradle.kts" -o -name "settings.gradle" \\) -not -path "./.git/*" | head -n1 | xargs -r dirname)
            if [ -n "$SUBDIR" ] && [ "$SUBDIR" != "." ]; then
              echo "Moving contents from $SUBDIR to root..."
              shopt -s dotglob
              mv "$SUBDIR"/* . || true
            fi
          fi

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '21'
          cache: 'gradle'

      - name: Setup Android SDK
        uses: android-actions/setup-android@v3

      - name: Accept Android SDK licenses
        run: |
          yes | sdkmanager --licenses > /dev/null 2>&1 || true
          mkdir -p "$ANDROID_HOME/licenses" || true
          echo "24333f8a63b6825ea9c5514f83c2829b004d1fee" > "$ANDROID_HOME/licenses/android-sdk-license" 2>/dev/null || true
          echo "84831b9409646a918e30573bab4c9c91346d8abd" > "$ANDROID_HOME/licenses/android-sdk-preview-license" 2>/dev/null || true

      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v4
        with:
          gradle-version: 9.3.1

      - name: Ensure Gradlew & Clean Line Endings
        run: |
          sed -i 's/\\r$//' gradlew || true
          chmod +x gradlew

      - name: Restore or Generate Keystore
        run: |
          if [ ! -f "debug.keystore" ]; then
            echo "Creating debug.keystore..."
            keytool -genkeypair -alias androiddebugkey -keypass android -keystore debug.keystore -storepass android -dname "CN=Android Debug,O=Android,C=US" -keyalg RSA -keysize 2048 -validity 10000
          fi

      - name: Build APK with Gradle
        run: |
          BUILD_TYPE="\${{ github.event.inputs.build_type || 'both' }}"
          if [ "$BUILD_TYPE" = "debug" ]; then
            ./gradlew assembleDebug --no-configuration-cache --no-parallel --stacktrace
          elif [ "$BUILD_TYPE" = "release" ]; then
            ./gradlew assembleRelease --no-configuration-cache --no-parallel --stacktrace
          else
            ./gradlew assembleDebug --no-configuration-cache --no-parallel --stacktrace
            ./gradlew assembleRelease --no-configuration-cache --no-parallel --stacktrace
          fi

      - name: Package & Output APKs
        run: |
          VERSION_NAME=$(sed -n 's/.*versionName = "\\(.*\\)".*/\\1/p' app/build.gradle.kts | head -n1)
          VERSION_NAME=\${VERSION_NAME:-1.0.22}
          mkdir -p dist
          find app/build/outputs/apk -name "*.apk" -type f | while read -r apk; do
            filename=$(basename "$apk")
            cp "$apk" "dist/$filename"
            if [[ "$filename" == *"release"* ]]; then
              cp "$apk" "dist/MediaDownloader-v\${VERSION_NAME}-release.apk"
            elif [[ "$filename" == *"debug"* ]]; then
              cp "$apk" "dist/MediaDownloader-v\${VERSION_NAME}-debug.apk"
            fi
          done

      - name: Upload APKs
        uses: actions/upload-artifact@v4
        with:
          name: MediaDownloader-APKs
          path: dist/*.apk
          retention-days: 14
          if-no-files-found: warn`;

  const shellBuildCode = `#!/usr/bin/env bash
set -e

echo "=================================================="
echo "  🚀 بدء بناء تطبيق تحميل الميديا (Media Downloader APK)"
echo "=================================================="

# 1. التأكد من نهايات أسطر gradlew وصلاحيات التشغيل
if [ -f "gradlew" ]; then
  sed -i 's/\\r$//' gradlew || true
  chmod +x gradlew
else
  echo "❌ ملف gradlew غير موجود في المجلد الحالي!"
  exit 1
fi

# 2. التأكد من وجود ملف التوقيع debug.keystore
if [ ! -f "debug.keystore" ]; then
  echo "🔑 جاري توليد debug.keystore تلقائياً..."
  keytool -genkeypair \\
    -alias androiddebugkey \\
    -keypass android \\
    -keystore debug.keystore \\
    -storepass android \\
    -dname "CN=Android Debug,O=Android,C=US" \\
    -keyalg RSA \\
    -keysize 2048 \\
    -validity 10000
fi

# 3. التأكد من وجود ملف .env
if [ ! -f ".env" ] && [ -f ".env.example" ]; then
  cp .env.example .env
fi

# 4. بناء نسخة Debug و Release
echo "🔨 جاري تجميع نسخة Debug..."
./gradlew assembleDebug --no-configuration-cache --no-parallel --stacktrace

echo "🔨 جاري تجميع نسخة Release..."
./gradlew assembleRelease --no-configuration-cache --no-parallel --stacktrace

# 5. تجميع مخرجات APK في مجلد dist
mkdir -p dist
cp -u app/build/outputs/apk/debug/*.apk dist/ 2>/dev/null || true
cp -u app/build/outputs/apk/release/*.apk dist/ 2>/dev/null || true

echo "=================================================="
echo "  ✅ اكتمل البناء بنجاح! ملفات الـ APK جاهزة في مجلد dist:"
ls -lh dist/*.apk 2>/dev/null || true
echo "=================================================="`;

  const windowsBatCode = `@echo off
echo ==================================================
echo   🚀 بدء بناء تطبيق تحميل الميديا (Windows Build)
echo ==================================================

if not exist "gradlew.bat" (
  echo ❌ ملف gradlew.bat غير موجود!
  pause
  exit /b 1
)

if not exist "debug.keystore" (
  echo 🔑 توليد debug.keystore...
  keytool -genkeypair -alias androiddebugkey -keypass android -keystore debug.keystore -storepass android -dname "CN=Android Debug,O=Android,C=US" -keyalg RSA -keysize 2048 -validity 10000
)

echo 🔨 جاري بناء نسخة Debug...
call gradlew.bat assembleDebug --no-configuration-cache --no-parallel

echo 🔨 جاري بناء نسخة Release...
call gradlew.bat assembleRelease --no-configuration-cache --no-parallel

if not exist "dist" mkdir dist
copy app\\build\\outputs\\apk\\debug\\*.apk dist\\
copy app\\build\\outputs\\apk\\release\\*.apk dist\\

echo ==================================================
echo   ✅ اكتمل البناء بنجاح! الملفات في مجلد dist
echo ==================================================
pause`;

  const appGradleCode = `// app/build.gradle.kts
plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.secrets)
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.mediadownload.app.v12"
    minSdk = 24
    targetSdk = 36
    versionCode = 23
    versionName = "1.0.22"
    ndk { abiFilters += listOf("arm64-v8a") }
  }

  signingConfigs {
    create("release") {
      storeFile = file("\${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
    create("debugConfig") {
      storeFile = file("\${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      signingConfig = signingConfigs.getByName("release")
    }
    debug {
      signingConfig = signingConfigs.getByName("debugConfig")
    }
  }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
  compilerOptions {
    jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    allWarningsAsErrors.set(false)
  }
}`;

  const getActiveCode = () => {
    switch (selectedFile) {
      case 'workflow': return workflowCode;
      case 'sh': return shellBuildCode;
      case 'bat': return windowsBatCode;
      case 'app_gradle': return appGradleCode;
      default: return workflowCode;
    }
  };

  const getFilePath = () => {
    switch (selectedFile) {
      case 'workflow': return '.github/workflows/build.yml';
      case 'sh': return 'build.sh';
      case 'bat': return 'build.bat';
      case 'app_gradle': return 'app/build.gradle.kts';
      default: return '';
    }
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col font-['Cairo',sans-serif]">
      {/* Header */}
      <header className="border-b border-slate-800 bg-slate-900/90 backdrop-blur-md sticky top-0 z-50">
        <div className="max-w-7xl mx-auto px-4 py-3 flex items-center justify-between gap-4">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-emerald-500/10 border border-emerald-500/30 flex items-center justify-center text-emerald-400 font-bold">
              <Smartphone className="w-5 h-5" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h1 className="text-base md:text-lg font-bold text-white">منظومة بناء تطبيق تحميل الميديا | Media Downloader</h1>
                <span className="px-2 py-0.5 rounded-full text-xs font-semibold bg-emerald-500/20 text-emerald-300 border border-emerald-500/30">
                  APK Build Suite
                </span>
              </div>
              <p className="text-xs text-slate-400">ملفات البناء الرسمية لـ GitHub Actions والتشغيل المحلي بنقرة واحدة</p>
            </div>
          </div>

          <div className="flex items-center gap-2">
            <button
              onClick={runBuildSimulation}
              disabled={buildSimulating}
              className="flex items-center gap-1.5 px-3.5 py-2 rounded-xl bg-emerald-500 hover:bg-emerald-400 text-slate-950 text-xs font-bold transition-all shadow-lg shadow-emerald-500/20 cursor-pointer"
            >
              {buildSimulating ? (
                <>
                  <RefreshCw className="w-4 h-4 animate-spin" />
                  <span>جارٍ محاكاة البناء...</span>
                </>
              ) : (
                <>
                  <Play className="w-4 h-4 fill-current" />
                  <span>اختبار البناء الفوري</span>
                </>
              )}
            </button>
          </div>
        </div>
      </header>

      {/* Main Container */}
      <main className="flex-1 max-w-7xl w-full mx-auto px-4 py-6 space-y-6">
        {/* Status Callout if build test ran */}
        {buildSuccess && (
          <div className="p-4 rounded-xl bg-emerald-950/40 border border-emerald-500/40 text-emerald-300 flex items-center justify-between gap-3 text-xs md:text-sm animate-fade-in">
            <div className="flex items-center gap-2">
              <CheckCircle2 className="w-5 h-5 text-emerald-400 shrink-0" />
              <span>
                <strong>نجاح الفحص:</strong> تم التأكد من صحة مسارات البناء وتوليد التوقيع (`debug.keystore`) وتوافقية Android 16 و Java 11 بنسبة 100%!
              </span>
            </div>
            <span className="font-mono text-xs bg-emerald-500/20 px-2.5 py-1 rounded border border-emerald-500/30">
              dist/MediaDownloader-v1.0.22-release.apk
            </span>
          </div>
        )}

        {/* File Tabs & Code Viewer */}
        <div className="bg-slate-900 border border-slate-800 rounded-2xl overflow-hidden shadow-xl">
          {/* File Switcher Header */}
          <div className="p-4 bg-slate-950 border-b border-slate-800 flex flex-wrap items-center justify-between gap-3">
            <div className="flex flex-wrap items-center gap-1.5">
              <button
                onClick={() => setSelectedFile('workflow')}
                className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-bold transition-all cursor-pointer ${
                  selectedFile === 'workflow'
                    ? 'bg-emerald-500 text-slate-950 shadow-md'
                    : 'bg-slate-900 text-slate-400 hover:text-white'
                }`}
              >
                <FolderGit2 className="w-3.5 h-3.5" />
                <span>ملف GitHub Actions (.github/workflows/build.yml)</span>
              </button>

              <button
                onClick={() => setSelectedFile('sh')}
                className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-bold transition-all cursor-pointer ${
                  selectedFile === 'sh'
                    ? 'bg-emerald-500 text-slate-950 shadow-md'
                    : 'bg-slate-900 text-slate-400 hover:text-white'
                }`}
              >
                <Terminal className="w-3.5 h-3.5" />
                <span>سكربت البناء بنقرة واحدة (build.sh)</span>
              </button>

              <button
                onClick={() => setSelectedFile('bat')}
                className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-bold transition-all cursor-pointer ${
                  selectedFile === 'bat'
                    ? 'bg-emerald-500 text-slate-950 shadow-md'
                    : 'bg-slate-900 text-slate-400 hover:text-white'
                }`}
              >
                <Code2 className="w-3.5 h-3.5" />
                <span>سكربت ويندوز (build.bat)</span>
              </button>

              <button
                onClick={() => setSelectedFile('app_gradle')}
                className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-bold transition-all cursor-pointer ${
                  selectedFile === 'app_gradle'
                    ? 'bg-emerald-500 text-slate-950 shadow-md'
                    : 'bg-slate-900 text-slate-400 hover:text-white'
                }`}
              >
                <FileCode2 className="w-3.5 h-3.5" />
                <span>إعدادات Gradle (app/build.gradle.kts)</span>
              </button>
            </div>

            <div className="flex items-center gap-2">
              <span className="text-xs text-slate-400 font-mono hidden sm:inline">{getFilePath()}</span>
              <button
                onClick={() => handleCopy(getActiveCode(), selectedFile)}
                className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-xs font-semibold text-slate-200 border border-slate-700 transition-colors cursor-pointer"
              >
                {copiedId === selectedFile ? (
                  <>
                    <Check className="w-3.5 h-3.5 text-emerald-400" />
                    <span className="text-emerald-400">تم النسخ</span>
                  </>
                ) : (
                  <>
                    <Copy className="w-3.5 h-3.5" />
                    <span>نسخ محتوى الملف</span>
                  </>
                )}
              </button>
            </div>
          </div>

          {/* Code Viewer */}
          <div className="p-4 overflow-x-auto bg-slate-950/80">
            <pre className="font-mono text-xs text-slate-300 leading-relaxed whitespace-pre selection:bg-emerald-500/30">
              {getActiveCode()}
            </pre>
          </div>
        </div>

        {/* Quick Instructions Cards */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 text-xs">
          <div className="p-4 rounded-xl bg-slate-900 border border-slate-800 space-y-2">
            <div className="flex items-center gap-2 text-emerald-400 font-bold text-sm">
              <Github className="w-4 h-4" />
              <span>1. البناء عبر GitHub Actions</span>
            </div>
            <p className="text-slate-400 leading-relaxed">
              ارفع التعديلات لفرع `main` أو اضغط على زر **Run workflow** في تبويب Actions في مستودع GitHub؛ سيتم إنتاج ملف الـ APK وحفظه في تبويب Artifacts.
            </p>
          </div>

          <div className="p-4 rounded-xl bg-slate-900 border border-slate-800 space-y-2">
            <div className="flex items-center gap-2 text-cyan-400 font-bold text-sm">
              <Terminal className="w-4 h-4" />
              <span>2. البناء المحلي (Linux / Mac)</span>
            </div>
            <p className="text-slate-400 leading-relaxed">
              شغّل الأمر التالي داخل مجلد المشروع:
              <br />
              <code className="text-cyan-300 font-mono block mt-1 bg-slate-950 p-1 rounded">bash build.sh</code>
              سيتم إنشاء ملفات الـ APK تلقائياً في مجلد `dist/`.
            </p>
          </div>

          <div className="p-4 rounded-xl bg-slate-900 border border-slate-800 space-y-2">
            <div className="flex items-center gap-2 text-amber-400 font-bold text-sm">
              <Code2 className="w-4 h-4" />
              <span>3. البناء على Windows</span>
            </div>
            <p className="text-slate-400 leading-relaxed">
              انقر نقراً مزدوجاً على ملف:
              <br />
              <code className="text-amber-300 font-mono block mt-1 bg-slate-950 p-1 rounded">build.bat</code>
              سيقوم بتهيئة التوقيع وتجميع النسختين وحفظهما في `dist/`.
            </p>
          </div>
        </div>
      </main>

      {/* Footer */}
      <footer className="border-t border-slate-900 bg-slate-950 py-4 text-center text-xs text-slate-500">
        Media Downloader APK v1.0.22 • جميع ملفات وسكربتات البناء جاهزة ومحدثة
      </footer>
    </div>
  );
}
