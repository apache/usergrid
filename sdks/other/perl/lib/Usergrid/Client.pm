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

with (
  'Usergrid::Request',
);

our $VERSION = '0.22';

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

Usergrid::Client provides an easy to use Perl API for Apache Usergrid.

=head1 WHAT IS APACHE USERGRID

Usergrid is an open-source Backend-as-a-Service ("BaaS" or "mBaaS") composed of
an integrated distributed NoSQL database, application layer and client tier with
SDKs for developers looking to rapidly build web and/or mobile applications.
It provides elementary services (user registration & management, data storage,
file storage, queues) and retrieval features (full text search, geolocation
search, joins) to power common app features.

It is a multi-tenant system designed for deployment to public cloud environments
(such as Amazon Web Services, Rackspace, etc.) or to run on traditional server
infrastructures so that anyone can run their own private BaaS deployment.

For architects and back-end teams, it aims to provide a distributed, easily
extendable, operationally predictable and highly scalable solution. For
front-end developers, it aims to simplify the development process by enabling
them to rapidly build and operate mobile and web applications without requiring
backend expertise.

Source: L<https://usergrid.apache.org/docs/>

For more information, visit L<http://www.usergrid.org>

=head1 ATTRIBUTES

The following attributes are made available via the L<Usergrid::Request> role:

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

=back

=head1 METHODS

The following methods are provided in this API for interacting with the Apache
Usergrid backend.

=head2 Authentication

=over 4

=item login ( $username, $password )

Performs application user authentication. Returns the user token for the
logged in user. The token is also kept in memory and used for subsequent
authentication of requests.

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

=item admin_login ( $username, $password )

Performs admin user authentication. Returns the user token for the
logged in user. The token is also kept in memory and used for subsequent
authentication of requests.

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

=back

=head2 Entities

This section covers some of the entity level methods available in the API.
Entities form one of the basic building blocks of Usergrid and is analogous to
a row in an RDBMS table.

=over 4

=item add_entity ( $collection, \%entity )

Creates a new entity within the specified collection. Returns a L<Usergrid::Entity>
for the newly added entity.

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

=item update_entity ( L<Usergrid::Entity> )

Saves changes to the given entity. Returns the updated L<Usergrid::Entity>.

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

=item get_entity ( $collection, $id )

Returns a L<Usergrid::Entity> identified by either UUID or name.
If the entity does not exist, the method returns FALSE.

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

=item delete_entity_by_id ( $collection, $id )

Deletes an entity from the collection identified by either UUID or name. Returns
a L<Usergrid::Entity> of the deleted entity.

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

=item delete_entity ( L<Usergrid::Entity> )

Deletes the specified L<Usergrid::Entity>. Returns an instance of the deleted
L<Usergrid::Entity> if successful.

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

=item connect_entities ( L<Usergrid::Entity>, $relationship, L<Usergrid::Entity> )

Creates a connection from Entity #1 to Entity #2 signified by the relationship.
The first entity in this relationship is identified as the connecting entity and
the second as the connected entity. The relationship is a string that signifies
the type of connection. This returns a L<Usergrid::Entity> of the connecting
entity.

=cut
sub connect_entities {
  my ($self, $connecting, $relationship, $connected) = @_;

  my $uri = URI::Template
    ->new ('/{organization}/{application}/{connecting_collection}/{connecting}/{relationship}/{connected_collection}/{connected}')
    ->process(
      organization          => $self->organization,
      application           => $self->application,
      connecting_collection => $connecting->get('type'),
      connecting            => $connecting->get('uuid'),
      relationship          => $relationship,
      connected_collection  => $connected->get('type'),
      connected             => $connected->get('uuid')
    );

    return Usergrid::Entity->new ( object => $self->POST($uri));
}

=item disconnect_entities ( L<Usergrid::Entity>, $relationship, L<Usergrid::Entity> )

Removes the connection between the two entities signified by the relationship.
This does not affect the entities in any other way apart from the removal of the
connection that is depicted by the relationship. This returns a L<Usergrid::Entity>
of the connecting entity with the given relationship removed.

=cut
sub disconnect_entities {
my ($self, $connecting, $relationship, $connected) = @_;

my $uri = URI::Template
  ->new ('/{organization}/{application}/{connecting_collection}/{connecting}/{relationship}/{connected_collection}/{connected}')
  ->process(
    organization          => $self->organization,
    application           => $self->application,
    connecting_collection => $connecting->get('type'),
    connecting            => $connecting->get('uuid'),
    relationship          => $relationship,
    connected_collection  => $connected->get('type'),
    connected             => $connected->get('uuid')
  );

  return Usergrid::Entity->new ( object => $self->DELETE($uri));
}

=back

=head2 Collections

This section covers the methods related to retrieving and working with
collections in the Usergrid API. Collections contains groups of entities and is
analogous to a table in an RDBMS.

=over 4

=item get_collection ( $collection, [ $limit ] )

Returns a L<Usergrid::Collection> with the list of entities up to the maximum
specified limit, which is 10 if not provided.

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

  return $self->_collection($self->GET($uri), $uri);
}

=item update_collection ( $collection, \%attributes, [ $query ], [ $limit ] )

Updates all the entities in the specified collection with the provided attributes.
Optionally pass in the SQL-like query to narrow the scope of the objects that
are affected. This also supports specifying a limit to restrict the maximum number of
records that are updated. If not specified, the limit defaults to 10 entities.

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

  return $self->_collection($self->PUT($uri, $properties), $uri);
}

=item delete_collection ( $collection, [ $query ], [ $limit ] )

Batch delete entities in the specified collection. Optionally pass in a SQL-like
query to narrow the scope of the objects that are affected and a limit to restrict
the maximum number of records that are deleted. If not specified, the limit
defaults to 10 entities.

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

  return $self->_collection($self->DELETE($uri), $uri);
}

=item query_collection ( $collection, $query, [ $limit ] )

Queries all the entities in the specified collection using a SQL-like query string.
This also supports specifying a limit to restrict the maximum number of
records that are returned. If not specified, the limit defaults to 10 entities.

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

  return $self->_collection($self->GET($uri), $uri);
}

=item query_connections ( $entity, $relationship, [ $query ], [ $limit ] )

Returns a collection of entities for the given relationship, optionally filtered
by a SQL-like query and limited to the maximum number of records specified. If no limit
is provided, a default of 10 entities is assumed.

=cut
sub query_connections {
  my ($self, $entity, $relationship, $query, $limit) = @_;

  my $uri = URI::Template
    ->new('/{organization}/{application}/{collection}/{id}/{relationship}?limit={limit}&ql={ql}')
    ->process(
      organization => $self->organization,
      application  => $self->application,
      collection   => $entity->get('type'),
      id           => $entity->get('uuid'),
      relationship => $relationship,
      limit        => ( defined $limit ) ? $limit : 10,
      ql           => $query
  );

  return $self->_collection($self->GET($uri), $uri);
}

__PACKAGE__->meta->make_immutable;

1;

__END__

=back

=head1 SEE ALSO

L<Usergrid::Collection>, L<Usergrid::Entity>, L<Usergrid::Request>

=head1 LICENSE

This software is distributed under the Apache 2 license.

=head1 AUTHOR

Anuradha Weeraman <anuradha@cpan.org>

=cut
