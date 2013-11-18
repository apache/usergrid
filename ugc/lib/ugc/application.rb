module Ugc
  class Application < Usergrid::Application

    def initialize
      super application_url
      self.auth_token = $settings.access_token
    end

    def application_url
      url = $settings.base_url
      org = $settings.organization
      app = $settings.application

      raise "not configured" unless url && org && app

      concat_urls url, "#{org}/#{app}"
    end

    def login(username, password)
      super username, password
      $settings.access_token = auth_token
    end

    def [](uri)
      uri = perform_substitutions uri
      if (URI.parse(uri).host rescue nil)
        Usergrid::Resource.new(uri, nil, $application.options) # absolute
      else
        super # relative
      end
    end

    def list_collections
      app = $application.entity
      collections = app['metadata']['collections']
      table border: $settings.table_border? do
        row header: true do
          collections.first[1].each_key do |k|
            column k
          end
        end
        collections.each_value do |coll|
          row do
            coll.each_value do |v|
              column v
            end
          end
        end
      end
    end
  end
end
