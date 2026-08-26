@rem Gradle wrapper for Windows
@echo off
setlocal
set GRADLE_OPTS=-Xmx512m
set JAVA_EXE=java

"%JAVA_EXE%" -jar "%~dp0gradle\wrapper\gradle-wrapper.jar" %*
