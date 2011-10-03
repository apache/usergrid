Usergrid Command-line Tools

Command line tools for administration of a Usergrid installation.

Invoked from the command-line like this:

java -jar usergrid-tools-0.0.1-SNAPSHOT.jar Test

java -jar usergrid-tools-0.0.1-SNAPSHOT.jar SetupDB

java -jar usergrid-tools-0.0.1-SNAPSHOT.jar ImportFB -i fb_users.json -o ug_users.json

Use with caution!

java -jar usergrid-tools-0.0.1-SNAPSHOT.jar Cli -remote

These are built by Maven using the Maven Shade Plugin:

http://maven.apache.org/plugins/maven-shade-plugin/

The shade plugin bundles all the dependencies into a single jar so that the
tools can be run without any installation other than the JVM. The build
process currently outputs a number of duplicate class warnings and we've seen
some issues with overwritten manifest files but these should not cause the
build to fail.


