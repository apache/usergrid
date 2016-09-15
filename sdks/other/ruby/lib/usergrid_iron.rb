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

require 'logger'
require 'multi_json'


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
