# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

describe Usergrid::Application do

  before :all do
    @application = create_random_application
    @user = create_random_user @application, true
  end

  after :all do
    @user.delete
    delete_application @application
  end

  it "should be able to create a user using deprecated syntax" do
    random = SecureRandom.hex
    response = @application.create_user "username_#{random}", 'password'
    response.entity.uuid.should_not be_nil
  end

  it "should be able to create, login, and delete a user" do
    random = SecureRandom.hex
    application = Usergrid::Application.new @application.url # create application resource that's not logged in
    response = application.create_user username: "username_#{random}", password: 'password'
    entity = response.entity
    application.login "username_#{random}", 'password'
    begin
      response.code.should eq 200
      response.entities.size.should eq 1
      entity.uuid.should_not be_nil
      response = application["users/#{entity.uuid}"].get
      response.code.should eq 200
    ensure
      entity.delete
      # deleted user shouldn't be able to access anything now
      expect { application["users/#{entity.uuid}"].get }.to raise_error(RestClient::Unauthorized)
      # use original application - logged in under existing user
      expect { @application["users/#{entity.uuid}"].get }.to raise_error(RestClient::ResourceNotFound)
    end
  end

  it "should be able to retrieve users" do
    entity1 = create_random_user @application
    entity2 = create_random_user @application
    begin
      response = @application.users
      response.entities.size.should be >= 2
      match1 = match2 = false
      response.entities.each do |e|
        match1 ||= e.username == entity1.username
        match2 ||= e.username == entity2.username
      end
      match1.should be_true
      match2.should be_true
    ensure
      entity1.delete
      entity2.delete
    end
    response = @application.users
    response.code.should eq 200
    response.entities.should be_an Array
  end

  it "should be able to query users" do
    entity1 = create_random_user @application
    entity2 = create_random_user @application
    begin
      response = @application.users "select * where username = \'#{entity1.username}\'"
      response.entities.size.should eq 1
      response.entity.username.should eq entity1.username
    ensure
      entity1.delete
      entity2.delete
    end
  end

  it "should be able to update a user" do
    entity = create_random_user @application
    begin
      updates = { email: 'testuser6@email.com', city: 'santa clarita' }
      response = @application.put updates
      updates.each {|k,v| response.entity[k.to_s].should eq v }
    ensure
      entity.delete
    end
  end

  it "should be able to retrieve the logged in user" do
    user = @application.current_user
    user.uuid.should_not be_nil
  end

  it "should be able to add and retrieve activities" do
    user = @application.current_user
    activity = {
        verb: "post",
        content: "testy, testy",
        actor: {
          uuid: user.uuid,
          email: user.email,
          username: user.username,
          displayName: user.username,
          image: {
            height: 80,
            width: 80,
            url: user.picture
          }}}
    response = @application['activities'].post activity
    activity_path = response.entity['metadata']['path']
    response.code.should eq 200

    begin
      entity = @application.activities.entity
      activity.add_dot_notation!
      entity.verb.should eq activity.verb
      entity.content.should eq activity.content
      entity.actor.uuid.should eq activity.actor.uuid
    ensure
      @application[activity_path].delete
    end
  end

  it "should be able to retrieve a user's feed" do
    response = @application.current_user.resource['feed'].get
    response.code.should eq 200
    response.entities.should be_an Array
  end

  it "should be able to follow a user" do
    follower = create_random_user @application
    begin
      me = @application.current_user
      @application["users/#{follower.uuid}/following/users/#{me.uuid}"].post "{}"

      response = @application["users/#{follower.uuid}/following"].get
      response.entities.size.should == 1
      response.entity.uuid.should eq me.uuid

      response = @application.current_user.resource['followers'].get
      response.entities.size.should be > 0
    ensure
      follower.delete
    end
  end

  it "should be able to create and retrieve groups" do
    response = @application.groups
    size = response.entities.size
    random = SecureRandom.hex
    @application.create_entity :groups, path: "test-#{random}"
    response = @application.groups
    response.entities.size.should eq size+1
  end

  it "should be able to create and retrieve devices" do
    response = @application.devices
    size = response.entities.size
    random = SecureRandom.hex
    @application.create_entity :devices, path: "test-#{random}"
    response = @application.devices
    response.entities.size.should eq size+1
  end

  it "should be able to create and retrieve assets" do
    response = @application.assets
    size = response.entities.size
    random = SecureRandom.hex
    @application.create_entity :assets, path: "test-#{random}", owner: @application.current_user.uuid
    response = @application.assets
    response.entities.size.should eq size+1
  end

  it "should be able to create and retrieve folders" do
    response = @application.folders
    size = response.entities.size
    random = SecureRandom.hex
    @application.create_entity :folders, path: "test-#{random}", owner: @application.current_user.uuid
    response = @application.folders
    response.entities.size.should eq size+1
  end

  # can't reliably test this - counters are batched on server
  #it "should be able to create and retrieve events and retrieve counters" do
  #  # clear events
  #  {} while @application.events.entities.size > 0
  #
  #  events_in = []
  #  events_in  << {timestamp: 0, category: 'test', counters: { test: 1 }}
  #  events_in  << {timestamp: 0, category: 'testme', counters: { testme: 1 }}
  #  events_in  << {timestamp: 0, counters: { test: 1 }}
  #  events_in.each {|e| @application.create_entity :events, e }
  #
  #  events_out = []
  #  events_out << @application.events.entity
  #  events_out << @application.events.entity
  #  events_out << @application.events.entity
  #
  #  (0..2).each do |i|
  #    events_in[i][:category].should eq events_out[i]['category']
  #  end
  #
  #  response = @application.events
  #  response.entities.size.should eq 0
  #
  #  # get and test counters
  #  counter_names = @application.counter_names
  #  counter_names.should include 'test'
  #  counter_names.should include 'testme'
  #
  #  response = @application.counter 'test'
  #  counter = response.data.counters.first
  #  counter.name.should eq 'test'
  #  counter.values.last.first.value.should be > 0
  #end

  it "should be able to create, retrieve, and delete roles" do
    size = @application.roles.collection.size

    role_name = "test-#{SecureRandom.hex}"
    @application.create_entity :role, name: role_name, title: 'title', inactivity: 0
    roles = @application.roles.collection
    roles.size.should eq size+1
    role = roles.detect {|r| r.name == role_name }
    role.should_not be_nil

    role.delete
    @application.roles.collection.size.should eq size
  end

  it "should be able to add/remove permissions from a user" do
    user = create_random_user @application
    permission = 'post:/users/*'
    user.resource['permissions'].post permission: permission

    permissions = user.resource['permissions'].get.data.data
    permissions.size.should eq 1
    permissions.first.should eq permission

    user.resource['permissions'].delete({params: { permission: permission }})

    permissions = user.resource['permissions'].get.collection.entities
    permissions.size.should eq 0
  end

  it "should be able to add/remove a user from a roles" do
    user = create_random_user @application

    roles = user.resource['roles'].get.entities
    size = roles.size

    @application["roles/admin/users/#{user.uuid}"].post nil

    roles = user.resource['roles'].get.entities
    roles.size.should == size+1

    @application["roles/admin/users/#{user.uuid}"].delete

    roles = user.resource['roles'].get.entities
    roles.size.should == size
  end

  it "should be able to create a new collection and access it" do
    entities = (1..4).collect { |i| { name: "test_#{i}" } }
    @application.create_entities 'tests', entities
    response = @application['tests'].get
    collection = response.collection
    collection.size.should eq 4
  end

  it "should be able to create a new collection via create_ method and access it" do
    entities = (1..4).collect { |i| { name: "test_#{i}" } }
    @application.create_moretests entities
    response = @application['tests'].get
    collection = response.collection
    collection.size.should eq 4
  end

  it "should be able to access a collection without calling get" do
    entities = (1..4).collect { |i| { name: "test_#{i}" } }
    @application.create_entities 'tests', entities
    collection = @application['tests'].collection
    collection.size.should eq 4
  end

  it "should be able to access entities without calling get" do
    entities = (1..4).collect { |i| { name: "test_#{i}" } }
    @application.create_entities 'tests', entities
    entities = @application['tests'].entities
    entities.size.should eq 4
  end

  it "should be able to query using dot notation" do
    entities = (1..4).collect { |i| { name: "test_#{i}" } }
    @application.create_btests entities
    entities = @application.btests("name = 'test_1'").entities
    entities.size.should eq 1
  end

  describe "grant_type: client_credentials" do

    before(:each) do
      @app = create_random_application
    end

    context "invalid credentials" do
      it "should not be able to get access token with invalid credentials" do
        expect { @app.login_credentials "invalid_client_id", "invalid_client_secret" }.to raise_exception RestClient::BadRequest
      end

      it "should not be able to get access token with empty credentials" do
        expect { @app.login_credentials "", "" }.to raise_exception RestClient::BadRequest
      end
    end

    context "valid credentials" do
      it "should be able to get access token with valid credentials" do
        app_info = JSON.parse @app.get

        management = login_management
        app = management.application app_info["organization"], app_info["applicationName"]
        app_credentials = JSON.parse app.credentials
        app.login_credentials app_credentials["credentials"]["client_id"], app_credentials["credentials"]["client_secret"]

        expect(app.auth_token).to_not be_empty
        expect(app.auth_token).to be_instance_of String
      end
    end
  end
end