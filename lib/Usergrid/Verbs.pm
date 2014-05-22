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

use REST::Client;
use Moose::Role;
use JSON;

use namespace::autoclean;

our $json = JSON->new->allow_nonref;

sub DELETE ($$$) {
  my ($self, $token, $resource) = @_;

  my $client = REST::Client->new();
  $client->setHost($self->api_url);

  if (defined $token) {
    $client->addHeader('Authorization', 'Bearer ' . $token->{'access_token'});
  }

  $client->DELETE($resource);

  my $response = $client->responseContent();

  return $json->decode($response);
}

sub GET ($$$) {
  my ($self, $token, $resource) = @_;

  my $client = REST::Client->new();
  $client->setHost($self->api_url);

  if (defined $token) {
    $client->addHeader('Authorization', 'Bearer ' . $token->{'access_token'});
  }

  $client->GET($resource);

  my $response = $client->responseContent();

  my $json_resp = $json->decode($response);

  return undef if ($client->responseCode() eq "404");

  return $json_resp;
}

sub POST ($$$\%) {
  my ($self, $token, $resource, $request) = @_;

  my $json_req = $json->encode($request);

  my $client = REST::Client->new();
  $client->setHost($self->api_url);

  if (defined $token) {
    $client->addHeader('Authorization', 'Bearer ' . $token->{'access_token'});
  }

  $client->POST($resource, $json_req);

  my $response = $client->responseContent();

  return $json->decode($response);
}

sub PUT ($$$\%) {
  my ($self, $token, $resource, $request) = @_;

  my $json_req = $json->encode($request);

  my $client = REST::Client->new();
  $client->setHost($self->api_url);

  if (defined $token) {
    $client->addHeader('Authorization', 'Bearer ' . $token->{'access_token'});
  }

  $client->PUT($resource, $json_req);

  my $response = $client->responseContent();

  return $json->decode($response);
}

1;
