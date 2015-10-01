# Usergrid Perl Client ![CI status](https://travis-ci.org/aweeraman/usergrid-perl-sdk.svg?branch=master "CI Status")

Usergrid::Client provides an easy to use Perl API for Apache Usergrid.

## Quickstart

Install Usergrid::Client from CPAN:

    $ sudo cpan Usergrid::Client

Write a perl script that uses the Perl API to talk to Usergrid. Here's an example:

```perl
#!/usr/bin/perl
use Usergrid::Client;

# Create a client object for Usergrid that's used for subsequent activity
my $client = Usergrid::Client->new(
  organization => 'test-organization',
  application  => 'test-app',
  api_url      => 'http://localhost:8080',
  trace        => 0
);

# Logs the user in. The security token is maintained by the library in memory
$client->login('johndoe', 'Johndoe123$');

# Add two entities to the "books" collection
$client->add_entity("books", { name => "Ulysses", author => "James Joyce" });
$client->add_entity("books", { name => "Neuromancer", author => "William Gibson" });

# Retrieve a handle to the collection
my $books = $client->get_collection("books");

# Add a new attribute for quantity in stock
while ($books->has_next_entity()) {
  my $book = $books->get_next_entity();

  print "Name: "   . $book->get('name')   . ", ";
  print "Author: " . $book->get('author') . "\n";

  # Create a new attribute and update the entity
  $book->set("in-stock", 0);
  $client->update_entity($book);
}
```

## What is Apache Usergrid

Usergrid is an open-source Backend-as-a-Service (“BaaS” or “mBaaS”) composed of
an integrated distributed NoSQL database, application layer and client tier with
SDKs for developers looking to rapidly build web and/or mobile applications.
It provides elementary services (user registration & management, data storage,
file storage, queues) and retrieval features (full text search, geolocation
search, joins) to power common app features.

It is a multi-tenant system designed for deployment to public cloud environments
(such as Amazon Web Services, Rackspace, etc.) or to run on traditional server
infrastructures so that anyone can run their own private BaaS deployment.

For architects and back-end teams, it aims to provide a distributed, easily
extendable, operationally predictable and highly scalable solution. For
front-end developers, it aims to simplify the development process by enabling
them to rapidly build and operate mobile and web applications without requiring
backend expertise.

Source: [Usergrid Documentation](https://usergrid.apache.org/docs/)

For more information, visit [http://www.usergrid.org](http://www.usergrid.org)

## Installation

### Prerequisites
Usergrid::Client depends on the following modules which can be installed
from CPAN as shown below:

    $ sudo cpan install Moose
    $ sudo cpan install JSON
    $ sudo cpan install REST::Client
    $ sudo cpan install URI::Template
    $ sudo cpan install Log::Log4perl
    $ sudo cpan install namespace::autoclean

### Build and install

    $ perl Build.PL
    $ ./Build
    $ ./Build test
    $ sudo ./Build install

### For legacy users on older versions of Perl

    $ perl Makefile.PL
    $ make
    $ make test
    $ sudo make install

## Usage

### Getting started

In order to login to Usergrid using the API, create a Usergrid::Client object
as shown below and invoke the login function.

```perl
# Create the client object that will be used for all subsequent requests
my $client = Usergrid::Client->new(
  organization => $organization,
  application  => $application,
  api_url      => $api_url
);

$client->login($username, $password);
```

For troubleshooting the requests and responses, tracing can be enabled, which
will log all requests and responses to standard output.

```perl
# Create the client object that will be used for all subsequent requests
my $client = Usergrid::Client->new(
  organization => $organization,
  application  => $application,
  api_url      => $api_url,
  trace        => 1
);
```

To get more details on the API, read the following perldocs:

    Usergrid::Client
    Usergrid::Collection
    Usergrid::Entity

### Entities

Creating and updating an entity is easy. Here's how:

```perl
$book = $client->add_entity("books", { name => "Neuromancer", author => "William Gibson" });
$book->set('genre', 'Cyberpunk');

$client->update_entity($book);
```

Querying an entity can be done by UUID:

```perl
$book = $client->get_entity("books", $uuid);
```

or by name:

```perl
$book = $client->get_entity("books", "Neuromancer");
```

Similarly, an entity can be deleted by UUID or by name:

```perl
$client->delete_entity_by_id("books", $uuid_or_name);

# An entity can be also deleted by passing an entity object
$client->delete_entity($entity);
```

### Collections

A collection can be retrieved as shown below:

```perl
$collection = $client->get_collection("books");

# Outputs the number of records in the collection
print "$collection->count()\n";
```

To iterate over the collection:

```perl
while ($collection->has_next_entity()) {
  $book = $collection->get_next_entity();
  print "$book->get('name')\n";
}
```

Note that by default a collection returns a maximum of 10 records per page. This
can be overridden when retrieving the collection as shown below:

```perl
$collection = $client->get_collection("books", 30);

# Retrieve the first book in the collection's current page
$first_book = $collection->get_first_entity();

# Retrieve the last book in the collection's current page
$last_book  = $collection->get_last_entity();
```

To navigate the pages in the collection:

```perl
$collection->get_next_page();

$collection->get_prev_page();
```

Both of the above return FALSE if the end or the beginning of the collection
is reached.

When iterating through a collection, the auto_page attribute can be set
to allow the collection to transparently fetch the next page when iterating.

```perl
$collection = $client->get_collection("books");

$collection->auto_page(1);

while ($collection->has_next_entity()) {
  my $entity = $collection->get_next_entity();
  # do something
}
```

### Querying & Batch Updates

Collections can be queried using a SQL-like query language for greater control
over the data set that is returned.

```perl
$collection = $client->query_collection("books", "select * where genre = 'Cyberpunk'", $limit );
```

Queries can also be used when deleting collections:

```perl
$collection = $client->delete_collection("books", "select * where genre = 'Cyberpunk'", $limit);
```

If the $limit is omitted in the above method calls, a default of 10 is assumed.

A collection can be batch updated as shown below:

```perl
$client->update_collection("books", { in_stock => 1 });
```

A query can be used to fine-tune the update:

```perl
$client->update_collection("books", { in_stock => 0 }, "select * where genre = 'Cyberpunk'", $limit);
```

Similarly, entities can be deleted in batch:

```perl
$client->delete_collection("books", "select * where genre = 'Novel'", $limit);
```

### Entity Connections

Connections can be created between entities through relationships as shown below:

```perl
$book1 = $client->add_entity("books", { name => "Neuromancer", author => "William Gibson" });
$book2 = $client->add_entity("books", { name => "Count Zero", author => "William Gibson" });
$book3 = $client->add_entity("books", { name => "Mona Lisa Overdrive", author => "William Gibson" });

$client->connect_entities($book1, "similar_to", $book2);
$client->connect_entities($book1, "similar_to", $book3);
```

They can also be queried just like any other collection:

```perl
$collection = $client->query_connections($book1, "similar_to");

# Queries and limits can also be passed in
$collection = $client->query_connections($book1, "similar_to", $query, $limit);
```

To delete a connection:

```perl
$client->disconnect_entities($book1, "similar_to", $book2);
```

### Code Coverage

Code coverage reporting requires Devel::Cover module which can be
installed from CPAN as shown:

    $ sudo cpan install Devel::Cover

For generating reports on code coverage:

    $ ./Build testcover

The generated report artifacts are located in cover_db/.

## Release notes

### 0.22

* Auto paging for collections

### 0.21

* Documentation updates

### 0.2

* Creating, querying and deleting entity connections
* Bi-directional collection pagination

### 0.11

* Added namespace::autoclean.pm as a dependency to fix build break on some
  platforms

### 0.1

* Initial release
* Application and admin authentication
* Support for collections and entities (CRUD & queries)

## License
Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements.  See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
the ASF licenses this file to You under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License.  You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
