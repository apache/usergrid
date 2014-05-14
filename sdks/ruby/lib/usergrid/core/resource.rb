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

require 'rest_client'

module Usergrid
  class Resource < RestClient::Resource

    RESERVED = %w( created modified metadata uri )
    DEFAULT_API_URL = 'https://api.usergrid.com'
    TYPE_HEADERS = { content_type: :json, accept: :json }

    attr_reader :current_user, :api_url, :response

    def initialize(resource_url=DEFAULT_API_URL, api_url=nil, options={}, response=nil)
      options[:headers] = TYPE_HEADERS.merge options[:headers] || {}
      @api_url = api_url || resource_url
      self.response = response
      super resource_url, options, &method(:handle_response)
    end

    # gets user token and automatically set auth header for future requests
    # precondition: resource must already be set to the correct context (application or management)
    def login(username, password)
      response = self['token'].post grant_type: 'password', username: username, password: password
      self.auth_token = response.data['access_token']
      user_uuid = response.data['user']['uuid']
      @current_user = self["/users/#{user_uuid}"].get.entity
      response
    end

    # remove auth header for future requests
    # only affects self and derivative resources
    def logout
      self.auth_token = nil
      @current_user = nil
    end

    def logged_in?
      !!auth_token
    end

    def management
      Usergrid::Management.new api_url, options
    end

    # application defaults to sandbox if none provided
    def application(organization, application='sandbox')
      Usergrid::Application.new concat_urls(api_url, "#{organization}/#{application}"), options
    end

    # options: 'reversed', 'start', 'cursor', 'limit', 'permission'
    def query(query=nil, options={})
      options = options.merge({ql: query}) if query
      get({params: options})
    end

    # options: 'reversed', 'start', 'cursor', 'limit', 'permission'
    def update_query(updates, query=nil, options={})
      options = options.merge({ql: query}) if query
      put(updates, {params: options})
    end

    # options: 'reversed', 'start', 'cursor', 'limit', 'permission'
    def delete_query(query=nil, options={})
      options = options.merge({ql: query}) if query
      delete({params: options})
    end

    def entity
      get unless response
      response.entity
    end

    def entities
      get unless response
      response.entities
    end

    def collection
      get unless response
      Collection.new url, api_url, options, response
    end

    # overridden to ensure sub resources are instances of this class
    def [](suburl, &new_block)
      case
        when block_given? then Resource.new(concat_urls(url, suburl), api_url, options, &new_block)
        when block        then Resource.new(concat_urls(url, suburl), api_url, options, &block)
        else
          Resource.new(concat_urls(url, suburl), api_url, options)
      end
    end

    def api_resource(suburl)
      Resource.new(concat_urls(api_url, suburl), api_url, options)
    end

    def get(additional_headers={}, &block)
      self.response = super additional_headers, &block
    end

    def post(payload, additional_headers={}, &block)
      payload = MultiJson.dump(payload) unless payload.is_a? String
      self.response = super payload, additional_headers, &block
    end

    def put(payload, additional_headers={}, &block)
      payload = MultiJson.dump(payload) unless payload.is_a? String
      self.response = super payload, additional_headers, &block
    end

    def auth_token=(auth_token)
      if auth_token
        @options[:headers].merge!({ Authorization: "Bearer #{auth_token}" })
      else
        @options[:headers].delete :Authorization if @options
      end
    end

    def auth_token
      begin
        @options[:headers][:Authorization].gsub 'Bearer ', ''
      rescue
        nil
      end
    end

    protected

    def response=(response)
      @response = response
    end

    # add verbose debugging of response body
    def handle_response(response, request, result, &block)
      LOG.debug "response.body = #{response}"
      response = response.return!(request, result, &block)
      response.resource = self
      self.response = response
      response
    end

  end
end
