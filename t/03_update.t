#!/usr/bin/perl
use strict;
use warnings;

use Test::More tests => 7;

# TEST DATA
my $api_url         = 'http://localhost:8080';
my $organization    = 'test-organization';
my $application     = 'test-app';
my $username        = 'testuser';
my $password        = 'Testuser123$';
###########

my ($user, $token, $book, $deleted_book);

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

  ok ( $token->{user}->{username} eq $username, "user logged in" );

  $book = $client->add_entity("books", { name => "Ulysses", author => "James Joyce" });

  ok ( $book->get('author') eq "James Joyce", "check entity creation");

  $book->set('genre', 'Modernist');

  $book = $client->update_entity($book);

  ok ( $book->get('genre') eq "Modernist", "check for new attribute");

  $book->set('genre', 'Novel');

  $book = $client->update_entity($book);

  ok ( $book->get('genre') eq "Novel", "check for updated attribute");

  $book = $client->get_entity_by_uuid("books", $book->get('uuid'));

  ok ( $book->get('genre') eq "Novel", "check again for updated attribute by uuid");

  $book = $client->delete_entity_by_uuid("books", $book->get('uuid'));

  $deleted_book = $client->get_entity_by_uuid("books", $book->get('uuid'));

  ok ( (! defined $deleted_book), "deleted book cannot be found")

};

diag($@) if $@;

# Cleanup
$client->delete_entity($book);
$client->delete_entity($user);
