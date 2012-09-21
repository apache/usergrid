require 'logger'
require 'json'

module Usergrid

  LOG = Logger.new(STDOUT)

  USERGRID_PATH = File.join File.dirname(__FILE__), 'usergrid'

  def self.usergrid_path *path
    File.join USERGRID_PATH, *path
  end

  require usergrid_path('version')

  require usergrid_path('extensions', 'hash')
  require usergrid_path('extensions', 'response')

  require usergrid_path('core', 'resource')
  require usergrid_path('core', 'management')
  require usergrid_path('core', 'organization')
  require usergrid_path('core', 'application')
  require usergrid_path('core', 'entity')
  require usergrid_path('core', 'collection')

  #autoload :Management, usergrid_path('core', 'management')
end
