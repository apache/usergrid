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

describe Usergrid::Organization do

  before :each do
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

  describe "grant_type: client_credentials" do
    context "invalid credentials" do
      before :each do
        @organization.logout
      end

      it "should not be able to get access token with invalid credentials" do
        expect { @organization.login_credentials "invalid_client_id", "invalid_client_secret" }.to raise_exception RestClient::BadRequest
      end

      it "should not be able to get access token with empty credentials" do
        expect { @organization.login_credentials "", "" }.to raise_exception RestClient::BadRequest
      end
    end

    context "valid crendentials" do
      it "should be able to get access token with valid credentials" do
        org_credentials = JSON.parse @organization.credentials
        @organization.logout
        @organization.login_credentials org_credentials["credentials"]["client_id"], org_credentials["credentials"]["client_secret"]

        expect(@organization.auth_token).to_not be_empty
        expect(@organization.auth_token).to be_instance_of String
      end
    end
  end
end
