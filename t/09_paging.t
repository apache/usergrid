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
  plan tests => 14;
} else {
  plan skip_all => "server $api_url not reachable"
}

sub _check_port {
  my ($hostname, $port) = @_;
  new IO::Socket::INET ( PeerAddr => $hostname, PeerPort => $port,
    Proto => 'tcp' ) || return 0;
  return 1;
}

my ($user, $token, $book, $collection, $subset, $count);

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

  $collection = $client->get_collection("books", 10);

  ok ( $collection->count == 10, "checking first page count" );
  ok ( $collection->get_first_entity()->get('name') eq 'book 0', "Check first entity of first page");
  ok ( $collection->get_last_entity()->get('name') eq 'book 9', "Check last entity of first page");

  $collection = $collection->get_next_page();

  ok ( $collection->count == 10, "checking second page count" );
  ok ( $collection->get_first_entity()->get('name') eq 'book 10', "Check first entity of second page");
  ok ( $collection->get_last_entity()->get('name') eq 'book 19', "Check last entity of second page");

  $collection = $collection->get_next_page();

  ok ( $collection->count == 10, "checking third page count" );
  ok ( $collection->get_first_entity()->get('name') eq 'book 20', "Check first entity of third page");
  ok ( $collection->get_last_entity()->get('name') eq 'book 29', "Check last entity of third page");

  $collection = $collection->get_next_page();

  ok ( $collection->count == 0, "no more results" );

  $count = 0;
  $collection = $client->get_collection("books", 10);
  do {
    $count += $collection->count();
    $collection = $collection->get_next_page();
  } while ($collection->count != 0);

  ok ( $count == 30, "should return 30 entities" );

  $client->delete_collection("books", undef, 30);

  $collection = $client->get_collection("books");

  ok ( $collection->count() == 0, "count must now be again zero" );

};

diag($@) if $@;

# Cleanup
$client->delete_entity($user);
