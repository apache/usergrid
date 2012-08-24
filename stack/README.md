# Usergrid
A highly-scalable data platform for mobile applications.

* **Documentation**: http://apigee.com/docs/usergrid/
* **Homepage**: http://apigee.com/about/products/usergrid
* **Google Group**: http://groups.google.com/group/usergrid

## Requirements

* JDK 1.6 (http://www.oracle.com/technetwork/java/javase/downloads/index.html)
* Maven (http://maven.apache.org/)

## Building

From the command line, go to the usergrid directory and type the following:

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

## Getting Started with the Admin Portal

By default, the [Usergrid admin portal](https://github.com/apigee/usergrid-portal) points to our production servers at `api.usergrid.com`. However, by specifying an api_url argument in the query string, you can have it point to
your local instance instead. For example, you could reuse the version of the admin portal we host on github and have that point to your local cluster by opening the following URL in your browser:
`http://apigee.github.com/usergrid-portal/?api_url=http://localhost:8080`

The same trick would work if you used a local copy of the portal code served from your own machine or servers.


## Getting Started with the HTTP API

Start by creating an Organization. It’s the top-level structure in Usergrid:
all Apps and Administrators must belong to an Organziation. Here’s how you create one:

    curl -X POST  \
         -d 'organization=myfirstorg&username=myadmin&name=Admin&email=admin@example.com&password=password' \
         http://localhost:8080/management/organizations

You can see that creating an Organization creates an Administrator in the process. Let’s authenticate as him:
curl 'http://localhost:8080/management/token?grant_type=password&username=nate&password=password'

This will return an access\_token. We’ll use this to authenticate the next two calls.
Next, let’s create an Application:

    curl -H "Authorization: Bearer [the management token from above]" \
         -H "Content-Type: application/json" \
         -X POST -d '{ "name":"myapp" }' \
         http://localhost:8080/management/orgs/myfirstorg/apps

… And a User for the Application:

    curl -H "Authorization: Bearer [the management token from above]" \
         -X POST "http://localhost:8080/myfirstorg/myapp/users" \
         -d '{ "username":"myuser", "password":"password", "email":"user@example.com" }'

Let’s now generate an access token for this Application User:
    curl 'http://localhost:8080/myfirstorg/myapp/token?grant_type=password&username=myuser&password=mypassword'

This will also send back an access\_token, but limited in scope.
Let’s use it to create a collectioon with some data in it:

    curl -H "Authorization: Bearer [the user token]" \
         -X POST -d '[ { "cat":"fluffy" }, { "fish": { "gold":2, "oscar":1 } } ]' \
         http://localhost:8080/myfirstorg/myapp/pets

## Contributing

We welcome all contributions! If you want to submit code, please submit a pull request to [apigee/usergrid-stack](https://github.com/apigee/usergrid-stack/), using a [topic branch](http://git-scm.com/book/en/Git-Branching-Branching-Workflows).

We’d prefer if your commit messages referenced the issue at hand (if applicable). We don’t have particular guidelines for commit messages but we appreciate branch names that observe the following format: `issue#-singleworddescription` (i.e. `325-twitter`) or just a single word if no issue exists on the topic. Thanks!

## Licenses

Usergrid is licensed under the Apache License, Version 2.


