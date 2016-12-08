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

module Usergrid
  class Management < Resource

    def initialize(url, options={})
      management = url.split('/')[-1]
      if management == 'management'
        api_url = url[0..url.index(management)-2]
      else
        api_url = url
        url = concat_urls(api_url, 'management')
      end
      super url, api_url, options
    end

    def create_organization(organization, username, name, email, password)
      data = { organization: organization,
               username: username,
               name: name,
               email: email,
               password: password }
      self['organizations'].post data
    end

    def organizations
      self[__method__].get
    end

    def organization(organization)
      url = self["organizations/#{organization}"].url
      Organization.new url, options
    end

    def users
      self['users'].get
    end

    def user(name_or_uuid)
      self["users/#{name_or_uuid}"].get
    end

  end
end
