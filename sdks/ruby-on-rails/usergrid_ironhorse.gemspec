# -*- encoding: utf-8 -*-
lib = File.expand_path('../lib', __FILE__)
$LOAD_PATH.unshift(lib) unless $LOAD_PATH.include?(lib)
require 'usergrid_ironhorse/version'

Gem::Specification.new do |gem|
  gem.name          = "usergrid_ironhorse"
  gem.version       = Usergrid::Ironhorse::VERSION
  gem.authors       = ["Scott Ganyo"]
  gem.email         = ["scott@ganyo.com"]
  gem.description   = %q{Rails ActiveModel gem to access Usergrid / Apigee App Services}
  gem.summary       = %q{Usergrid_ironhorse enables simple ActiveModel access to Apigee's App Services
                        (aka Usergrid) REST API for Rails developers.}
  gem.homepage      = "https://github.com/scottganyo/usergrid_ironhorse"

  gem.files         = `git ls-files`.split($/)
  gem.executables   = gem.files.grep(%r{^bin/}).map{ |f| File.basename(f) }
  gem.test_files    = gem.files.grep(%r{^(test|spec|features)/})
  gem.require_paths = ["lib"]

  gem.add_dependency 'usergrid_iron', '0.9.1'
  gem.add_dependency 'activemodel', '~> 3.2'
  gem.add_dependency 'activerecord', '~> 3.2'
  gem.add_dependency 'i18n'

  gem.add_development_dependency 'rake'
  gem.add_development_dependency 'rspec'
  gem.add_development_dependency 'simplecov'
end
