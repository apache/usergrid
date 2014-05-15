use strict;
use warnings;

use Test::More tests => 2;

use_ok 'Usergrid::Client';

my $client = Usergrid::Client->new();

ok( $client->login() == 1, 'login' );
