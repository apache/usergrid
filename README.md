Apache Usergrid
===

**Apache Usergrid is a multi-tenant Backend-as-a-Service stack for web & mobile applications, based on RESTful APIs. It is [currently incubating at the Apache Software Foundation](http://usergrid.incubator.apache.org/).**

This repository contains all the code for Apache Usergrid, including the server stack, portal, client and SDKs. Each of them have their own, much more detailed README in the corresponding subdirectories.

* The server-side stack, a Java 7 + Cassandra codebase that powers all of the features, is located under [`/stack`](stack). You can install dependencies and compile it with maven. See [stack/README.md](stack#requirements) for instructions.
* a command-line client “ugc” allowing you to complete most maintenance tasks, as well as queries in a manner similar to the mysql or the mongo shell, located under [`/ugc`](ugc). You can install it on your machine with a simple `sudo gem install ugc`
* the admin portal and the many SDKs. A pure HTML5+JavaScript app allowing you to register developers and let them manage their apps in a multi-tenant cluster. Located under [`/portal`](portal)
* SDKs for [iOS](sdks/ios), [Android](sdks/android), [HTML5/JavaScript](sdks/html5-javascript), [node.js](sdks/nodejs), [Ruby on Rails](ruby-on-rails), [pure Ruby](sdks/ruby), [PHP](sdks/php), (server-side) [Java](sdks/java) and [.Net / Windows](sdks/dotnet), located in their respective subdirectories under [`/sdks`](sdks).

How to contribute to Apache Usergrid
===
See the [How to Contribute](https://usergrid.incubator.apache.org/docs/contribute-code/) page on our website.

For more information
===
See the [Apache Usergrid website](https://usergrid.incubator.apache.org/).




