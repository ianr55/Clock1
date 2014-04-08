#!bash
# Launch from Windows desktop shortcut.
# Requires mingw bash.exe from git/bin, as W7 default program for .sh extension.
# Explicit setup when default java not 1.8.
# Derived from scala/bin/scala script.
HOME=/c/Users/ianr
JAVA_HOME='/c/Program Files/Java/jdk1.8.0'
JAVACMD="$JAVA_HOME/bin/java"
SCALA_HOME='/c/Program Files (x86)/scala'
for ext in "$SCALA_HOME"/lib/* ; do
    if [[ -z "$TOOL_CP" ]]; then
        TOOL_CP="$ext"
    else
        TOOL_CP="${TOOL_CP}:${ext}"
    fi
done
PRJ_HOME=/c/usr/ianr/Projects/Idea1/Clock1
PRJ_CP="$PRJ_HOME/out/production/Clock1"
CLASSPATH=$PRJ_CP:$HOME/lib/ScalaFX8m4.jar:$TOOL_CP:$SCALA_HOME/lib/scala-library.jar:$JAVA_HOME/jre/lib/rt.jar
PATH=
cd $PRJ_HOME/run
echo "Java start"
"$JAVACMD" $JAVA_OPTS -classpath "$CLASSPATH" \
    -Dscala.home="$SCALA_HOME" -Dscala.usejavacp=true \
    scala.tools.nsc.MainGenericRunner clock.Clock \
    2>Clock.error.txt
echo "Java exit"

