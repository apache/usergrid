module Ugc
  class Settings

    SETTINGS_FILE = 'settings.yml'

    def initialize(directory)
      @file = File.join directory, SETTINGS_FILE
      @settings = YAML.load_file(@file) rescue default_settings
    end

    def active_profile_name
      @settings['active_profile'] || 'local'
    end

    def active_profile_name=(name)
      raise "unknown profile: #{name}" unless profile(name)
      @settings['active_profile'] = name
      save
    end

    def delete_profile(name)
      raise "cannot delete active profile" if active_profile_name == name
      raise "unknown profile: #{name}" unless profiles[name]
      profiles.delete name
      save
    end

    def profile(name=nil)
      name ||= active_profile_name
      profiles[name] || profiles[name] = {}
    end

    def profiles
      @settings['profiles'] || @settings['profiles'] = {}
    end

    def base_url
      profile['base_url']
    end

    def base_url=(url)
      URI.parse url
      set_profile 'base_url', url
    end

    def organization
      profile['organization']
    end

    def organization=(org)
      set_profile 'organization', org
    end

    def application
      profile['application']
    end

    def application=(app)
      set_profile 'application', app
    end

    def access_token
      profile['access_token']
    end

    def access_token=(token)
      set_profile 'access_token', token
    end

    def configured?
      base_url && organization && application
    end

    def logged_in?
      !!access_token
    end

    private

    def set_profile(prop, value)
      profile[prop] = value
      save
    end

    def save
      Dir.mkdir(File.dirname(@file)) unless Dir.exist?(File.dirname(@file))
      File.open(@file, 'w') do |out|
        YAML.dump(@settings, out)
      end
    end

    def default_settings
      {
        'target' => 'https://api.usergrid.com',
        'active_profile' => 'local',
        'profiles' => {
          'local' => {
              'base_url' => 'http://localhost:8080',
              'organization' => 'test-organization',
              'application' => 'test-app'
          },
          'apigee' => {
            'base_url' => 'https://api.usergrid.com'
          }
        }
      }
    end

  end
end