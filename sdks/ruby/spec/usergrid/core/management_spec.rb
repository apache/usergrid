describe Usergrid::Management do

  before :all do
    @base_resource = Usergrid::Resource.new(SPEC_SETTINGS[:api_url])
    @management = @base_resource.management
    @management.login SPEC_SETTINGS[:management][:username], SPEC_SETTINGS[:management][:password]
  end

  it "should be able to create an organization" do
    # can't delete an org or a user, so we create new random ones and leave them
    random = SecureRandom.hex
    org_name = "test_org_#{random}"
    user_name = "test_admin_#{random}"
    response = @management.create_organization("#{org_name}",
                                               "#{user_name}",
                                               "#{user_name}",
                                               "#{user_name}@email.com",
                                               "#{random}")
    response.code.should eq 200
    #@base_resource["users/#{user_name}"].delete
  end

  # this is broken on the server
  #it "should be able to get organizations" do
  #  response = @management.organizations
  #end

  it "should be able to get an organization" do
    organization = @management.organization SPEC_SETTINGS[:organization][:name]
    organization.should be_a Usergrid::Organization
    org_ent = organization.get.entity
    org_ent.uuid.should_not be_nil
    org_ent.name.should eq SPEC_SETTINGS[:organization][:name]
  end

end
