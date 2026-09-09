@REM ----------------------------------------------------------------------------
@REM Licensed to the Apache Software Foundation (ASF) under one
@REM or more contributor license agreements.  See the NOTICE file
@REM distributed with this work for additional information
@REM regarding copyright ownership.  The ASF licenses this file
@REM to you under the Apache License, Version 2.0 (the
@REM "License"); you may not use this file except in compliance
@REM with the License.  You may obtain a copy of the License at
@REM
@REM    https://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing,
@REM software distributed under the License is distributed on an
@REM "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
@REM KIND, either express or implied.  See the License for the
@REM specific language governing permissions and limitations
@REM under the License.
@REM ----------------------------------------------------------------------------

@REM ----------------------------------------------------------------------------
@REM Apache Maven Wrapper startup batch script, version 3.3.2
@REM
@REM Required ENV vars:
@REM   JAVA_HOME - location of a JDK home dir
@REM
@REM Optional ENV vars
@REM   MAVEN_BATCH_ECHO - set to 'on' to enable the echoing of the batch commands
@REM   MAVEN_BATCH_PAUSE - set to 'on' to wait for a keystroke before ending
@REM   MAVEN_OPTS - parameters passed to the Java VM when running Maven
@REM     e.g. to debug Maven itself, use
@REM       set MAVEN_OPTS=-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000
@REM   MAVEN_SKIP_RC - flag to disable loading of mavenrc files
@REM ----------------------------------------------------------------------------

@IF "%__MVNW_ARG0_NAME__%"=="" (SET MVNW_VERBOSE=false) ELSE (
  IF /I "%__MVNW_ARG0_NAME__%"=="MVNW_VERBOSE" (
    IF NOT "%MAVEN_MVNW_VERBOSE%"=="" (SET MVNW_VERBOSE=%MAVEN_MVNW_VERBOSE%) ELSE (SET MVNW_VERBOSE=true)
  ) ELSE (SET MVNW_VERBOSE=false)
)

@SET MAVEN_PROJECTBASEDIR=%MAVEN_BASEDIR%
IF NOT "%MAVEN_PROJECTBASEDIR%"=="" GOTO endDetectBaseDir

@SET "EXEC_DIR=%CD%"
@SET "WRK_DIR=%CD%"
:findBaseDir
IF EXIST "%WRK_DIR%"\.mvn GOTO baseDirFound
cd ..
SET "WRK_DIR=%CD%"
IF NOT "%EXEC_DIR%"=="%WRK_DIR%" GOTO findBaseDir
SET WRK_DIR=
GOTO baseDirNotFound

:baseDirFound
SET "MAVEN_PROJECTBASEDIR=%WRK_DIR%"
cd "%EXEC_DIR%"
GOTO endDetectBaseDir

:baseDirNotFound
SET "MAVEN_PROJECTBASEDIR=%EXEC_DIR%"
cd "%EXEC_DIR%"

:endDetectBaseDir
IF "%MVNW_VERBOSE%"=="true" @echo MAVEN_PROJECTBASEDIR=%MAVEN_PROJECTBASEDIR%

@REM Strip trailing backslash from JAVA_HOME to avoid double-backslash in path
@SET "JAVA_HOME_TRIMMED=%JAVA_HOME%"
IF "%JAVA_HOME_TRIMMED:~-1%"=="\" SET "JAVA_HOME_TRIMMED=%JAVA_HOME_TRIMMED:~0,-1%"

@SET "MAVEN_JAVA_EXE=%JAVA_HOME_TRIMMED%\bin\java.exe"
IF NOT EXIST "%MAVEN_JAVA_EXE%" SET "MAVEN_JAVA_EXE=java"

@SET WRAPPER_LAUNCHER=org.apache.maven.wrapper.MavenWrapperMain
@SET "WRAPPER_JAR=%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.jar"
@SET DOWNLOAD_URL=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.2/maven-wrapper-3.3.2.jar

FOR /F "usebackq tokens=1,2 delims==" %%A IN ("%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.properties") DO (
    IF /I "%%A"=="wrapperUrl" SET DOWNLOAD_URL=%%B
)

@IF EXIST "%WRAPPER_JAR%" (
    IF "%MVNW_VERBOSE%"=="true" echo Found %WRAPPER_JAR%
) ELSE (
    IF "%MVNW_VERBOSE%"=="true" echo Downloading %DOWNLOAD_URL% to %WRAPPER_JAR%
    powershell -Command "(New-Object System.Net.WebClient).DownloadFile('%DOWNLOAD_URL%', '%WRAPPER_JAR%')"
)

@IF NOT EXIST "%WRAPPER_JAR%" (
    @echo ERROR: Maven wrapper jar could not be downloaded
    exit /b 1
)

@SET MAVEN_CONFIG=
@IF NOT "%MVNW_USERNAME%"=="" SET MAVEN_CONFIG=--settings "%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\settings.xml"

@SET JVM_CONFIG_MAVEN_PROPS=
@IF EXIST "%MAVEN_PROJECTBASEDIR%\.mvn\jvm.config" (
    FOR /F "usebackq delims=" %%A IN ("%MAVEN_PROJECTBASEDIR%\.mvn\jvm.config") DO (
        SET "JVM_CONFIG_MAVEN_PROPS=%JVM_CONFIG_MAVEN_PROPS% %%A"
    )
)

"%MAVEN_JAVA_EXE%" %JVM_CONFIG_MAVEN_PROPS% %MAVEN_OPTS% %MAVEN_DEBUG_OPTS% ^
  -classpath "%WRAPPER_JAR%" ^
  "-Dmaven.multiModuleProjectDirectory=%MAVEN_PROJECTBASEDIR%" ^
  %WRAPPER_LAUNCHER% %MAVEN_CONFIG% %*

@IF "%ERRORLEVEL%"=="0" GOTO end
SET ERROR_CODE=%ERRORLEVEL%

:end
EXIT /B %ERROR_CODE%
