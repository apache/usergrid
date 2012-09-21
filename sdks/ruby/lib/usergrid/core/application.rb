module Usergrid
  class Application < Resource

    def initialize(url, api_url, options)
      super url, api_url, options
    end

    def create_user(username, name, email, password, invite=false)
      data = { username: username,
               name: name,
               email: email,
               password: password,
               invite: invite }
      create_entity 'users', data
    end

    # note: collection_name s/b plural!
    def create_entity(collection_name, entity_data)
      self[collection_name].post entity_data
    end
    alias_method :create_entities, :create_entity

    def users(query=nil, options={})
      self[__method__].query(query, options)
    end

    def groups(query=nil, options={})
      self[__method__].query(query, options)
    end

    def activities(query=nil, options={})
      self[__method__].query(query, options)
    end

    def devices(query=nil, options={})
      self[__method__].query(query, options)
    end

    def assets(query=nil, options={})
      self[__method__].query(query, options)
    end

    def folders(query=nil, options={})
      self[__method__].query(query, options)
    end

    def events(query=nil, options={})
      self[__method__].query(query, options)
    end

    def roles(query=nil, options={})
      self[__method__].query(query, options)
    end

    def rolenames(query=nil, options={})
      self[__method__].query(query, options).data.data.keys
    end

    def counter_names
      self['counters'].get.data.data
    end

    # other_params: 'start_time' (ms), 'end_time' (ms), 'resolution' (minutes)
    def counter(name, other_params={})
      options = other_params.merge({counter: name})
      self['counters'].get({params: options})
    end

  end
end
