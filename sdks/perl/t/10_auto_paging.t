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
  plan tests => 3;
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

  $collection = $client->get_collection("books");

  ok ( $collection->get_last_entity()->get('name') eq 'book 9', "only return 10 entites per page by default");

  $collection = $client->get_collection("books");

  $collection->auto_page(1);

  $count = 0;

  while ($collection->has_next_entity()) {
    $count += 1;
    my $entity = $collection->get_next_entity();
  }

  ok ( $count == 30, "should return 30 entities in when auto paging" );
};

diag($@) if $@;

# Cleanup
$client->delete_collection("books", undef, 30);
$client->delete_entity($user);
