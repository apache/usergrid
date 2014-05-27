#!/usr/bin/perl
use strict;
use warnings;

use Test::More tests => 9;

# TEST DATA
my $api_url         = 'http://localhost:8080';
my $organization    = 'test-organization';
my $application     = 'test-app';
my $username        = 'testuser';
my $password        = '1QAZ2wsx';
my $admin_username  = 'superuser';
my $admin_password  = 'superuser';
my $test_collection = 'collection_foo';
###########

my ($resp, @to_delete, $del, $uuid, $collection, $entity);

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
my $user = $client->add_entity("users", { username=>$username, password=>$password });
ok( length($user->get('uuid')) > 0, 'create user' );

# Log the test user in
$client->login($username, $password);

# Retrieve the user details by UUID
$entity = $client->get_entity_by_uuid("user", $user->get('uuid'));
ok( length($entity->get('uuid')) > 0, 'retrieve user entity by id' );

# Retrieve all from a test collection
$collection = $client->get_collection("collection_foo");
my $old_count = $collection->count();

# Create two entities in test collection
$entity = $client->add_entity($test_collection,
  { name=> "bar", coll_type=>"fruit" });
push (@to_delete, $entity);

$entity = $client->add_entity($test_collection,
  { name=> "baz", coll_type=>"not-a-fruit" });
push (@to_delete, $entity);

# Check value of attribute before modifying
$uuid = $entity->get('uuid');
ok( $entity->get('coll_type') eq 'not-a-fruit', "check value before PUT" );

$entity->set('coll_type', 'fruit');
$client->update_entity($entity);

$entity = $client->get_entity_by_uuid($test_collection, $uuid);
ok( $entity->get('coll_type') eq 'fruit', "check value after PUT");

# Retrieve all from test collection to check whether the entities are created
$collection = $client->get_collection("collection_foo");

my $new_count = $collection->count();
ok( $new_count == $old_count + 2, 'added two entities' );

# Iterate through the collection
my $count = 0;
while ($collection->has_next_entity()) {
  $count++;
  my $ent = $collection->get_next_entity();
}

ok ( $count == $new_count, 'iterating through the collection');

# Delete the two created entities
foreach $del (@to_delete) {
  $client->delete_entity($del);
}

# Confirm the count again in the test collection
$collection = $client->get_collection($test_collection);
my $after_delete_count = $collection->count();
ok ( $after_delete_count == $old_count, 'deleted two entities' );

# Get a management token and delete the test user
$client->management_login($admin_username, $admin_password);
$entity = $client->delete_entity_by_uuid("users", $user->get('uuid'));

# Try to get the test user by UUID again and confirm it doesn't exist
$entity = $client->get_entity_by_uuid("user", $entity->get('uuid'));
ok( ! defined($entity->object), 'user deleted' );
