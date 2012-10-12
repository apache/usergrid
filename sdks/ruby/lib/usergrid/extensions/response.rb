module RestClient
  module Response

    def resource=(resource)
      @resource = resource
    end

    def resource
      @resource
    end

    def data
      @data = MultiJson.load(self).add_dot_notation! unless @data
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
      entities_data = data['entities'] || data['data'] || data['messages'] || data['list']
      raise "unable to determine entities from: #{data}" unless entities_data.is_a?(Array)
      entities_data.each do |e|
        e['uri'] = concat_urls(data['uri'], e['uuid']) if e.is_a?(Hash) && e['uuid']
      end
      @entities_data = entities_data
    end

    def entities
      return @entities if @entities
      index = -1
      @entities = entities_data.collect do |e|
        if e.is_a? Array
          e
        else
          Usergrid::Entity.new e['uri'], resource.api_url, resource.options, self, index+=1
        end
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
      Usergrid::Entity.new(entity_data['uri'], resource.api_url, resource.options, self) if entity_data
    end

    protected

    def concat_urls(url, suburl) # :nodoc:
      url = url.to_s
      suburl = suburl.to_s
      if url.slice(-1, 1) == '/' or suburl.slice(0, 1) == '/'
        url + suburl
      else
        "#{url}/#{suburl}"
      end
    end

  end
end
