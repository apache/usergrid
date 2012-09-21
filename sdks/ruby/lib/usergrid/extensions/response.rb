module RestClient
  module Response

    def resource=(resource)
      @resource = resource
    end

    def resource
      @resource
    end

    def data
      @data = JSON.parse(self).add_dot_notation! unless @data
      @data
    end

    def collection
      resource.collection
    end

    def multiple_entities?
      entities_data = data['entities'] || data['data'] || data['messages']
      entities_data.is_a? Array
    end

    def entities_data
      return @entities_data if @entities_data
      entities_data = data['entities'] || data['data'] || data['messages']
      raise "unable to determine entities from: #{data}" unless entities_data.is_a?(Array)
      entities_data.each do |e|
        e['uri'] = resource.concat_urls(data['uri'], e['uuid']) if e['uuid']
      end
      @entities_data = entities_data
    end

    def entities
      return @entities if @entities
      index = -1
      @entities = entities_data.collect do |e|
        Usergrid::Entity.new e['uri'], resource.api_url, resource.options, self, index+=1
      end
    end

    def entity_data
      if multiple_entities?
        entities_data.first
      elsif data['data']
        d = data['data']
        d['uri'] = @resource.url
        d
      elsif data['organization']
        d = data['organization']
        d['uri'] = @resource.url
        d
      else
        entities_data.first
      end
    end

    def entity
      Usergrid::Entity.new entity_data['uri'], resource.api_url, resource.options, self
    end
  end
end
