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
use JSON;
use REST::Client;
use URI::Template;

use namespace::autoclean;

our $VERSION = '0.1';

has 'organization'  => ( is => 'rw', isa => 'Str');
has 'application'   => ( is => 'rw', isa => 'Str');
has 'api_url'       => ( is => 'rw', isa => 'Str');

has 'username'      => ( is => 'rw', isa => 'Str'); 
has 'password'      => ( is => 'rw', isa => 'Str'); 

our $json = JSON->new->allow_nonref;

our $admin_token;
our $user_token;

sub DELETE_request ($$$) {
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

sub GET_request ($$$) {
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

sub POST_request ($$$\%) {
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

sub admin_login($$$) {
  my ($self, $username, $password) = @_;

  my %request = (
    grant_type=>"password",
    username=>$username,
    password=>$password);

  $admin_token = $self->POST_request(undef, '/management/token', \%request);

  return $admin_token;
}

sub app_user_login($$$) {
  my ($self, $username, $password) = @_;

  my %request = (
    grant_type=>"password",
    username=>$username,
    password=>$password
  );

  my $uri = URI::Template
    ->new('/{organization}/{application}/token')
    ->process(
      organization=>$self->organization,
      application=>$self->application
  );

  $user_token = $self->POST_request(undef, $uri, \%request);

  return $user_token;
}

sub create($$\%) {
  my ($self, $collection, $data) = @_;

  my $uri = URI::Template
    ->new('/{organization}/{application}/{collection}')
    ->process(
      organization=>$self->organization,
      application=>$self->application,
      collection=>$collection
  );

  return $self->POST_request($user_token, $uri, $data);
}

sub retrieve_by_id($$) {
  my ($self, $collection, $id) = @_;

  my $uri = URI::Template
    ->new('/{organization}/{application}/{collection}/{id}')
    ->process(
      organization=>$self->organization,
      application=>$self->application,
      collection=>$collection,
      id=>$id
  );

  return $self->GET_request($user_token, $uri);
}

sub delete($$$) {
  my ($self, $collection, $uuid) = @_;

  my $uri = URI::Template
    ->new('/{organization}/{application}/{collection}/{uuid}')
    ->process(
      organization=>$self->organization,
      application=>$self->application,
      collection=>$collection,
      uuid=>$uuid
  );

  return $self->DELETE_request($user_token, $uri);
}

__PACKAGE__->meta->make_immutable;

1;

__END__

=head1 NAME

Usergrid::Client - Usergrid Perl Client

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
