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


# -*- encoding: utf-8 -*-
require File.expand_path('../lib/usergrid/version', __FILE__)

Gem::Specification.new do |gem|
  gem.authors       = ["Scott Ganyo"]
  gem.email         = ["scott@ganyo.com"]
  gem.description   = %q{Low-level gem to access Usergrid / Apigee App Services}
  gem.summary       = %q{Usergrid_iron enables simple, low-level Ruby access to Apigee's App Services
                        (aka Usergrid) REST API with minimal dependencies.}
  gem.homepage      = "https://github.com/scottganyo/usergrid_iron"

  gem.files         = `git ls-files`.split($\)
  gem.executables   = gem.files.grep(%r{^bin/}).map{ |f| File.basename(f) }
  gem.test_files    = gem.files.grep(%r{^(spec|spec|features)/})
  gem.name          = "usergrid_iron"
  gem.require_paths = ["lib"]
  gem.version       = Usergrid::VERSION

  gem.add_dependency 'rest-client'
  gem.add_dependency 'multi_json'

  gem.add_development_dependency 'rake'
  gem.add_development_dependency 'rspec'
  gem.add_development_dependency 'simplecov'
end
