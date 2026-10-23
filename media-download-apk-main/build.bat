@echo off
REM ==============================================================================
REM سكربت بناء تطبيق تحميل الميديا - Media Downloader لنظام Windows
REM ==============================================================================

echo ==================================================
echo   🚀 بدء عملية بناء تطبيق Media Downloader APK
echo ==================================================

if not exist "gradlew.bat" (
    echo ❌ خطأ: ملف gradlew.bat غير موجود في هذا المجلد!
    pause
    exit /b 1
)

if not exist "debug.keystore" (
    echo 🔑 توليد ملف التوقيع debug.keystore تلقائياً...
    keytool -genkeypair -alias androiddebugkey -keypass android -keystore debug.keystore -storepass android -dname "CN=Android Debug,O=Android,C=US" -keyalg RSA -keysize 2048 -validity 10000
)

if not exist ".env" (
    if exist ".env.example" (
        copy .env.example .env >nul
    )
)

echo 🔨 [1/2] جاري بناء نسخة Debug...
call gradlew.bat assembleDebug --no-configuration-cache --no-parallel

echo 🔨 [2/2] جاري بناء نسخة Release...
call gradlew.bat assembleRelease --no-configuration-cache --no-parallel

if not exist "dist" mkdir dist
copy /y app\build\outputs\apk\debug\*.apk dist\ >nul 2>&1
copy /y app\build\outputs\apk\release\*.apk dist\ >nul 2>&1

echo ==================================================
echo   ✅ اكتمل البناء بنجاح! تم نسخ الملفات إلى مجلد dist
echo ==================================================
pause
