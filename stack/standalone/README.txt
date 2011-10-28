Usergrid Standalone Server

Standalone server for testing and debugging

Invoked from the command-line like this:

java -jar usergrid-standalone-0.0.1-SNAPSHOT.jar

To initialize the database:

java -jar usergrid-standalone-0.0.1-SNAPSHOT.jar -init

To launch an embedded Cassandra server and initialize the database:

java -jar usergrid-standalone-0.0.1-SNAPSHOT.jar -db -init

The standalone server will load a usergrid-custom.properties file from the
same location as the jar file. You can override things like the location of
the Cassandra cluster in that file

