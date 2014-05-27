#!/usr/bin/perl
use strict;
use warnings;

use Test::More tests => 3;

BEGIN {
  use_ok 'Usergrid::Client'     || print "Bail out!\n";
  use_ok 'Usergrid::Entity'     || print "Bail out!\n";
  use_ok 'Usergrid::Collection' || print "Bail out!\n";
}
