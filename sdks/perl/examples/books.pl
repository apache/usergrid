#!/usr/bin/perl

# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

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
