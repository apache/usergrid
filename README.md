# Usergrid Perl Client

Usergrid::Client provides a modular object oriented interface to
Apache Usergrid's REST API.

## Quickstart

Install Usergrid::Client from CPAN (not yet available):

    $ sudo cpan install Usergrid::Client

Write a perl script that uses the Perl API to talk to Usergrid. Here's an example:

```perl
#!/usr/bin/perl
use Usergrid::Client;

# Create a client object for Usergrid that's used for subsequent activity
my $client = Usergrid::Client->new(
  organization => 'test-organization',
  application  => 'test-app',
  api_url      => 'http://localhost:8080/ROOT',
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

## Installation

### Prerequisites
Usergrid::Client depends on the following modules which can be installed
from CPAN as shown below:

    $ sudo cpan install Moose
    $ sudo cpan install JSON
    $ sudo cpan install REST::Client
    $ sudo cpan install URI::Template
    $ sudo cpan install Log::Log4perl

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
    Usergrid::Core
    Usergrid::Collection
    Usergrid::Entity

### Code Coverage

Code coverage reporting requires Devel::Cover module which can be
installed from CPAN as shown:

    $ sudo cpan install Devel::Cover

For generating reports on code coverage:

    $ ./Build testcover

The generated report artifacts are located in cover_db/.

## Release notes

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
