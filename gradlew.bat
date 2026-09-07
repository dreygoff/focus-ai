@REM @ECHO OFF
SETLOCAL
SET "ANDROID_SDK_ROOT=%LOCALAPPDATA%\Android\Sdk"
SET "JAVA_HOME=C:\Program Files\Java\jdk-25"
SET "JAVA_EXE=%JAVA_HOME%\bin\java"
IF NOT EXIST "%JAVA_EXE%" SET JAVA_EXE=java
"%JAVA_EXE%" -classpath "%~dp0gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain %*
