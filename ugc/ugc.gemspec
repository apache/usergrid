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
  s.add_runtime_dependency('gli','2.5.0')
  s.add_runtime_dependency('usergrid_iron')
  s.add_runtime_dependency('highline')
  s.add_runtime_dependency('command_line_reporter')
end
