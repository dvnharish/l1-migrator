@ECHO OFF
SETLOCAL
set WRAPPER_JAR=.mvn\wrapper\maven-wrapper-3.2.0.jar
IF NOT EXIST %WRAPPER_JAR% (
  mkdir .mvn\wrapper 2> NUL
  powershell -Command "Invoke-WebRequest -UseBasicParsing https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar -OutFile %WRAPPER_JAR%"
)
set CMD=%*
java -Dmaven.multiModuleProjectDirectory=%CD% -cp %WRAPPER_JAR% org.apache.maven.wrapper.MavenWrapperMain %CMD%
ENDLOCAL


