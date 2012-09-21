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
