describe Usergrid::Organization do

  before :all do
    @management = Usergrid::Resource.new(SPEC_SETTINGS[:api_url]).management
    @management.login SPEC_SETTINGS[:organization][:username], SPEC_SETTINGS[:organization][:password]
    @organization = @management.organization SPEC_SETTINGS[:organization][:name]
  end

  it "should be able to create (and delete) an application" do
    app_name = "test_app_#{SecureRandom.hex}"
    app = @organization.create_application app_name
    app.should be_an Usergrid::Application

    response = @organization["applications/#{app_name}"].delete
    response.code.should eq 200
  end

  it "should be able to get applications" do
    response = @organization.applications
    response.should be_an Array
    if response.size > 0
      app = response.first
      app.should be_an Usergrid::Application
    end
  end

  it "should be able to get users" do
    response = @organization.users
    response.code.should eq 200
    response.entities.should be_an Array
    response.entities[0].uuid.should_not be_nil
  end

  it "should be able to get a user" do
    response = @organization.user(SPEC_SETTINGS[:organization][:username])
    response.code.should eq 200
    response.entity.uuid.should_not be_nil
  end

  it "should be able to get an application" do
    app_name = "test_app_#{SecureRandom.hex}"
    @organization.create_application app_name
    begin
      app = @organization.application app_name
      app.should be_an Usergrid::Application
    ensure
      @organization["applications/#{app_name}"].delete
    end
  end

  it "should be able to get feed" do
    response = @organization.feed
    entities = response.entities
    entities.size.should be > 0
    entities.first.uuid.should_not be_nil
  end

  it "should be able to get credentials" do
    response = @organization.credentials
    response.code.should eq 200
    response.data.credentials.client_id.should_not be_nil
    response.data.credentials.client_secret.should_not be_nil
  end

  it "should be able to generate credentials" do
    response = @organization.generate_credentials
    response.code.should eq 200
    response.data.credentials.client_id.should_not be_nil
    response.data.credentials.client_secret.should_not be_nil
  end

end
