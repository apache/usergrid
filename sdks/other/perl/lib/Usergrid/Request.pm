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

package Usergrid::Request;

use Moose::Role;
use namespace::autoclean;
use Carp qw(confess);
use Log::Log4perl qw(:easy);
use REST::Client;
use URI::Template;
use JSON;

my $json = JSON->new->allow_nonref;

=head1 NAME

Usergrid::Request - Role that provides HTTP invocation and utility methods

=head1 DESCRIPTION

This is a Role that is applied to L<Usergrid::Client> and L<Usergrid::Collection>
which provides methods that relate to HTTP invocation as well as some utility
functions.

=head1 ATTRIBUTES

=over 4

=item organization (String)

Organization name

=item application (String)

Application name

=item api_url (String)

URL of the Usergrid instance

=item trace (Boolean)

Enable/disable request and response tracing for debugging and troubleshooting
(Optional)

=cut
has 'organization'  => ( is => 'rw', isa => 'Str', required => 1);
has 'application'   => ( is => 'rw', isa => 'Str', required => 1);
has 'api_url'       => ( is => 'rw', isa => 'Str', required => 1);

has 'trace'         => ( is => 'rw', isa => 'Bool', trigger => \&_enable_tracing);

has 'user_token'    => ( is => 'rw');

# internal method
sub _is_token_required {
  my ($self, $resource) = @_;
  return 0 if $resource =~ m/\/management\/token/;
  1;
}

# internal method
sub _api_request {
  my ($self, $method, $resource, $request) = @_;

  $self->trace_message("$method $resource");
  $self->trace_message("REQUEST: " . $self->prettify($request)) if ($request);

  my $client = REST::Client->new();
  $client->setHost($self->api_url);

  if ($self->_is_token_required($resource) == 1 && defined $self->user_token) {
     $client->addHeader('Authorization',
        'Bearer ' . $self->user_token->{'access_token'});
  }

  my $json_req = $self->json_encode($request) if ($request);

  $client->DELETE($resource)          if ($method eq 'DELETE');
  $client->GET($resource)             if ($method eq 'GET');
  $client->POST($resource, $json_req) if ($method eq 'POST');
  $client->PUT($resource, $json_req)  if ($method eq 'PUT');

  my $response = $client->responseContent();

  $self->trace_message("RESPONSE: " . $self->prettify($response)) if ($response);

  return undef if ($client->responseCode() eq "404");

  confess "Bad request"  if ($client->responseCode() eq "400");
  confess "Unauthorized" if ($client->responseCode() eq "401");
  confess "Forbidden"    if ($client->responseCode() eq "403");
  confess "Server error" if ($client->responseCode() eq "500");

  return $self->json_decode($response);
}

# internal method
sub _enable_tracing {
  my ($self, $state, $old_state) = @_;
  if ($state) {
    Log::Log4perl::easy_init($DEBUG);
    our $logger = Log::Log4perl->get_logger();
  }
}

# internal method
sub _collection {
  my ($self, $object, $uri) = @_;

  return Usergrid::Collection->new (
    object       => $object,
    uri          => $uri,
    organization => $self->organization,
    application  => $self->application,
    api_url      => $self->api_url,
    trace        => $self->trace,
    user_token   => $self->user_token
  );
}

=back

=head1 METHODS

=head2 HTTP Invocation Methods

=over 4

=item DELETE ( $resource )

Invokes HTTP DELETE on the specified resource.

=cut
sub DELETE {
  my ($self, $resource) = @_;
  $self->_api_request('DELETE', $resource);
}

=item GET ( $resource )

Invokes HTTP GET on the specified resource.

=cut
sub GET {
  my ($self, $resource) = @_;
  $self->_api_request('GET', $resource);
}

=item POST ( $resource, \%request )

Invokes HTTP POST on the specified resource and passes in the payload
for the request.

=cut
sub POST {
  my ($self, $resource, $request) = @_;
  $self->_api_request('POST', $resource, $request);
}

=item PUT ( $resource, \%request )

Invokes HTTP PUT on the specified resource and passes in the payload
for the request.

=cut
sub PUT {
  my ($self, $resource, $request) = @_;
  $self->_api_request('PUT', $resource, $request);
}

=back

=head2 Utility Methods

=over 4

=item trace_message ( $message )

Utility method to log a message to console if tracing is enabled.

=cut
sub trace_message {
  my ($self, $message) = @_;
  $Usergrid::Request::logger->debug($message) if (defined $Usergrid::Request::logger);
}

=item prettify ( $message, \%object )

Returns a prettified string representation hash reference.

=cut
sub prettify {
  my ($self, $json_obj) = @_;
  return $json->pretty->encode($json_obj);
}

=item json_encode ( \%hashref )

Returns a JSON object from a hash reference.

=cut
sub json_encode {
  my ($self, $json_obj) = @_;
  $json->encode($json_obj);
}

=item json_decode ( $json_object )

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

L<Usergrid::Client>, L<Usergrid::Collection>, L<Usergrid::Entity>

=head1 LICENSE

This software is distributed under the Apache 2 license.

=head1 AUTHOR

Anuradha Weeraman <anuradha@cpan.org>

=cut
