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

package Usergrid::Verbs;

use Moose::Role;
use namespace::autoclean;
use Carp qw(confess);

use REST::Client;

sub is_token_required {
  my ($self, $resource) = @_;
  return 0 if $resource =~ m/\/management\/token/;
  1;
}

sub _api_request {
  my ($self, $method, $resource, $request) = @_;

  $self->trace_message("$method $resource");
  $self->trace_message("REQUEST: " . $self->prettify($request)) if ($request);

  my $client = REST::Client->new();
  $client->setHost($self->api_url);

  if ($self->is_token_required($resource) == 1 && defined $self->user_token) {
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

  confess "Bad request" if ($client->responseCode() eq "400");
  confess "Unauthorized" if ($client->responseCode() eq "401");
  confess "Forbidden" if ($client->responseCode() eq "403");
  confess "Server error" if ($client->responseCode() eq "500");

  return $self->json_decode($response);
}

sub DELETE {
  my ($self, $resource) = @_;
  $self->_api_request('DELETE', $resource);
}

sub GET {
  my ($self, $resource) = @_;
  $self->_api_request('GET', $resource);
}

sub POST {
  my ($self, $resource, $request) = @_;
  $self->_api_request('POST', $resource, $request);
}

sub PUT {
  my ($self, $resource, $request) = @_;
  $self->_api_request('PUT', $resource, $request);
}

1;
