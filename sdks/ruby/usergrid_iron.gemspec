# -*- encoding: utf-8 -*-
require File.expand_path('../lib/usergrid/version', __FILE__)

Gem::Specification.new do |gem|
  gem.authors       = ["Scott Ganyo"]
  gem.email         = ["sganyo@apigee.com"]
  gem.description   = %q{Low-level gem to access Usergrid / Apigee App Services}
  gem.summary       = %q{Usergrid_iron enables simple, low-level Ruby access to Apigee's App Services
                        (aka Usergrid) REST API with minimal dependencies.}
  gem.homepage      = "https://github.com/scottganyo/usergrid_iron"

  gem.files         = `/usr/local/git/bin/git ls-files`.split($\)
  gem.executables   = gem.files.grep(%r{^bin/}).map{ |f| File.basename(f) }
  gem.test_files    = gem.files.grep(%r{^(spec|spec|features)/})
  gem.name          = "usergrid_iron"
  gem.require_paths = ["lib"]
  gem.version       = Usergrid::VERSION

  gem.add_dependency 'rest-client'
  gem.add_dependency 'json_pure'

  gem.add_development_dependency 'rake'
  gem.add_development_dependency 'rspec'
  gem.add_development_dependency 'simplecov'
end
