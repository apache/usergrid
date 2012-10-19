# overrides methods dealing with auth_token to operate on a thread basis
module Usergrid
  class Resource
    def options
      options = @options.clone
      auth_token = Thread.current[:usergrid_auth_token]
      options[:headers].delete :Authorization
      options[:headers][:Authorization] = "Bearer #{auth_token}" if auth_token
      options
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
