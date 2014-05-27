#!/usr/bin/perl
use strict;
use warnings;

use Test::More tests => 2;

# TEST DATA
my $api_url         = 'http://localhost:8080';
my $organization    = 'test-organization';
my $application     = 'test-app';
my $username        = 'testuser';
my $password        = 'Testuser123$';
###########

my ($user, $token);

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

ok ( $token->{user}->{username} eq $username, "user logged in" );

$client->delete_entity($user);
