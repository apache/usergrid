module Usergrid
  class Collection < Entity
    include Enumerable

    attr_reader :query_params

    def initialize(url, api_url, options={}, response=nil)
      super url, api_url, options, response
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
    end

    def create_entity(data)
      response = self.post data
      response.entity
    end

    # options: 'reversed', 'start', 'cursor', 'limit', 'permission'
    #def update(new_data, query=nil, options={}) # todo: enable when server is fixed
    #  options = options.symbolize_keys
    #  @query_params = query ? options.merge({ql: query}) : options
    #  self.put({params: @query_params }, new_data)
    #  self
    #end

    # options: 'reversed', 'start', 'cursor', 'limit', 'permission'
    def query(query=nil, options={})
      options = options.symbolize_keys
      @query_params = query ? options.merge({ql: query}) : options
      self.get({params: @query_params })
      self
    end

    def size
      entities.size
    end

    def cursor
      response.data.cursor || nil
    end

    def next_page
      query(nil, @query_params.merge({cursor: cursor}))
    end

  end
end
