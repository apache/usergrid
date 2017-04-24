Pod::Spec.new do |s|

  s.name         = "UsergridSDK-ObjC"
  s.version      = "0.9.3"
  s.summary      = "Usergrid SDK written in Objective-C"
  s.homepage     = "https://github.com/ocgungor/usergrid"
  s.license      = { :type => "Apache 2.0", :file => "LICENSE" }
  s.author       = "Oguzhan Gungor"
  s.source       = { :git => "https://github.com/ocgungor/usergrid.git",
                     :tag => "#{s.version}"
                    }
  s.social_media_url = 'https://twitter.com/usergrid'

  s.source_files  = "Classes", "UGAPI/**/*.{h,m}"
  s.exclude_files = "Classes/Exclude"
  s.requires_arc = true

end
