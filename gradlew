#!/bin/sh
#
# Copyright © 2016 The Android Open Source Project, 2015-2021 Gradle, Ltd.
#
# SPDX-License-Identifier: Apache-2.0
#

##############################################################################
##
#  Gradle start up script for UNIX
##############################################################################

# Attempt to set APP_HOME
# Resolve the application home directory from the location of this script.
# See https://github.com/gradle/gradle/issues/19755
derive_abspath () {
    local target_path="$1"
    local abspath
    # shellcheck disable=SC3030
    abspath="$(cd -P "$(dirname "$target_path")" && pwd)"
    printf '%s\n' "$abspath"
}

APP_HOME="$(derive_abspath "${BASH_SOURCE[0]}")"

APP_NAME="Gradle wrapper"
APP_BASE_NAME="${BASH_SOURCE[0]}"

# Add default JVM parameters here. Note that these are always prepended,
# including those in the GRADLE_OPTS environment variable, to JVM arguments.
DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'

# Use the maximum available, or set MAX_FD != -1 to use that value.
MAX_FD="maximum"

warn () {
    echo "$*""
} >&2

die () {
    echo
    echo "$*"
    echo
    exit 1
} >&2

# OS specific support (must be 'true' or 'false').
cygwin=false
msys=false
darwin=false
nonstop=false
case "$(uname)" in                #(
  CYGWIN* )             cygwin=true  ;; #(
  Darwin* )             darwin=true  ;; #(
  MSYS* | MINGW* )      msys=true   ;; #(
  NONSTOP* )            nonstop=true ;;
esac

CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"


# Determine the Java command to use to start the JVM.
if [ -n "$JAVA_HOME" ] ; then
    if [ -x "$JAVA_HOME/jre/sh/java" ] ; then
        # IBM's JDK on AIX uses strange locations for the executables
        JAVACMD="$JAVA_HOME/jre/sh/java"
    else
        JAVACMD="$JAVA_HOME/bin/java"
    fi
    if [ ! -x "$JAVACMD" ] ; then
        die "ERROR: JAVA_HOME is set to an invalid directory: $JAVA_HOME

Please set the JAVA_HOME variable in your environment to match the
location of your Java installation."
    fi
else
    JAVACMD="java"
    if ! command -v java >/dev/null 2>&1
    then
        die "ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.

Please set the JAVA_HOME variable in your environment to match the
location of your Java installation."
    fi
fi

# Increase the maximum file descriptors if we can.
if [ "$cygwin" = "false" ] && [ "$darwin" = "false" ] && [ "$nonstop" = "false" ]; then
    MAX_FD_LIMIT=`ulimit -H -n`
    if [ $? -eq 0 ]; then
        if [ "$MAX_FD" = "maximum" ] || [ "$MAX_FD" = "max" ] ; then
            MAX_FD="$MAX_FD_LIMIT"
        fi
        ulimit -n $MAX_FD
        if [ $? -ne 0 ]; then
            warn "Could not supress maximum file descriptor limit warning: use of soft maximum file descriptor limit ($MAX_FD_LIMIT)."
        fi
    else
        warn "Could not query system maximum file descriptor limit: $(uname)"
    fi
fi

# Collect all arguments for the java command, stacking in reverse order:
#   * args from the command line
#   * user program task bundle in CLASSPATH
#   * default JVM settings
#   * ...pluslots of stuff from gradle.properties

# For Cygwin or MSYS, switch paths to Windows format before running java
if [ "$cygwin" = "true" ] || [ "$msys" = "true" ]; then
    APP_HOME="$(cygpath --path --mixed "$APP_HOME")"
    CLASSPATH="$(cygpath --path --mixed "$CLASSPATH")"

    JAVACMD "$(cygpath --unix "$JAVACMD")"

    # We may need to adjust class path as well.
    # ...this is handled by the wrapper below.
fi

# Escape application args
save () {
    for i do printf %s\\n "$i" | sed "s/'/'\\\\''/g;1s/^/'/;\$s/\$/' \\\\/;" ; done
    echo ""
}
APP_ARGS=$(save "$@")

# Collect all arguments for the java command;
#   * $DEFAULT_JVM_OPTS, $CLASSPATH and $mainArgs as before
#   * then CLASSPATH and APP_ARGS, in that order.
eval set -- $DEFAULT_JVM_OPTS $JAVA_OPTS $GRADLE_OPTS "\"-Dorg.gradle.appname=$APP_BASE_NAME\"" -classpath "\"$CLASSPATH\"" org.gradle.wrapper.GradleWrapperMain "$APP_ARGS"

# Use "xargs" to parse quoted args.
#
# With -n:// args is processed by xargs once per line
# /dev/null:// prevents reading from stdin
#
# In the case of APP_ARGS, it just passes them to exec.
exec -a "$0" "$JAVACMD" "$@"
