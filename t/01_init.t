#!/usr/bin/perl
use strict;
use warnings;

use Test::More tests => 5;
use JSON;

my $json = JSON->new->allow_nonref;
my $resp;

BEGIN {
  use_ok 'Usergrid::Client' || print "Bail out!\n";
}

my $client = Usergrid::Client->new(
  organization => 'test-organization',
  application => 'test-app',
  api_url => 'http://localhost:8080/ROOT'
);

my %user = (username=>'testuser',password=>'1QAZ2wsx');
my $user_obj = $client->create("users", \%user);

ok( length($user_obj->{'entities'}[0]->{'uuid'}) > 0, 'create user' );

$resp = $client->login('testuser', '1QAZ2wsx');

ok( length($resp->{'access_token'}) > 0, 'login' );

$resp = $client->retrieve_by_id("user", $resp->{'entities'}[0]->{'uuid'});

ok( length($resp->{'entities'}[0]->{'uuid'}) > 0, 'retrieve user entity by id' );

$client->admin_login('admin', 'admin');

$resp = $client->delete("users", $user_obj->{'entities'}[0]->{'uuid'});

$resp = $client->retrieve_by_id("user", $resp->{'entities'}[0]->{'uuid'});

diag($json->pretty->encode($resp));

ok( ! defined($resp), 'user deleted' );
