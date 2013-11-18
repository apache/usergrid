module Usergrid
  class Management < Resource

    def initialize(url, options={})
      management = url.split('/')[-1]
      if management == 'management'
        api_url = url[0..url.index(management)-2]
      else
        api_url = url
        url = concat_urls(api_url, 'management')
      end
      super url, api_url, options
    end

    def create_organization(organization, username, name, email, password)
      data = { organization: organization,
               username: username,
               name: name,
               email: email,
               password: password }
      self['organizations'].post data
    end

    def organizations
      self[__method__].get
    end

    def organization(organization)
      url = self["organizations/#{organization}"].url
      Organization.new url, options
    end

    def users
      self['users'].get
    end

    def user(name_or_uuid)
      self["users/#{name_or_uuid}"].get
    end

  end
end
