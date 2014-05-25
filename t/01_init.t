#!/usr/bin/perl
use strict;
use warnings;

use Test::More tests => 7;

# TEST DATA
my $api_url         = 'http://localhost:8080/ROOT';
my $organization    = 'test-organization';
my $application     = 'test-app';
my $username        = 'testuser';
my $password        = '1QAZ2wsx';
my $admin_username  = 'admin';
my $admin_password  = 'admin';
my $test_collection = 'collection_foo';
###########

my ($resp, @to_delete);

BEGIN {
  use_ok 'Usergrid::Client' || print "Bail out!\n";
}

# Create the client object that will be used for all subsequent requests
my $client = Usergrid::Client->new(
  organization => $organization,
  application => $application,
  api_url => $api_url
);

# Create a test user
my $user = $client->create("users", { username=>$username, password=>$password });
ok( length($user->{'entities'}[0]->{'uuid'}) > 0, 'create user' );

# Log the test user in
$resp = $client->login($username, $password);
ok( length($resp->{'access_token'}) > 0, 'login' );

# Retrieve the user details by UUID
$resp = $client->retrieve_by_id("user", $resp->{'entities'}[0]->{'uuid'});
ok( length($resp->{'entities'}[0]->{'uuid'}) > 0, 'retrieve user entity by id' );

# Retrieve all from a test collection
$resp = $client->retrieve("collection_foo");
my $old_count = scalar @{$resp->{entities}};

# Create two entities in test collection
$resp = $client->create($test_collection, { name=> "bar", type=>"fruit" });
push (@to_delete, $resp->{'entities'}[0]->{'uuid'});
$resp = $client->create($test_collection, { name=> "baz", type=>"not-a-fruit" });
push (@to_delete, $resp->{'entities'}[0]->{'uuid'});

# Retrieve all from test collection to check whether the entities are created
$resp = $client->retrieve("collection_foo");
my $new_count = scalar @{$resp->{entities}};
ok( $new_count == $old_count + 2, 'added two entities' );

# Delete the two created entities
my $uuid;
foreach $uuid (@to_delete) {
  $client->delete($test_collection, $uuid);
}

# Confirm the count again in the test collection
$resp = $client->retrieve($test_collection);
my $after_delete_count = scalar @{$resp->{entities}};
ok ( $after_delete_count == $old_count, 'deleted two entities' );

# Get a management token and delete the test user
$client->management_login($admin_username, $admin_password);
$resp = $client->delete("users", $user->{'entities'}[0]->{'uuid'});

# Try to get the test user by UUID again and confirm it doesn't exist
$resp = $client->retrieve_by_id("user", $resp->{'entities'}[0]->{'uuid'});
ok( ! defined($resp), 'user deleted' );
