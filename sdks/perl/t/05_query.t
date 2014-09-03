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

use strict;
use warnings;
use Usergrid::Client;
use IO::Socket::INET;
use Test::More;

# TEST DATA
my $hostname        = 'localhost';
my $port            = '8080';
my $api_url         = "http://$hostname:$port";
my $organization    = 'test-organization';
my $application     = 'test-app';
my $username        = 'testuser';
my $password        = 'Testuser123$';
###########

if (_check_port($hostname, $port)) {
  plan tests => 8;
} else {
  plan skip_all => "server $api_url not reachable"
}

sub _check_port {
  my ($hostname, $port) = @_;
  new IO::Socket::INET ( PeerAddr => $hostname, PeerPort => $port,
    Proto => 'tcp' ) || return 0;
  return 1;
}

my ($user, $token, $book, $collection, $subset);

# Create the client object that will be used for all subsequent requests
my $client = Usergrid::Client->new(
  organization => $organization,
  application  => $application,
  api_url      => $api_url,
  trace        => 0
);

# Create a test user
$user = $client->add_entity("users", { username=>$username, password=>$password });

$token = $client->login($username, $password);

eval {

  $collection = $client->get_collection("books");

  ok ( $collection->count() == 0, "count must be initially zero" );

  for (my $i = 0; $i < 30; $i++) {
    $client->add_entity("books", { name => "book $i", index => $i });
  }

  $collection = $client->get_collection("books", 30);

  ok ( $collection->count() == 30, "count must now be 30" );

  $book = $collection->get_first_entity();

  ok ( $book->get('index') eq '0', "first index should be 0");

  $book = $collection->get_last_entity();

  ok ( $book->get('index') eq '29', "last index should be 29");

  $subset = $client->query_collection("books", "select * where index = '5'", 15 );

  ok ( $subset->count() == 1, "subset is 1" );
  ok ( $subset->object->{'params'}->{'limit'}[0] eq '15', "check limit override" );

  $book = $subset->get_next_entity();

  ok ( $book->get('index') eq '5', "query returned the fifth book" );

  while ($collection->has_next_entity()) {
    $book = $collection->get_next_entity();
    $client->delete_entity($book);
  }

  $collection = $client->get_collection("books");

  ok ( $collection->count() == 0, "count must now be again zero" );

};

diag($@) if $@;

# Cleanup
$collection = $client->delete_collection("books", undef, 30);
$client->delete_entity($user);
