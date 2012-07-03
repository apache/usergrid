# Usergrid
Platform Stack for Mobile & Rich Client Apps

For more info:

* **Documentation**: http://apigee.com/docs/usergrid/
* **Homepage**: http://apigee.com/about/products/usergrid
* **Google Group**: http://groups.google.com/group/usergrid


## Getting Started

Note: The easiest way to run Usergrid is to download the double-clickable jar
at:

https://usergrid.ci.cloudbees.com/job/Usergrid%20Nightly/lastSuccessfulBuild/org.usergrid$usergrid-launcher/artifact/org.usergrid/usergrid-launcher/0.0.1-SNAPSHOT/usergrid-launcher-0.0.1-SNAPSHOT.jar

This will let you try out the system without building or installing it.

## Requirements

* JDK 1.6 (http://www.oracle.com/technetwork/java/javase/downloads/index.html)
* Maven (http://maven.apache.org/)

## Building

From the command line, go to the usergrid directory and type the
following:

    mvn clean install -DskipTests=true

If you don't want to do a full build, you can download a pre-built version
of the launcher app from:

https://usergrid.ci.cloudbees.com/job/Usergrid%20Nightly/lastSuccessfulBuild/org.usergrid$usergrid-launcher/artifact/org.usergrid/usergrid-launcher/0.0.1-SNAPSHOT/usergrid-launcher-0.0.1-SNAPSHOT.jar

## Running

Usergrid-core contains the persistence layer and shared utilities for powering
the Usergrid service. The services layer is contained in usergrid-services and
exposes a higher-level API that's used by the usergrid-rest web services tier.

You can run Usergrid from the command-line from the
jar in the usergrid/standalone project:

  java -jar standalone/target/usergrid-standalone-0.0.1-SNAPSHOT.jar

After startup, your instance will be available on localhost, port 8080.
To check it’s running properly, you can try loading our status page:
http://localhost:8080/status

You can also run it as a webapp in Tomcat, by deploying the ROOT.war
file generated in the usergrid/rest project,

## Using the Admin Portal

By default, the Usergrid admin portal (https://github.com/apigee/usergrid-
portal) points to our production servers at api.usergrid.com. However, by
specifying an api_url argument in the query string, you can have it point to
your local instance instead. For example, you could reuse the version of the
admin portal we host on github and have that point to your local cluster by
opening the following URL in your browser:

http://apigee.github.com/usergrid-portal/?api_url=http://localhost:8080

The same trick would work if you used a local copy of the portal code served
from your own machine or servers.

## Licenses

Usergrid is licensed under the Apache License, Version 2.


