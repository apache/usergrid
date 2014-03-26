Usergrid Core Repository

Requirements

JDK 1.7 (http://www.oracle.com/technetwork/java/javase/downloads/index.html)
Maven (http://maven.apache.org/)

Building

Download and install Maven using the instructions on the Apache Maven website,
then, from the command line, go to the usergrid-core directory and type the
following:

mvn install

This will cause Maven to download and install all the necessary dependencies.
This also runs a small set of tests after compilation, however these tests do
start up an instance of Cassandra. To run the build without all the tests
being fired, use the following:

mvn install -DskipTests=true

Running

Usergrid-core contains the persistence layer and shared utilities for powering
the Usergrid service. The services layer is contained in usergrid-services and
exposes a higher-level API that's used by the usergrid-rest web services tier.


