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

package Usergrid::Client;

use Moose;
use namespace::autoclean;
use Usergrid::Entity;
use Usergrid::Collection;

extends 'Usergrid::Core';

sub login {
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

  my $token = $self->POST($uri, \%request);

  $self->user_token($token);

  return $self->user_token;
}

sub management_login {
  my ($self, $username, $password) = @_;

  my %request = (
    grant_type=>"password",
    username=>$username,
    password=>$password);

  my $token = $self->POST('/management/token', \%request);

  $self->user_token($token);

  return $self->user_token;
}

sub add_entity {
  my ($self, $collection, $entity) = @_;

  my $uri = URI::Template
    ->new('/{organization}/{application}/{collection}')
    ->process(
      organization=>$self->organization,
      application=>$self->application,
      collection=>$collection
  );

  return Usergrid::Entity->new( object => $self->POST($uri, $entity));
}

sub update_entity {
  my ($self, $entity) = @_;

  my $uri = URI::Template
    ->new('/{organization}/{application}/{collection}/{uuid}')
    ->process(
      organization=>$self->organization,
      application=>$self->application,
      collection=>$entity->get('type'),
      uuid=>$entity->get('uuid')
  );

  return Usergrid::Entity->new( object => $self->PUT($uri,
    $entity->object->{'entities'}[0]) );
}

sub get_entity_by_uuid {
  my ($self, $collection, $id) = @_;

  my $uri = URI::Template
    ->new('/{organization}/{application}/{collection}/{id}')
    ->process(
      organization=>$self->organization,
      application=>$self->application,
      collection=>$collection,
      id=>$id
  );

  my $response = $self->GET($uri);

  return undef if (! $response);

  return Usergrid::Entity->new( object => $response );
}

sub get_collection {
  my ($self, $collection) = @_;

  my $uri = URI::Template
    ->new('/{organization}/{application}/{collection}')
    ->process(
      organization=>$self->organization,
      application=>$self->application,
      collection=>$collection
  );

  return Usergrid::Collection->new( object => $self->GET($uri) );
}

sub delete_entity_by_uuid {
  my ($self, $collection, $uuid) = @_;

  my $uri = URI::Template
    ->new('/{organization}/{application}/{collection}/{uuid}')
    ->process(
      organization=>$self->organization,
      application=>$self->application,
      collection=>$collection,
      uuid=>$uuid
  );

  return Usergrid::Entity->new( object => $self->DELETE($uri) );
}

sub delete_entity {
  my ($self, $entity) = @_;

  my $uri = URI::Template
    ->new('/{organization}/{application}/{collection}/{uuid}')
    ->process(
      organization=>$self->organization,
      application=>$self->application,
      collection=>$entity->get('type'),
      uuid=>$entity->get('uuid')
  );

  return Usergrid::Entity->new( object => $self->DELETE($uri) );
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
