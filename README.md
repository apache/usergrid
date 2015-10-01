Apache Usergrid
===============

__WARNING__: The Usergrid master branch represents and un-released Usergrid 2.x version with a completely new persistence and query engine. This new engine requires some new components. In addition to a Cassandra cluster, Usergrid 2.x also requires an ElasticSearch cluster and a distributed Queue system (currently only AWS SQS is supported). 

Overview
--------

**Apache Usergrid is a multi-tenant Backend-as-a-Service stack for web & mobile applications, based on RESTful APIs.**

## Contributing

We accept all contributions via our GitHub, so you can fork our repo (apache/usergrid) and then submit a PR back to us for approval. For larger PRs you'll need to have an ICLA form on file with Apache. For more information see our [Contribution Workflow Policy](https://cwiki.apache.org/confluence/display/usergrid/Usergrid+Contribution+Workflow), and specifically our [External Contributors Guide](https://cwiki.apache.org/confluence/display/usergrid/Usergrid+External+Contributors+Guide).

## Build awesome apps with Usergrid!

Apache Usergrid provides all code necessary to build and power modern mobile applications.  This includes the server stack, administrative portal website, SDKs in most popular languages, as well as command line tools. 

Look for much more detailed README files in their corresponding subdirectories, or check out [our website](http://usergrid.apache.org/) for more info.

* The server-side stack, a Java 7 + Cassandra codebase that powers all of the features, is located under [`/stack`](stack). You can install dependencies and compile it with maven. See [stack/README.md](stack#requirements) for instructions.

* The admin portal is a pure HTML5+JavaScript app allowing you to register developers and let them manage their apps in a multi-tenant cluster. Located under [`/portal`](portal)

* SDKs for [iOS](sdks/ios), [Android](sdks/android), [HTML5/JavaScript](sdks/html5-javascript), [node.js](sdks/nodejs), [Ruby on Rails](ruby-on-rails), [pure Ruby](sdks/ruby), [PHP](sdks/php), (server-side) [Java](sdks/java), [.Net / Windows](sdks/dotnet), and [Perl](sdks/perl), located in their respective subdirectories under [`/sdks`](sdks).

* a command-line client “ugc” allowing you to complete most maintenance tasks, as well as queries in a manner similar to the mysql or the mongo shell, located under [`/ugc`](ugc). You can install it on your machine with a simple `sudo gem install ugc`

## For more information

See the [Apache Usergrid web site](http://usergrid.apache.org/).

