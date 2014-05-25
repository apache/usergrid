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

package Usergrid::Core;

use Moose;
use namespace::autoclean;
use Log::Log4perl qw(:easy);

our $VERSION = '0.1';

with (
  'Usergrid::Verbs',
);

has 'organization'  => ( is => 'rw', isa => 'Str', required => 1);
has 'application'   => ( is => 'rw', isa => 'Str', required => 1);
has 'api_url'       => ( is => 'rw', isa => 'Str', required => 1);

has 'username'      => ( is => 'rw', isa => 'Str');
has 'password'      => ( is => 'rw', isa => 'Str');

has 'trace'         => ( is => 'rw', isa => 'Bool', trigger => \&_enable_tracing);

has 'user_token'    => ( is => 'rw');

sub _enable_tracing() {
  my ($self, $state, $old_state) = @_;
  if ($state) {
    Log::Log4perl::easy_init($DEBUG);
    our $logger = Log::Log4perl->get_logger();
  }
}

sub trace_message($) {
  my ($self, $message) = @_;
  $Usergrid::Core::logger->debug($message) if (defined $Usergrid::Core::logger);
}

1;
