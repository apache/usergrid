# Usegrid 1: Launcher Quick-start

## Requirements

* [JDK 1.7](http://www.oracle.com/technetwork/java/javase/downloads/index.html)
* [Maven](http://maven.apache.org/)

## Download

### Download2

Start by [downloading our latest code](https://github.com/apache/usergrid/archive/master.zip) and extract it.

#### Building 3

From the command line, navigate to stack directory and type the following:

    mvn clean install -DskipTests=true

## Running

Usergrid-core contains the persistence layer and shared utilities for powering the Usergrid service. The services layer is contained in usergrid-services and exposes a higher-level API that's used by the usergrid-rest web services tier.

You can run Usergrid from the command-line from the
jar in the usergrid/standalone project:

    cd launcher; java -jar target/usergrid-launcher-*.jar

After startup, your instance will be available on localhost, port 8080.
To check it’s running properly, you can try loading our status page:

    curl http://localhost:8080/status

You can also run it as a webapp in Tomcat, by deploying the ROOT.war file generated in the usergrid/rest project.

## Getting Started with the HTTP API

Start by creating an Organization. It’s the top-level structure in Usergrid:
all Apps and Administrators must belong to an Organization. Here’s how you create one:

    curl -X POST  \
         -d 'organization=myfirstorg&username=myadmin&name=Admin&email=admin@example.com&password=password' \
         http://localhost:8080/management/organizations

You can see that creating an Organization creates an Administrator in the process. Let’s authenticate as him:

    curl 'http://localhost:8080/management/token?grant_type=password&username=myadmin&password=password'

This will return an access\_token. We’ll use this to authenticate the next two calls.
Next, let’s create an Application:

    curl -H "Authorization: Bearer [the management token from above]" \
         -H "Content-Type: application/json" \
         -X POST -d '{ "name":"myapp" }' \
         http://localhost:8080/management/orgs/myfirstorg/apps

… And a User for the Application:

    curl -H "Authorization: Bearer [the management token from above]" \
         -X POST "http://localhost:8080/myfirstorg/myapp/users" \
         -d '{ "username":"myuser", "password":"mypassword", "email":"user@example.com" }'

Let’s now generate an access token for this Application User:

    curl 'http://localhost:8080/myfirstorg/myapp/token?grant_type=password&username=myuser&password=mypassword'

This will also send back an access\_token, but limited in scope.
Let’s use it to create a collection with some data in it:

    curl -H "Authorization: Bearer [the user token]" \
         -X POST -d '[ { "cat":"fluffy" }, { "fish": { "gold":2, "oscar":1 } } ]' \
         http://localhost:8080/myfirstorg/myapp/pets
