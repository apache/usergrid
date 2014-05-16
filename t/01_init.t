use strict;
use warnings;

use Test::More tests => 4;

use_ok 'Usergrid::Client';

my $client = Usergrid::Client->new(
  organization => 'test-organization',
  application => 'test-app',
  api_url => 'http://localhost:8080/ROOT'
);

my $resp = $client->admin_login('admin', 'admin');

ok( length($resp->{'access_token'}) > 0, 'admin login' );

my %user = (username=>'testuser',password=>'1QAZ2wsx');
$resp = $client->create("users", \%user);

ok( length($resp->{'application'}) > 0, 'create user' );

$resp = $client->app_user_login('testuser', '1QAZ2wsx');

ok( length($resp->{'access_token'}) > 0, 'app user login' );
