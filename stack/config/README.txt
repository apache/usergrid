
Global configuration properties

Standard configuration:

src/main/resources
	cassandra.yaml
	hazelcast.xml
	log4j.properties
	usergrid.properties

Test configuration:

src/test/resources
	cassandra.yaml
	hazelcast.xml
	log4j.properties
	usergrid.properties

Important configuration options in usergrid.properties:

cassandra.use_remote
cassandra.local.url
cassandra.remote.url

Make sure you're only running with cassandra.use_remote=false when testing and
that you're running with cassandra.use_remote=true when running in production.

You can override the properties by having a file named
usergrid-custom.properties in the classpath. This will get loaded after the
usergrid.properties file is loaded and will override any values with the same
names. This makes it easier to keep confidential credentials out of source
code version control.

