#!/usr/bin/perl
use strict;
use warnings;

use Test::More tests => 7;
use JSON;

my $json = JSON->new->allow_nonref;
my $resp;

BEGIN {
  use_ok 'Usergrid::Client' || print "Bail out!\n";
}

# Create the client object that will be used for all subsequent requests
my $client = Usergrid::Client->new(
  organization => 'test-organization',
  application => 'test-app',
  api_url => 'http://localhost:8080/ROOT'
);

# Create a test user
my $user = $client->create("users", { username=>'testuser', password=>'1QAZ2wsx' });
ok( length($user->{'entities'}[0]->{'uuid'}) > 0, 'create user' );

# Log the test user in
$resp = $client->login('testuser', '1QAZ2wsx');
ok( length($resp->{'access_token'}) > 0, 'login' );

# Retrieve the user details by UUID
$resp = $client->retrieve_by_id("user", $resp->{'entities'}[0]->{'uuid'});
ok( length($resp->{'entities'}[0]->{'uuid'}) > 0, 'retrieve user entity by id' );

$resp = $client->retrieve("collection_foo");
my $old_count = scalar @{$resp->{entities}};

my @to_delete;
$resp = $client->create("collection_foo", { name=> "bar", type=>"fruit" });
push (@to_delete, $resp->{'entities'}[0]->{'uuid'});

$resp = $client->create("collection_foo", { name=> "baz", type=>"not-a-fruit" });
push (@to_delete, $resp->{'entities'}[0]->{'uuid'});

$resp = $client->retrieve("collection_foo");
my $new_count = scalar @{$resp->{entities}};

ok( $new_count == $old_count + 2, 'added two entities' );
my $uuid;

foreach $uuid (@to_delete) {
  $resp = $client->delete("collection_foo", $uuid);
}

$resp = $client->retrieve("collection_foo");
my $after_delete_count = scalar @{$resp->{entities}};

ok ( $after_delete_count == $old_count, 'deleted two entities' );

# Get a management token and delete the test user
my $tok = $client->management_login('admin', 'admin');
$resp = $client->delete("users", $user->{'entities'}[0]->{'uuid'});

# Try to get the test user by UUID again and confirm it doesn't exist
$resp = $client->retrieve_by_id("user", $resp->{'entities'}[0]->{'uuid'});
ok( ! defined($resp), 'user deleted' );
