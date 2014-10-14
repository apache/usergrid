
# Licensed to the Apache Software Foundation (ASF) under one or more contributor
# license agreements.  See the NOTICE.txt file distributed with this work for
# additional information regarding copyright ownership.  The ASF licenses this
# file to you under the Apache License, Version 2.0 (the "License"); you may not
# use this file except in compliance with the License.  You may obtain a copy of
# the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
# License for the specific language governing permissions and limitations under
# the License.

module Ugc
  class Management < Usergrid::Management

    def initialize
      super management_url
      auth_token = $settings.access_token
    end

    def management_url
      raise "not configured" unless $settings.base_url
      $settings.base_url
    end

    def login(username, password)
      super username, password
      $settings.access_token = auth_token
    end

  end
end
