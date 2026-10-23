#!/usr/bin/env bash
# ==============================================================================
# سكربت بناء تطبيق تحميل الميديا - Media Downloader (APK Build Script)
# متوافق مع أنظمة Linux, macOS, GitHub Actions, و Termux
# ==============================================================================

set -e

echo "=================================================="
echo "  🚀 بدء عملية بناء تطبيق Media Downloader APK"
echo "=================================================="

# 1. تنظيف وتصحيح نهايات أسطر ملف gradlew وصلاحيات التشغيل
if [ -f "gradlew" ]; then
    sed -i 's/\r$//' gradlew 2>/dev/null || true
    chmod +x gradlew
else
    echo "❌ خطأ: ملف gradlew غير موجود في هذا المجلد!"
    exit 1
fi

# 2. التأكد من وجود ملف التوقيع الرقمي (debug.keystore)
if [ ! -f "debug.keystore" ]; then
    echo "🔑 توليد ملف التوقيع debug.keystore تلقائياً..."
    keytool -genkeypair \
        -alias androiddebugkey \
        -keypass android \
        -keystore debug.keystore \
        -storepass android \
        -dname "CN=Android Debug,O=Android,C=US" \
        -keyalg RSA \
        -keysize 2048 \
        -validity 10000
    echo "✅ تم إنشاء debug.keystore بنجاح."
fi

# 3. التأكد من ملف البيئة .env
if [ ! -f ".env" ] && [ -f ".env.example" ]; then
    echo "📄 نسخ إعدادات .env.example إلى .env..."
    cp .env.example .env
fi

# 4. بناء نسخة assembleDebug
echo "🔨 [1/2] جاري بناء وتجميع نسخة الفحص والتطوير (Debug APK)..."
./gradlew assembleDebug --no-configuration-cache --no-parallel --stacktrace

# 5. بناء نسخة assembleRelease
echo "🔨 [2/2] جاري بناء وتجميع النسخة النهائية الموقعة (Release APK)..."
./gradlew assembleRelease --no-configuration-cache --no-parallel --stacktrace

# 6. تجميع ملفات الـ APK في مجلد dist/
VERSION_NAME=$(sed -n 's/.*versionName = "\(.*\)".*/\1/p' app/build.gradle.kts 2>/dev/null | head -n1)
VERSION_NAME=${VERSION_NAME:-1.0.22}

mkdir -p dist

find app/build/outputs/apk -name "*.apk" -type f | while read -r apk; do
    filename=$(basename "$apk")
    cp -f "$apk" "dist/$filename"
    if [[ "$filename" == *"release"* ]]; then
        cp -f "$apk" "dist/MediaDownloader-v${VERSION_NAME}-release.apk"
    elif [[ "$filename" == *"debug"* ]]; then
        cp -f "$apk" "dist/MediaDownloader-v${VERSION_NAME}-debug.apk"
    fi
done

echo "=================================================="
echo "  ✅ اكتمل البناء بنجاح تام! مخرجات الـ APK جاهزة في مجلد dist/:"
echo "=================================================="
ls -lh dist/*.apk 2>/dev/null || true
echo "=================================================="
