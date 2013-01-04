module Ugc
  class Settings

    SETTINGS_FILE = 'settings.yml'

    def initialize(global_options)
      @draw_table_border = global_options[:border]
      @settings_file = File.join global_options[:settings], SETTINGS_FILE
      @settings = YAML.load_file(@settings_file) rescue default_settings
    end

    def active_profile_name
      @settings['active_profile'] || 'local'
    end

    def active_profile_name=(name)
      raise "unknown profile: #{name}" unless profile(name)
      @settings['active_profile'] = name
      save_profile
    end

    def delete_profile(name)
      raise "cannot delete active profile" if active_profile_name == name
      raise "unknown profile: #{name}" unless profiles[name]
      profiles.delete name
      save_profile
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

    def table_border?
      !!@draw_table_border
    end

    def logged_in?
      !!access_token
    end

    def save_profile_blob(name, data)
      file = File.join settings_dir, "#{active_profile_name}.#{name}"
      File.open(file, 'w') do |out|
        out.write data
      end
    end

    def load_profile_blob(name)
      IO.read File.join settings_dir, "#{active_profile_name}.#{name}"
    end

    private

    def set_profile(prop, value)
      profile[prop] = value
      save_profile
    end

    def settings_dir
      File.dirname(@settings_file)
    end

    def save_profile
      Dir.mkdir(settings_dir) unless Dir.exist?(settings_dir)
      File.open(@settings_file, 'w') do |out|
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