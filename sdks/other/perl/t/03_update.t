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
  plan tests => 7;
} else {
  plan skip_all => "server $api_url not reachable"
}

sub _check_port {
  my ($hostname, $port) = @_;
  new IO::Socket::INET ( PeerAddr => $hostname, PeerPort => $port,
    Proto => 'tcp' ) || return 0;
  return 1;
}

my ($user, $token, $book, $deleted_book);

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

  ok ( $token->{user}->{username} eq $username, "user logged in" );

  $book = $client->add_entity("books", { name => "Ulysses", author => "James Joyce" });

  ok ( $book->get('author') eq "James Joyce", "check entity creation" );

  $book->set('genre', 'Modernist');

  $book = $client->update_entity($book);

  ok ( $book->get('genre') eq "Modernist", "check for new attribute" );

  $book->set('genre', 'Novel');

  $book = $client->update_entity($book);

  ok ( $book->get('genre') eq "Novel", "check for updated attribute" );

  $book = $client->get_entity("books", $book->get('uuid'));

  ok ( $book->get('genre') eq "Novel", "check again for updated attribute by uuid" );

  $book = $client->get_entity("books", "Ulysses");

  ok ( $book->get('name') eq 'Ulysses', "get object by name") ;

  $book = $client->delete_entity_by_id("books", $book->get('uuid'));

  $deleted_book = $client->get_entity("books", $book->get('uuid'));

  ok ( (! defined $deleted_book), "deleted book cannot be found" );

};

diag($@) if $@;

# Cleanup
$client->delete_entity($book);
$client->delete_entity($user);
