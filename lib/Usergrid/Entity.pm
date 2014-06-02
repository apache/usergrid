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

package Usergrid::Entity;

use Moose;
use namespace::autoclean;

=head1 NAME

Usergrid::Entity - a Usergrid entity

=head1 DESCRIPTION

Encapsulates Usergrid entities and provides methods for accessing the underlying
data.

=head1 ATTRIBUTES

=over 4

=item object

A hash reference with the entity data

=back

=cut
has 'object'      => ( is => 'rw', required => 1);

=head1 METHODS

=over 4

=item get ( $attribute_name )

Returns the value of the specified attribute.

=cut
sub get {
  my ($self, $key) = @_;
  return $self->object->{$key} if (defined $self->object->{$key});
  return $self->object->{'entities'}[0]->{$key};
}

=item set ( $attribute_name, $value )

Sets the value of the specified attribute.

=cut
sub set {
  my ($self, $key, $value) = @_;
  if (defined $self->object->{$key}) {
    $self->object->{$key} = $value;
    return;
  }
  $self->object->{'entities'}[0]->{$key} = $value;
}

__PACKAGE__->meta->make_immutable;

1;

__END__

=back

=head1 SEE ALSO

L<Usergrid::Client>, L<Usergrid::Collection>, L<Usergrid::Request>

=head1 LICENSE

This software is distributed under the Apache 2 license.

=head1 AUTHOR

Anuradha Weeraman <anuradha@cpan.org>

=cut
