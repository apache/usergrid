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

package Usergrid::Collection;

use Moose;
use namespace::autoclean;

with (
  'Usergrid::Request',
);

my @stack;

=head1 NAME

Usergrid::Collection - Encapsulates collection functionality

=head1 DESCRIPTION

Provides support for encapsulating collection data and for iterating through
them easily.

=head1 ATTRIBUTES

=over 4

=item object

A hash reference with the collection data (Read/Write, Required).

=back
=cut
has 'object'      => ( is => 'rw', required => 1 );
has 'uri'         => ( is => 'rw', required => 1 );
has 'iterator'    => ( is => 'rw', isa => 'Int', default => sub { -1 } );

=head1 METHODS

=over 4

=item has_next_entity

Returns true if there's another entity available during iteration.

=cut
sub has_next_entity {
  my $self = shift;
  my $next = $self->iterator + 1;
  return ($next >= 0 && $next < $self->count());
}

=item get_next_entity

Returns the next available entity. Checks to see if there's an entity available
to return, and if not returns an undef.

=cut
sub get_next_entity {
  my $self = shift;
  if ($self->has_next_entity()) {
    $self->iterator ($self->iterator + 1);
    return Usergrid::Entity->new ( object => $self->object->{'entities'}[$self->iterator] );
  }
  return undef;
}

=item count

Returns the count of the items in the collection.

=cut
sub count {
  my $self = shift;
  return scalar @{$self->object->{'entities'}};
}

=item reset_iterator

Rewinds the iterator back to the beginning.

=cut
sub reset_iterator {
  my $self = shift;
  $self->iterator (-1);
}

=item get_first_entity

Returns the first entity in the collection

=cut
sub get_first_entity {
  my $self = shift;
  return ($self->count() > 0) ? Usergrid::Entity->new (
    object => $self->object->{'entities'}[0] ) : undef;
}

=item get_last_entity

Returns the last entity in the collection

=cut
sub get_last_entity {
  my $self = shift;
  return ($self->count() > 0) ? Usergrid::Entity->new (
    object => $self->object->{'entities'}[$self->count() - 1] ) : undef;

}

=item get_next_page

Fetches the next page in the collection. Returns false when there are no more reults.

=cut
sub get_next_page {
  my $self = shift;

  my $csr = $self->object->{'cursor'};

  my $object = $self->GET($self->uri . "&cursor=". $csr);

  if ($object->{'count'} > 0) {
    push @stack, "1" if (scalar @stack == 0);
    push @stack, $csr;

    $self->object( $object );
    $self->reset_iterator();

    return $self;
  } else {
    return 0;
  }
}

=item get_prev_page

Fetches the previous page in the collection. Returns false when there are no more reults.

=cut
sub get_prev_page {
  my $self = shift;
  my $object;

  if (scalar @stack > 0) {
    my $csr = pop @stack;

    if ($csr eq "1") {
      $object = $self->GET($self->uri);
    } else {
      $object = $self->GET($self->uri . "&cursor=" . $csr);
    }

    $self->object( $object );
    $self->reset_iterator();

    return $self;
  } else {
    return 0;
  }
}

__PACKAGE__->meta->make_immutable;

1;

__END__

=back

=head1 SEE ALSO

L<Usergrid::Client>, L<Usergrid::Entity>, L<Usergrid::Request>

=head1 LICENSE

This software is distributed under the Apache 2 license.

=head1 AUTHOR

Anuradha Weeraman <anuradha@cpan.org>

=cut
