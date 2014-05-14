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

# also allows hash or dot notation for entity properties
module Usergrid
  class Entity < Resource

    def initialize(url, api_url, options={}, response=nil, index=nil)
      super url, api_url, options, response
      @index = index
    end

    def data
      get unless response
      @index ? response.entities_data[@index] : response.entity_data
    end

    def data?
      !!response
    end

    def resource
      Resource.new url, api_url, options
    end

    def [](k)
      data[k]
    end

    def []=(k,v)
      data[k] = v
    end

    def collection
      Collection.new url[0..url[0..-2].rindex('/')-1], api_url, options
    end

    def save
      self.put data
    end

    def to_s
      "resource: #{url}\ndata: #{data}"
    end

    def to_json(*args)
      data.except(RESERVED).to_json *args
    end
    alias :encode :to_json
    alias :dump :to_json

    private

    def method_missing(method, *args, &block)
      if data.respond_to?(method)
        data.send(method, *args, &block)
      elsif method[-1] == '=' && args.size == 1
        data[method[0..-2]] = args[0]
      else
        super method, args, block
      end
    end

    def respond_to?(method)
      super.respond_to?(method) || data.respond_to?(method)
    end
  end
end
