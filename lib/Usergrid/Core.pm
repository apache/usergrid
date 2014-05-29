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

package Usergrid::Core;

use Moose;
use namespace::autoclean;
use Log::Log4perl qw(:easy);
use URI::Template;
use JSON;

our $VERSION = '0.1';

my $json = JSON->new->allow_nonref;

with (
  'Usergrid::Request',
);

=head1 NAME

Usergrid::Core - Common functionality


=head1 DESCRIPTION

Usergrid::Core is the base class for Usergrid::Client and contains common
functionality and attributes.

=head1 ATTRIBUTES

=over

=item organization

Organization name (Read/Write, String, Required).

=item application

Application name (Read/Write, String, Required).

=item api_url

The URL for the API server (Read/Write, String, Required).

=item trace

Enable/disable request and response tracing for debugging and troubleshooting
(Read/Write, Boolean, Optional).

=item user_token

The logged in user context (Read/Write).

=cut
has 'organization'  => ( is => 'rw', isa => 'Str', required => 1);
has 'application'   => ( is => 'rw', isa => 'Str', required => 1);
has 'api_url'       => ( is => 'rw', isa => 'Str', required => 1);

has 'trace'         => ( is => 'rw', isa => 'Bool', trigger => \&_enable_tracing);

has 'user_token'    => ( is => 'rw');

# internal method
sub _enable_tracing {
  my ($self, $state, $old_state) = @_;
  if ($state) {
    Log::Log4perl::easy_init($DEBUG);
    our $logger = Log::Log4perl->get_logger();
  }
}

=head1 METHODS

=item trace_message ($message)

Utility method to log a message to console if tracing is enabled.

=cut
sub trace_message {
  my ($self, $message) = @_;
  $Usergrid::Core::logger->debug($message) if (defined $Usergrid::Core::logger);
}

=item prettify ($message)

Returns a prettified string representation for a JSON encoded object.

=cut
sub prettify {
  my ($self, $json_obj) = @_;
  return $json->pretty->encode($json_obj);
}

=item json_encode ($hashref)

Returns a JSON object from a hash reference.

=cut
sub json_encode {
  my ($self, $json_obj) = @_;
  $json->encode($json_obj);
}

=item json_decode ($json_object)

Returns a hash reference from a JSON object.

=cut
sub json_decode {
  my ($self, $json_obj) = @_;
  $json->decode($json_obj);
}

1;

__END__

=back

=head1 SEE ALSO

L<Usergrid::Client>, L<Usergrid::Collection>, L<Usergrid::Entity>, L<Usergrid::Request>

=head1 LICENSE

This software is distributed under the Apache 2 license.

=head1 AUTHOR

Anuradha Weeraman <anuradha@cpan.org>

=cut
