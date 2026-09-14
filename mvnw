#!/bin/sh
# ----------------------------------------------------------------------------
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
# ----------------------------------------------------------------------------
# Apache Maven Wrapper startup script, version 3.3.2

if [ -z "${MAVEN_SKIP_RC}" ]; then
  [ -f /usr/local/etc/mavenrc ] && . /usr/local/etc/mavenrc
  [ -f /etc/mavenrc ] && . /etc/mavenrc
  [ -f "$HOME/.mavenrc" ] && . "$HOME/.mavenrc"
fi

saveddir=$(pwd)

cygwin=false
darwin=false
mingw=false
case "$(uname)" in
CYGWIN*) cygwin=true ;;
MINGW*) mingw=true ;;
Darwin*)
  darwin=true
  if [ -z "${JAVA_HOME}" ]; then
    if [ -x "/usr/libexec/java_home" ]; then
      JAVA_HOME="$(/usr/libexec/java_home)"
      export JAVA_HOME
    elif [ -d "/Library/Java/Home" ]; then
      JAVA_HOME=/Library/Java/Home
      export JAVA_HOME
    fi
  fi
  ;;
esac

if [ -z "${JAVA_HOME}" ] && [ -r /etc/gentoo-release ]; then
  JAVA_HOME=$(java-config --jre-home)
fi

if [ -z "${JAVA_HOME}" ]; then
  javaExecutable="$(command -v javac)"
  if [ -n "$javaExecutable" ]; then
    javaHome="$(dirname "$javaExecutable")"
    javaHome=$(expr "$javaHome" : '\(.*\)/bin')
    JAVA_HOME="$javaHome"
    export JAVA_HOME
  fi
fi

if [ -z "${JAVA_HOME}" ]; then
  echo "Error: JAVA_HOME not found in your environment." >&2
  exit 1
fi

if [ ! -x "${JAVA_HOME}/bin/java" ]; then
  echo "Error: JAVA_HOME is set to an invalid directory: ${JAVA_HOME}" >&2
  exit 1
fi

find_maven_basedir() {
  basedir="$1"
  wdir="$1"
  while [ "$wdir" != "/" ]; do
    if [ -d "$wdir/.mvn" ]; then
      basedir=$wdir
      break
    fi
    wdir=$(cd "$wdir/.." && pwd) || exit 1
  done
  printf '%s' "$(cd "$basedir" && pwd)"
}

concat_lines() {
  if [ -f "$1" ]; then
    tr -s '\r\n' '  ' <"$1"
  fi
}

BASE_DIR=$(find_maven_basedir "$(dirname "$0")")
MAVEN_PROJECTBASEDIR=${MAVEN_BASEDIR:-"$BASE_DIR"}
export MAVEN_PROJECTBASEDIR

cd "$saveddir" || exit 1

MAVEN_OPTS="$(concat_lines "$MAVEN_PROJECTBASEDIR/.mvn/jvm.config") $MAVEN_OPTS"

WRAPPER_JAR="$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.jar"
WRAPPER_LAUNCHER=org.apache.maven.wrapper.MavenWrapperMain
WRAPPER_URL="https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.2/maven-wrapper-3.3.2.jar"

if [ -r "$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.properties" ]; then
  wrapperUrl=$(grep -e "^wrapperUrl" "$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.properties" | cut -d '=' -f2-)
  if [ -n "$wrapperUrl" ]; then
    WRAPPER_URL="$wrapperUrl"
  fi
fi

if [ ! -r "$WRAPPER_JAR" ]; then
  if command -v wget >/dev/null; then
    wget -q "$WRAPPER_URL" -O "$WRAPPER_JAR"
  elif command -v curl >/dev/null; then
    curl -fsSL -o "$WRAPPER_JAR" "$WRAPPER_URL"
  else
    echo "Error: maven-wrapper.jar is missing and neither wget nor curl is available." >&2
    exit 1
  fi
fi

# shellcheck disable=SC2086
exec "$JAVA_HOME/bin/java" \
  $MAVEN_OPTS \
  $MAVEN_DEBUG_OPTS \
  -classpath "$WRAPPER_JAR" \
  "-Dmaven.multiModuleProjectDirectory=${MAVEN_PROJECTBASEDIR}" \
  ${WRAPPER_LAUNCHER} "$@"
