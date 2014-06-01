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

Provides methods for easily invoking HTTP methods.

=head1 ATTRIBUTES

=over 4

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

# Private method
sub _is_token_required {
  my ($self, $resource) = @_;
  return 0 if $resource =~ m/\/management\/token/;
  1;
}

#Private method
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

=back

=head1 METHODS

=over 4

=item DELETE ($resource)

Invokes the specified resource with the HTTP DELETE method.

=cut
sub DELETE {
  my ($self, $resource) = @_;
  $self->_api_request('DELETE', $resource);
}

=item GET ($resource)

Invokes the specified resource with the HTTP GET method.

=cut
sub GET {
  my ($self, $resource) = @_;
  $self->_api_request('GET', $resource);
}

=item POST ($resource, $request)

Invokes the specified resource with the HTTP POST method and passes in the
JSON request.

=cut
sub POST {
  my ($self, $resource, $request) = @_;
  $self->_api_request('POST', $resource, $request);
}

=item POST ($resource, $request)

Invokes the specified resource with the HTTP PUT method and passes in the
JSON request.

=cut
sub PUT {
  my ($self, $resource, $request) = @_;
  $self->_api_request('PUT', $resource, $request);
}

# internal method
sub _enable_tracing {
  my ($self, $state, $old_state) = @_;
  if ($state) {
    Log::Log4perl::easy_init($DEBUG);
    our $logger = Log::Log4perl->get_logger();
  }
}

=item trace_message ($message)

Utility method to log a message to console if tracing is enabled.

=cut
sub trace_message {
  my ($self, $message) = @_;
  $Usergrid::Request::logger->debug($message) if (defined $Usergrid::Request::logger);
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

=item collection ($object, $uri)

Returns a L<Usergrid::Collection> object that encapsulates the given hashref
and the URI that resulted in it.

=cut
sub collection {
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
