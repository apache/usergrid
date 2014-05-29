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

=head1 NAME

Usergrid::Client - Usergrid Perl Client

=head1 SYNOPSIS

  use Usergrid::Client;

  my $client = Usergrid::Client->new(
    organization => 'test-organization',
    application  => 'test-app',
    api_url      => 'http://localhost:8080'
  );

  $client->login('johndoe', 'Johndoe123$');

  $client->add_entity("books", { name => "Ulysses" });
  $client->add_entity("books", { name => "Neuromancer" });

  my $books = $client->get_collection("books");

  while ($books->has_next_entity()) {
    my $book = $books->get_next_entity();

    print "Name: "   . $book->get('name')   . ", ";
    print "Author: " . $book->get('author') . "\n";

    $book->set("in-stock", 0);
    $client->update_entity($book);
  }

=head1 DESCRIPTION

Usergrid::Client is the client SDK for Apache Usergrid. It provides a Perl
object based wrapper for the Usergrid REST-ful APIs.

=head1 METHODS

=over

=item login ($username, $password)

Logs into Usergrid with the provided username and password using application
authentication.

=cut
sub login {
  my ($self, $username, $password) = @_;

  my %request = (
    grant_type => "password",
    username   => $username,
    password   => $password
  );

  my $uri = URI::Template
    ->new('/{organization}/{application}/token')
    ->process(
      organization => $self->organization,
      application  => $self->application
  );

  my $token = $self->POST($uri, \%request);

  $self->user_token($token);

  return $self->user_token;
}

=item management_login ($username, $password)

Used for obtaining a management token for performing privileged operations.

=cut
sub management_login {
  my ($self, $username, $password) = @_;

  my %request = (
    grant_type => "password",
    username   => $username,
    password   => $password
  );

  my $token = $self->POST('/management/token', \%request);

  $self->user_token($token);

  return $self->user_token;
}

=item add_entity ($collection, $entity)

Creates a new entity with the attributes specified in the $entity hash reference
in the given collection.

=cut
sub add_entity {
  my ($self, $collection, $entity) = @_;

  my $uri = URI::Template
    ->new('/{organization}/{application}/{collection}')
    ->process(
      organization => $self->organization,
      application  => $self->application,
      collection   => $collection
  );

  return Usergrid::Entity->new( object => $self->POST($uri, $entity));
}

=item update_entity ($entity)

Saves changes to the entity.

=cut
sub update_entity {
  my ($self, $entity) = @_;

  my $uri = URI::Template
    ->new('/{organization}/{application}/{collection}/{uuid}')
    ->process(
      organization => $self->organization,
      application  => $self->application,
      collection   => $entity->get('type'),
      uuid         => $entity->get('uuid')
  );

  return Usergrid::Entity->new( object => $self->PUT($uri,
    $entity->object->{'entities'}[0]) );
}

=item get_entity ($collection, $id)

Returns the entity by either id or name for the given collection. If the entity
does not exist, the method returns an undef.

=cut
sub get_entity {
  my ($self, $collection, $id_or_name) = @_;

  my $uri = URI::Template
    ->new('/{organization}/{application}/{collection}/{id_or_name}')
    ->process(
      organization => $self->organization,
      application  => $self->application,
      collection   => $collection,
      id_or_name   => $id_or_name
  );

  my $response = $self->GET($uri);

  return undef if (! $response);

  return Usergrid::Entity->new( object => $response );
}

=item get_collection ($collection, [$limit])

Returns a L<Usergrid::Collection> with the list of entities up to a maximum
specified by $limit, which is 10 if not specified.

=cut
sub get_collection {
  my ($self, $collection, $limit) = @_;

  my $uri = URI::Template
    ->new('/{organization}/{application}/{collection}?limit={limit}')
    ->process(
      organization => $self->organization,
      application  => $self->application,
      collection   => $collection,
      limit        => ( defined $limit ) ? $limit: 10
  );

  return Usergrid::Collection->new( object => $self->GET($uri) );
}

=item update_collection ($collection, $properties, [$query], [$limit])

Updates all the entities in the specified collection with attributes in the
$properties hash reference. Optionally pass in the $query to narrow the scope of
the objects that are affected and the $limit to specify the maximum number of
records.

=cut
sub update_collection {
  my ($self, $collection, $properties, $query, $limit) = @_;

  my $uri = URI::Template
    ->new('/{organization}/{application}/{collection}/?limit={limit}&ql={query}')
    ->process(
      organization => $self->organization,
      application  => $self->application,
      collection   => $collection,
      limit        => ( defined $limit ) ? $limit : 10,
      query        => ( defined $query ) ? $query : undef
  );

  return Usergrid::Collection->new( object => $self->PUT($uri, $properties) );
}

=item delete_collection ($collection, [$query], [$limit])

Deletes all the entities in the specified collection. Optionally pass in the
$query to narrow the scope of the objects that are affected and the $limit to
specify the maximum number of records.

=cut
sub delete_collection {
  my ($self, $collection, $query, $limit) = @_;

  my $uri = URI::Template
    ->new('/{organization}/{application}/{collection}/?limit={limit}&ql={query}')
    ->process(
      organization => $self->organization,
      application  => $self->application,
      collection   => $collection,
      limit        => ( defined $limit ) ? $limit : 10,
      query        => ( defined $query ) ? $query : undef
  );

  return Usergrid::Collection->new( object => $self->DELETE($uri) );
}

=item query_collection ($collection, $query, [$limit])

Queries all the entities in the specified collection. Optionally pass in the
$limit to specify the maximum number of records returned.

=cut
sub query_collection {
  my ($self, $collection, $query, $limit) = @_;

  my $uri = URI::Template
    ->new('/{organization}/{application}/{collection}?limit={limit}&ql={ql}')
    ->process(
      organization => $self->organization,
      application  => $self->application,
      collection   => $collection,
      limit        => ( defined $limit ) ? $limit : 10,
      ql           => $query
  );

  return Usergrid::Collection->new( object => $self->GET($uri) );
}

=item delete_entity_by_id ($collection, $id)

Deletes an entity from the collection, specified by either UUID or name.

=cut
sub delete_entity_by_id {
  my ($self, $collection, $id_or_name) = @_;

  my $uri = URI::Template
    ->new('/{organization}/{application}/{collection}/{id}')
    ->process(
      organization => $self->organization,
      application  => $self->application,
      collection   => $collection,
      id           => $id_or_name
  );

  return Usergrid::Entity->new( object => $self->DELETE($uri) );
}

=item delete_entity ($entity)

Deletes the specified instance of L<Usergrid::Entity>.

=cut
sub delete_entity {
  my ($self, $entity) = @_;

  my $uri = URI::Template
    ->new('/{organization}/{application}/{collection}/{uuid}')
    ->process(
      organization => $self->organization,
      application  => $self->application,
      collection   => $entity->get('type'),
      uuid         => $entity->get('uuid')
  );

  return Usergrid::Entity->new( object => $self->DELETE($uri) );
}

__PACKAGE__->meta->make_immutable;

1;

__END__

=back

=head1 SEE ALSO

L<Usergrid::Core>, L<Usergrid::Collection>, L<Usergrid::Entity>, L<Usergrid::Request>

=head1 LICENSE

This software is distributed under the Apache 2 license.

=head1 AUTHOR

Anuradha Weeraman <anuradha@cpan.org>

=cut
