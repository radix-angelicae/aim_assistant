@rem
@rem Gradle startup script for Windows
@rem
@if "%DEBUG%" == "" @echo off
@rem Set local scope
setlocal
set DIRNAME=%~dp0
if "%DIRNAME%" == "" set DIRNAME=.
set APP_HOME=%DIRNAME%
set CLASSPATH=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar
@rem Execute Gradle
java -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*
