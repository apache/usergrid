# overrides methods dealing with auth_token to operate on a thread basis
module Usergrid
  class Resource

    def options
      options = @options.clone
      require_login = Ironhorse::Base.settings[:require_login] != false
      if require_login
        auth_token = Thread.current[:usergrid_auth_token]
      else
        unless Ironhorse::Base.settings[:auth_token]
          as_admin {}
        end
        auth_token = Ironhorse::Base.settings[:auth_token]
      end
      options[:headers].delete :Authorization
      options[:headers][:Authorization] = "Bearer #{auth_token}" if auth_token
      options
    end

    def as_admin(&block)
      save_auth_token = Thread.current[:usergrid_auth_token]
      begin
        unless Ironhorse::Base.settings[:auth_token]
          resource = RestClient::Resource.new Ironhorse::Base.settings[:application_url]
          response = resource['token'].post grant_type: 'client_credentials', client_id: Ironhorse::Base.settings[:client_id], client_secret: Ironhorse::Base.settings[:client_secret]
          Ironhorse::Base.settings[:auth_token] = MultiJson.load(response)['access_token']
        end
        Thread.current[:usergrid_auth_token] = Ironhorse::Base.settings[:auth_token]
        yield block
      ensure
        Thread.current[:usergrid_auth_token] = save_auth_token
      end
    end

    # gets user token and automatically set auth header for future requests on this Thread
    # precondition: resource must already be set to the correct context (application or management)
    def login(username, password)
      params = { grant_type: "password", username: username, password: password }
      response = self['token'].get({ params: params })
      user_uuid = response.data['user']['uuid']
      user_access_token = response.data['access_token']
      Thread.current[:usergrid_user_id] = user_uuid
      Thread.current[:usergrid_auth_token] = user_access_token
      @current_user = self["/users/#{user_uuid}"].get.entity
      response
    end

  end
end
