@echo off
setlocal
rem ===== false-god-backend one-click start =====
set "JAVA_HOME=C:\Program Files\Zulu\zulu-21"
set "PATH=%JAVA_HOME%\bin;%PATH%"
set "M2=C:\Users\Administrator\.workbuddy\binaries\maven\apache-maven-3.9.9"
set "JAR=%~dp0target\false-god-backend-1.0.0.jar"

if not exist "%JAR%" (
  echo [backend] jar not found, building with maven, please wait...
  cd /d "%~dp0"
  java -classpath "%M2%\boot\plexus-classworlds-2.8.0.jar" -Dclassworlds.conf="%M2%\bin\m2.conf" -Dmaven.home="%M2%" -Dmaven.multiModuleProjectDirectory="%~dp0." -Dlibrary.jansi.path="%M2%\lib\jansi-native" org.codehaus.plexus.classworlds.launcher.Launcher -q -DskipTests package
)

echo [backend] starting on http://localhost:8080 ...
java -jar "%JAR%" --server.port=8080
pause
