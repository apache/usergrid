
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

use strict;
use warnings;

use 5.008;

use ExtUtils::MakeMaker;

WriteMakefile(
  LICENSE           => 'apache',
  AUTHOR            => 'Anuradha Weeraman <anuradha@cpan.org',
  ABSTRACT          => 'Client API for Apache Usergrid',
  NAME              => 'Usergrid::Client',
  VERSION_FROM      => 'lib/Usergrid/Client.pm',
  PREREQ_PM => {
    'Moose'                => 0,
    'JSON'                 => 0,
    'REST::Client'         => 0,
    'URI::Template'        => 0,
    'Log::Log4perl'        => 0,
    'namespace::autoclean' => 0
  },
  BUILD_REQUIRES    => {
    'Test::More'    => '0.98'
  }
);
