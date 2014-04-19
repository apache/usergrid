#Licensed to the Apache Software Foundation (ASF) under one or more contributor
#license agreements.  See the NOTICE.txt file distributed with this work for
#additional information regarding copyright ownership.  The ASF licenses this
#file to you under the Apache License, Version 2.0 (the "License"); you may not
#use this file except in compliance with the License.  You may obtain a copy of
#the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
#Unless required by applicable law or agreed to in writing, software
#distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
#WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
#License for the specific language governing permissions and limitations under
#the License.

# Ensure we require the local version and not one we might have installed already
require File.join([File.dirname(__FILE__),'lib','ugc','version.rb'])
spec = Gem::Specification.new do |s| 
  s.name = 'ugc'
  s.version = Ugc::VERSION
  s.author = 'Scott Ganyo'
  s.email = 'scott@ganyo.com'
  s.homepage = 'http://ganyo.com'
  s.platform = Gem::Platform::RUBY
  s.summary = 'Usergrid Command Line'
  s.license = 'Apache License, Version 2.0'
  s.files = `git ls-files`.split($\)
  s.require_paths << 'lib'
  s.has_rdoc = true
  s.extra_rdoc_files = ['README.rdoc','ugc.rdoc']
  s.rdoc_options << '--title' << 'ugc' << '--main' << 'README.rdoc' << '-ri'
  s.bindir = 'bin'
  s.executables << 'ugc'
  s.add_development_dependency('rake')
  s.add_development_dependency('rdoc')
  s.add_development_dependency('aruba')
  s.add_runtime_dependency('gli', '>= 2.6')
  s.add_runtime_dependency('usergrid_iron','>= 0.9.1')
  s.add_runtime_dependency('highline')
  s.add_runtime_dependency('command_line_reporter')
end
