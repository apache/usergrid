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
  class Collection < Entity
    include Enumerable

    attr_accessor :iterator_follows_cursor

    def initialize(url, api_url, options={}, response=nil)
      super url, api_url, options, response
      @iterator_follows_cursor = false
    end

    def collection
      self
    end

    def entities
      get unless response
      response.entities
    end

    def [](k)
      entities[k]
    end

    def []=(k,v)
      raise "unsupported operation"
    end

    # does not save entities! just fields (eg. 'name')
    def save
      super save
    end

    def each(&block)
      entities.each &block
      while cursor
        next_page
        entities.each &block
      end if iterator_follows_cursor
    end

    # use in conjunction with each() like: collection.follow_cursor.each {|e| }
    def follow_cursor
      my_clone = self.clone
      my_clone.iterator_follows_cursor = true
      my_clone
    end

    def create_entity(data)
      self.post data
    end
    alias_method :create_entities, :create_entity

    # options: 'reversed', 'start', 'cursor', 'limit', 'permission'
    def update_query(updates, query=nil, options={})
      options = options.symbolize_keys
      query_params = query ? options.merge({ql: query}) : options
      self.put(updates, {params: query_params })
      self
    end

    # options: 'reversed', 'start', 'cursor', 'limit', 'permission'
    def delete_query(query=nil, options={})
      options = options.symbolize_keys
      query_params = query ? options.merge({ql: query}) : options
      self.delete({params: query_params })
      self
    end

    # options: 'reversed', 'start', 'cursor', 'limit', 'permission'
    def query(query=nil, options={})
      options = options.symbolize_keys
      query_params = query ? options.merge({ql: query}) : options
      self.get({params: query_params })
      self
    end

    def size
      entities.size
    end

    def empty?
      entities.empty?
    end

    def cursor
      response.data['cursor']
    end

    def next_page
      query(nil, query_params.merge({cursor: cursor}))
    end

    protected

    def query_params
      params = {}
      if response.data['params']
        response.data['params'].each do |k,v|
          params[k] = v[0]
        end
      end
      params
    end
  end
end
