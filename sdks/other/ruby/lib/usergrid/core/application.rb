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
  class Application < Resource

    def initialize(url, options={})
      org_name = url.split('/')[-2]
      api_url = url[0..url.index(org_name)-2]
      super url, api_url, options
    end

    # note: collection_name s/b plural, but the server will change it if not
    def create_entity(collection_name, entity_data)
      self[collection_name].post entity_data
    end
    alias_method :create_entities, :create_entity

    # allow create_something(hash_or_array) method
    def method_missing(method, *args, &block)
      method_s = method.to_s
      if method_s.start_with? 'create_'
        entity = method_s.split('_')[1]
        return _create_user *args if entity == 'user' && args[0].is_a?(String) # backwards compatibility
        create_entity entity, *args
      elsif method_s.end_with? 's' # shortcut for retrieving collections
        self[method].query(*args)
      else
        super method, args, block
      end
    end

    def counter_names
      self['counters'].get.data.data
    end

    # other_params: 'start_time' (ms), 'end_time' (ms), 'resolution' (minutes)
    def counter(name, other_params={})
      options = other_params.merge({counter: name})
      self['counters'].get({params: options})
    end

    # login with Facebook token. matching user will be created in usergrid as needed.
    # usergrid auth token automatically set in auth header for future requests
    def facebook_login(access_token)
      params = { fb_access_token: access_token }
      response = self['auth/facebook'].get({ params: params })
      self.auth_token = response.data['access_token']
      user_uuid = response.data['user']['uuid']
      @current_user = self["/users/#{user_uuid}"].get.entity
      response
    end

    def login_credentials(client_id, client_secret)
      response = self['token'].post grant_type: 'client_credentials', client_id: client_id, client_secret: client_secret
      self.auth_token = response.data['access_token']
    end

    private

    def _create_user(username, password, email=nil, name=nil, invite=false)
      LOG.warn "create_user(username, password, ...) is deprecated"
      user_hash = { username: username,
                    password: password,
                    email: email,
                    name: name,
                    invite: invite }
      create_entity 'users', user_hash
    end

  end
end
