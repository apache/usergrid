use strict;
use warnings;

use Test::More tests => 4;

our $json = JSON->new->allow_nonref;

BEGIN {
  use_ok 'Usergrid::Client' || print "Bail out!\n";
}

my $client = Usergrid::Client->new(
  organization => 'test-organization',
  application => 'test-app',
  api_url => 'http://localhost:8080/ROOT'
);

my $resp = $client->admin_login('admin', 'admin');

ok( length($resp->{'access_token'}) > 0, 'admin login' );

my %user = (username=>'testuser',password=>'1QAZ2wsx');
my $user_obj = $client->create("users", \%user);

ok( length($user_obj->{'entities'}[0]->{'uuid'}) > 0, 'create user' );

$resp = $client->app_user_login('testuser', '1QAZ2wsx');

ok( length($resp->{'access_token'}) > 0, 'app user login' );

$resp = $client->delete("users", $user_obj->{'entities'}[0]->{'uuid'});
