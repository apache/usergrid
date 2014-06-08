#!/usr/bin/perl
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
  plan tests => 5;
} else {
  plan skip_all => "server $api_url not reachable"
}

sub _check_port {
  my ($hostname, $port) = @_;
  new IO::Socket::INET ( PeerAddr => $hostname, PeerPort => $port,
    Proto => 'tcp' ) || return 0;
  return 1;
}

my ($user, $token, $book, $book1, $book2, $book3, $collection);

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
  $book1 = $client->add_entity("books", { name => "Neuromancer", author => "William Gibson" });
  $book2 = $client->add_entity("books", { name => "Count Zero", author => "William Gibson" });
  $book3 = $client->add_entity("books", { name => "Mona Lisa Overdrive", author => "William Gibson" });

  $client->connect_entities($book1, "similar_to", $book2);
  $client->connect_entities($book1, "similar_to", $book3);

  $collection = $client->query_connections($book1, "similar_to");

  ok ( $collection->count == 2, "two connections must exist" );

  while ($collection->has_next_entity()) {
    $book = $collection->get_next_entity();
    ok ( $book->get('name') eq 'Count Zero' || $book->get('name') eq ('Mona Lisa Overdrive'), "check connections");
  }

  $client->disconnect_entities($book1, "similar_to", $book2);

  $collection = $client->query_connections($book1, "similar_to");

  ok ( $collection->count() == 1 );

  $book = $collection->get_next_entity();

  ok ( $book->get('name') eq 'Mona Lisa Overdrive', "check remaining connection");
};

diag($@) if $@;

# Cleanup
$client->delete_collection("books", undef, 10);
$client->delete_entity($user);
