Usergrid Graphical Server Launcher

A simple Java app that provides a GUI for running the server as a
desktop app.

Creates a double-clickable jar.

Invoked from the command-line like this:

java -jar usergrid-launcher-${version}.jar -nogui

To initialize the database:

java -jar usergrid-launcher-${version}.jar -nogui -init

To launch an embedded Cassandra server and initialize the database:

java -jar usergrid-launcher-${version}.jar -nogui -db -init

The standalone server will load a usergrid-deployment.properties file from the
same location as the jar file. You can override things like the location of
the Cassandra cluster in that file.

