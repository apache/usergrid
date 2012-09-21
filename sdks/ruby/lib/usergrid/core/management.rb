module Usergrid
  class Management < Resource

    def initialize(api_url, options={})
      url = concat_urls(api_url, 'management')
      super url, api_url, options
    end

    # one way: cannot delete organizations
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
      Organization.new url, api_url, options
    end

    def users
      self['users'].get
    end

    def user(name_or_uuid)
      self["users/#{name_or_uuid}"].get
    end

  end
end
