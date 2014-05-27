#!/usr/bin/perl
use strict;
use warnings;

use Test::More tests => 11;

# TEST DATA
my $api_url         = 'http://localhost:8080';
my $organization    = 'test-organization';
my $application     = 'test-app';
my $username        = 'testuser';
my $password        = 'Testuser123$';
###########

my ($user, $token, $book, $collection);

BEGIN {
  use_ok 'Usergrid::Client'     || print "Bail out!\n";
}

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

  ok ( $collection->count() == 0, "count must be initially zero");

  $client->add_entity("books", { name => "Ulysses", author => "James Joyce" });
  $client->add_entity("books", { name => "Neuromancer", author => "William Gibson" });
  $client->add_entity("books", { name => "On the Road", author => "Jack Kerouac" });
  $client->add_entity("books", { name => "Ubik", author => "Philip K. Dick" });
  $client->add_entity("books", { name => "Reef", author => "Romesh Gunasekera" });

  $collection = $client->get_collection("books");

  ok ( $collection->count() == 5, "count must now be five");

  while ($collection->has_next_entity()) {
    $book = $collection->get_next_entity();
    ok ( length($book->get('name')) > 3, "check the book titles");
  }

  $collection->reset_iterator();

  ok ( $collection->iterator == -1, "iterator must be reset");

  ok ( $collection->count() == 5, "count must be five");

  while ($collection->has_next_entity()) {
    $book = $collection->get_next_entity();
    $client->delete_entity($book);
  }

  $collection = $client->get_collection("books");

  ok ( $collection->count() == 0, "count must now be again zero");

};

diag($@) if $@;

# Cleanup
$client->delete_entity($book);
$client->delete_entity($user);
