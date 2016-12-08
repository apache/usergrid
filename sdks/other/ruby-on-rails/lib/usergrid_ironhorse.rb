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

    autoload :UserContext, usergrid_path('user_context')
  end
end
