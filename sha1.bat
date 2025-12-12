@echo off
echo ==============================================
echo    Running Firebase SHA-1 / SHA-256 Report
echo ==============================================

REM ---- CHANGE THIS PATH IF ANDROID STUDIO IS IN A DIFFERENT DIRECTORY ----
set JAVA_PATH="C:\Program Files\Android\Android Studio\jbr\bin\java.exe"

REM ---- CHECK JAVA PATH ----
%JAVA_PATH% -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Cannot find Android Studio JDK.
    echo Please check your installation path.
    pause
    exit /b
)

echo Using Java at: %JAVA_PATH%

REM ---- RUN SIGNING REPORT ----
%JAVA_PATH% -classpath "gradle/wrapper/gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain signingReport

echo.
echo ==============================================
echo       DONE. Scroll up to see SHA-1.
echo ==============================================
pause
