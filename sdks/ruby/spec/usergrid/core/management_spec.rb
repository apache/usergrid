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
