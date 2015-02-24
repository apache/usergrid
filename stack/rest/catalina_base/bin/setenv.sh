
JAVA_OPTS="$JAVA_OPTS -Dcom.sun.management.jmxremote.port=8089 "
JAVA_OPTS="$JAVA_OPTS -Dcom.sun.management.jmxremote.ssl=false "
JAVA_OPTS="$JAVA_OPTS -Dcom.sun.management.jmxremote.authenticate=false"
JAVA_OPTS="$JAVA_OPTS -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=8000"
JAVA_OPTS="$JAVA_OPTS -Xmx3000m -Xms1000m -XX:MaxPermSize=200m -XX:+CMSClassUnloadingEnabled -XX:+CMSPermGenSweepingEnabled"

CATALINA_PID="$CATALINA_BASE/tomcat.pid"
