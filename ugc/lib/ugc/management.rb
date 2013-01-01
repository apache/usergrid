module Ugc
  class Management < Usergrid::Management

    def initialize
      super management_url
      auth_token = $settings.access_token
    end

    def management_url
      raise "not configured" unless $settings.base_url
      $settings.base_url
    end

    def login(username, password)
      super username, password
      $settings.access_token = auth_token
    end

  end
end
