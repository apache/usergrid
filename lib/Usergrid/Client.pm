#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# Author: Anuradha Weeraman (anuradha@cpan.org)
#

package Usergrid::Client;
use Moose;

our $VERSION = '0.1';

has 'orgName' => (is => 'rw');
has 'appName' => (is => 'rw');

sub login {
  return 1;
}

1;

__END__

=head1 NAME

Usergrid::Client - Usergrid Client SDK

=head1 SYNOPSIS

	use Usergrid::Client;
	my $client = Usergrid::Client->new();
	$client->login($username, $password);

=head1 DESCRIPTION

Usergrid::Client is the client SDK for Apache Usergrid Backend-as-a-Service.

=head1 LICENSE

This software is distributed under the Apache 2 license.

=head1 AUTHOR

Anuradha Weeraman <anuradha@cpan.org>

=cut
