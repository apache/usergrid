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

use Moose::Role;
use namespace::autoclean;

sub create {
  my ($self, $collection, $data) = @_;

  my $uri = URI::Template
    ->new('/{organization}/{application}/{collection}')
    ->process(
      organization=>$self->organization,
      application=>$self->application,
      collection=>$collection
  );

  $self->POST($uri, $data);
}

sub retrieve_by_id {
  my ($self, $collection, $id) = @_;

  my $uri = URI::Template
    ->new('/{organization}/{application}/{collection}/{id}')
    ->process(
      organization=>$self->organization,
      application=>$self->application,
      collection=>$collection,
      id=>$id
  );

  $self->GET($uri);
}

sub retrieve {
  my ($self, $collection) = @_;

  my $uri = URI::Template
    ->new('/{organization}/{application}/{collection}/{id}')
    ->process(
      organization=>$self->organization,
      application=>$self->application,
      collection=>$collection
  );

  $self->GET($uri);
}

sub delete {
  my ($self, $collection, $uuid) = @_;

  my $uri = URI::Template
    ->new('/{organization}/{application}/{collection}/{uuid}')
    ->process(
      organization=>$self->organization,
      application=>$self->application,
      collection=>$collection,
      uuid=>$uuid
  );

  $self->DELETE($uri);
}

1;
