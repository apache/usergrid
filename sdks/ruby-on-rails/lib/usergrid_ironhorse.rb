require 'logger'
require 'active_model'
require 'rest-client'
require 'active_support'
require 'usergrid_iron'
require 'active_record/errors'

module Usergrid
  module Ironhorse

    Dir[Pathname.new(File.dirname(__FILE__)).join("extensions/**/*.rb")].each { |f| require f }

    USERGRID_PATH = File.join File.dirname(__FILE__), 'usergrid_ironhorse'

    def self.usergrid_path *path
      File.join USERGRID_PATH, *path
    end

    require usergrid_path('base')
    require usergrid_path('query')

    #require usergrid_path('../extensions', 'hash')
    #autoload :Management, usergrid_path('core', 'management')
  end
end
