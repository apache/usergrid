#!/usr/bin/perl
use strict;
use warnings;

use Test::More tests => 10;

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

my ($resp, @to_delete, $uuid, $collection, $entity);

BEGIN {
  use_ok 'Usergrid::Client'     || print "Bail out!\n";
  use_ok 'Usergrid::Entity'     || print "Bail out!\n";
  use_ok 'Usergrid::Collection' || print "Bail out!\n";
}

# Create the client object that will be used for all subsequent requests
my $client = Usergrid::Client->new(
  organization => $organization,
  application => $application,
  api_url => $api_url,
  trace => 0
);

# Create a test user
my $user = $client->create("users", { username=>$username, password=>$password });
ok( length($user->get('uuid')) > 0, 'create user' );

# Log the test user in
$client->login($username, $password);

# Retrieve the user details by UUID
$entity = $client->retrieve_by_id("user", $user->get('uuid'));
ok( length($entity->get('uuid')) > 0, 'retrieve user entity by id' );

# Retrieve all from a test collection
$collection = $client->retrieve("collection_foo");
my $old_count = $collection->count();

# Create two entities in test collection
$entity = $client->create($test_collection, { name=> "bar", coll_type=>"fruit" });
push (@to_delete, $entity->get('uuid'));

$entity = $client->create($test_collection, { name=> "baz", coll_type=>"not-a-fruit" });
push (@to_delete, $entity->get('uuid'));

# Check value of attribute before modifying
$uuid = $entity->get('uuid');
ok( $entity->get('coll_type') eq 'not-a-fruit', "check value before PUT" );

$entity->set('coll_type', 'fruit');
$client->update($test_collection, $uuid, $entity);

$entity = $client->retrieve_by_id($test_collection, $uuid);
ok( $entity->get('coll_type') eq 'fruit', "check value after PUT");

# Retrieve all from test collection to check whether the entities are created
$collection = $client->retrieve("collection_foo");
my $new_count = $collection->count();
ok( $new_count == $old_count + 2, 'added two entities' );

# Delete the two created entities
foreach $uuid (@to_delete) {
  $client->delete($test_collection, $uuid);
}

# Confirm the count again in the test collection
$collection = $client->retrieve($test_collection);
my $after_delete_count = $collection->count();
ok ( $after_delete_count == $old_count, 'deleted two entities' );

# Get a management token and delete the test user
$client->management_login($admin_username, $admin_password);
$entity = $client->delete("users", $user->get('uuid'));

# Try to get the test user by UUID again and confirm it doesn't exist
$entity = $client->retrieve_by_id("user", $entity->get('uuid'));
ok( ! defined($entity->object), 'user deleted' );
