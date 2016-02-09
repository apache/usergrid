# Apache Usergrid

A highly-scalable data platform for mobile applications.

* **Documentation**: http://usergrid.apache.org/docs/
* **Homepage**: http://http://usergrid.apache.org/


## Requirements

* JDK 1.8 (http://www.oracle.com/technetwork/java/javase/downloads/index.html)
* 3.0.0 <= Maven (http://maven.apache.org/)
* Cassandra 1.2.1+
* ElasticSearch 1.4.4+


## Building

From the command line, go to the usergrid directory and type the following:

    mvn clean install -DskipTests=true

## Running

The build process will package the Usergrid Stack into one file `stack/rest/target/ROOT.war`

To run Usergrid Stack you will need to deploy it to Tomcat. You can find instructions for
doing that in the [Usergrid Deployment Guide](http://usergrid.apache.org/docs/installation/deployment-guide.html).


## Upgrading from Previous Versions

There is currently no upgrade path for a Usergrid 1 database to Usergrid 2.x.


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

## Contributing

We welcome all contributions, including via pull requests on GitHub! If you want to submit code, please read more about our [contribution workflow](https://cwiki.apache.org/confluence/display/usergrid/GitHub+Based+Contribution+Workflow)


## Licenses

Usergrid is licensed under the Apache License, Version 2.
