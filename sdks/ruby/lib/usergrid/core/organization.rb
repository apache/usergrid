require 'uri'
module Usergrid
  class Organization < Resource

    def initialize(url, options={})
      org_name = url.split('/')[-1]
      api_url = url[0..url.index('management')-2]
      super url, api_url, options
    end

    def name
      URI(url).path.split('/').last
    end

    def create_application(name)
      self['applications'].post({ name: name })
      application name
    end

    def applications(query=nil)
      resource = self[__method__]
      response = query ? resource.query(query) : resource.get
      response.data['data'].collect do |k|
        application concat_urls(api_url, k)
      end
    end

    def application(name_or_uuid)
      Usergrid::Application.new concat_urls(api_url, "#{name}/#{name_or_uuid}"), options
    end

    def users(query=nil)
      self[__method__].query(query)
    end

    def user(user)
      management.user(user)
    end

    def feed(query=nil)
      self[__method__].query(query)
    end

    def credentials
      self[__method__].get
    end

    def generate_credentials
      self['credentials'].post nil
    end

    def login_credentials(client_id, client_secret)
      resource = api_resource 'management'
      response = resource['token'].post grant_type: 'client_credentials', client_id: client_id, client_secret: client_secret
      self.auth_token = response.data['access_token']
    end

  end
end
