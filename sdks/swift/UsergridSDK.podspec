Pod::Spec.new do |s|
  s.name = 'UsergridSDK'
  s.version = '2.1.0'
  s.summary = 'Usergrid SDK written in Swift'
  s.homepage = 'https://github.com/apache/usergrid/tree/master/sdks/swift'
  s.license = 'Apache 2.0'
  s.author = { 'Robert Walsh' => 'rjwalsh1985@gmail.com' }
  s.social_media_url = 'https://twitter.com/usergrid'
  s.requires_arc = true

  s.ios.deployment_target = '8.0'
  s.watchos.deployment_target = '2.1'
  s.tvos.deployment_target = '9.1'
  s.osx.deployment_target = '10.11'

  s.source = { :git => 'https://github.com/apache/usergrid.git', :branch => 'master' }
  s.source_files  = 'sdks/swift/Source/*.swift'
end
