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


describe Usergrid::Ironhorse::Base do

  it_should_behave_like 'ActiveModel'

  class User < Usergrid::Ironhorse::Base
    extend Usergrid::Ironhorse::UserContext
  end

  before :all do
    @application = create_random_application
    Foo.configure!(@application.url, @application.auth_token)
    @user = create_random_user @application, true
    @foo = (@application.create_entity 'foos', name: 'foo42', answer: 42).entity
  end

  after :all do
    @foo.delete
    @user.delete
    #delete_application @application # not supported on server yet
  end

  class Foo < Usergrid::Ironhorse::Base; end
  Foo.validates :name, :presence => true

  class Bar < Usergrid::Ironhorse::Base; end

  describe 'subclasses should be able to' do

    it "do tasks as admin when requested" do
      organization = @foo.management.organization SPEC_SETTINGS[:organization][:name]
      organization.logout

      # should fail under current user's context
      expect {
        organization.create_application "_test_app_#{SecureRandom.hex}"
      }.to raise_error RestClient::Unauthorized

      # should succeed under admin context
      User.as_admin do
        organization.create_application "_test_app_#{SecureRandom.hex}"
      end
    end

    it "do tasks as admin if require_login is false" do
      organization = @foo.management.organization SPEC_SETTINGS[:organization][:name]
      organization.logout

      # should fail under current user's context
      expect {
        organization.create_application "_test_app_#{SecureRandom.hex}"
      }.to raise_error RestClient::Unauthorized

      # should succeed once require_login is false
      User.settings[:require_login] = false
      organization.create_application "_test_app_#{SecureRandom.hex}"
      User.settings[:require_login] = true
    end

    it 'be created and destroyed' do
      foo = Foo.create name: 'foo man'
      foo.persisted?.should be_true
      foo.name.should eq 'foo man'
      foo = Foo.find_by_name 'foo man'
      foo.should_not be_nil
      foo.destroy.should be_true
      foo.persisted?.should be_false
      foo = Foo.find_by_name 'foo man'
      foo.should be_nil
    end

    it 'be changed and saved' do
      foo = Foo.find_by_name @foo.name
      foo.answer.should eq @foo.answer
      foo.number = 43
      foo.changed?.should be_true
      foo.number.should == 43
      foo.save!
      foo = Foo.find_by_name @foo.name
      foo.number.should == 43
    end

    it 'be reloaded' do
      foo = Foo.find @foo.uuid
      foo.answer = 44
      foo.changed?.should be_true
      foo.answer.should == 44
      foo.reload
      foo.answer.should == 42
    end

    it 'be found using find_by_name' do
      foo = Foo.find_by_name @foo.name
      foo.uuid.should_not be_nil
      foo.name.should eq @foo.name
      foo.should be_a Foo
      foo.persisted?.should be_true
    end

    it 'be found using find_by_name!' do
      foo = Foo.find_by_name! @foo.name
      foo.uuid.should_not be_nil
      foo.name.should eq @foo.name
      foo.should be_a Foo
      foo.persisted?.should be_true
    end

    it 'throw a RecordNotFound when find_by_name! misses' do
      expect { Foo.find_by_name! 'name3' }.to raise_error(ActiveRecord::RecordNotFound)
    end

    it 'add a validation' do
      foo = Foo.new
      foo.valid?.should be_false
      foo.name = 'joe'
      foo.valid?.should be_true
    end

    it 'fail to save an invalid record and save the errors' do
      foo = Foo.new
      foo.save.should be_false
      foo.persisted?.should be_false
      foo.errors.count.should == 1
      foo.errors.get(:name).first.should eq "can't be blank"
    end

    it 'fail to create an invalid record and save the errors' do
      foo = Foo.create
      foo.persisted?.should be_false
      foo.errors.count.should == 1
      foo.errors.get(:name).first.should eq "can't be blank"
    end

    it 'fail to save! an invalid record' do
      expect { Foo.new.save! }.to raise_error(ActiveRecord::RecordNotSaved)
    end

    it 'fail to create! an invalid record' do
      expect { Foo.create! }.to raise_error(ActiveRecord::RecordNotSaved)
    end

    it 'retrieve first' do
      foo2 = Foo.create! name: 'foo2'
      foo = Foo.first
      foo.uuid.should eq @foo.uuid
      foo2.destroy
    end

    it 'retrieve last' do
      foo2 = Foo.create! name: 'foo2'
      foo = Foo.last
      foo.uuid.should eq foo2.uuid
      foo2.destroy
    end

    it 'take multiple' do
      foo2 = Foo.create! name: 'foo2'
      foos = Foo.take(2)
      foos.size.should == 2
      foos.first.uuid.should_not be_nil
      foo2.destroy
    end

    it 'find multiple by id' do
      foo2 = Foo.create name: 'foo2'
      foos = Foo.find @foo.uuid, foo2.uuid
      foos.size.should == 2
      foos.first.uuid.should eq @foo.uuid
      foos.last.uuid.should eq foo2.uuid
      foo2.destroy
    end

    it 'find multiple by name' do
      foo2 = Foo.create! name: 'foo2'
      foos = Foo.find @foo.name, foo2.name
      foos.size.should == 2
      foos.first.uuid.should eq @foo.uuid
      foos.last.uuid.should eq foo2.uuid
      foo2.destroy
    end

    it 'check exists?(string)' do
      Foo.exists?(@foo.name).should be_true
    end

    it 'check not exists?(string)' do
      Foo.exists?('asdfasdf').should be_false
    end

    it 'check exists?(Array)' do
      Foo.exists?(['name = ?', @foo.name]).should be_true
    end

    it 'check not exists?(Array)' do
      Foo.exists?(['name = ?', 'asdfasdf']).should be_false
    end

    it 'check exists?(Hash)' do
      Foo.exists?(name: @foo.name).should be_true
    end

    it 'check not exists?(Hash)' do
      Foo.exists?(name: 'asfasdf').should be_false
    end

    it 'check exists?(nil)' do
      Foo.exists?.should be_true
    end

    it 'check not exists?(nil)' do
      Bar.exists?.should be_false
    end

    it 'perform first_or_create where found' do
      foo = Foo.where(name: @foo.name).first_or_create
      foo.uuid.should eq @foo.uuid
    end

    it 'perform first_or_create where not found' do
      foo = Foo.where(name: 'foo2').first_or_create(number: 42)
      foo.name.should eq 'foo2'
      foo.number.should == 42
      foo.destroy
    end

    it 'perform first_or_create! where found' do
      foo = Foo.where(name: @foo.name).first_or_create!
      foo.uuid.should eq @foo.uuid
    end

    it 'perform first_or_create! where not found' do
      expect { Foo.where(nuumber: 'foo2').first_or_create! }.to raise_error(ActiveRecord::RecordNotSaved)
    end

    it 'perform first_or_initialize where found' do
      foo = Foo.where(name: @foo.name).first_or_initialize(xxx: 42)
      foo.uuid.should eq @foo.uuid
      foo.xxx.should be_nil
    end

    it 'perform first_or_initialize where not found' do
      foo = Foo.where(name: 'foo2').first_or_initialize(number: 42)
      foo.name.should eq 'foo2'
      foo.number.should == 42
      foo.persisted?.should be_false
    end

    it "should destroy by id" do
      foo = Foo.create! name: 'foo'
      Foo.destroy(foo.id)
      foo = Foo.find_by_name 'foo'
      foo.should be_nil
    end

    it "should destroy by ids" do
      foo = Foo.create! name: 'foo'
      foo2 = Foo.create! name: 'foo2'
      Foo.destroy([foo.id, foo2.id])
      Foo.find_by_name('foo').should be_nil
      Foo.find_by_name('foo2').should be_nil
    end

    it "should destroy_all" do
      foo = Foo.create! name: 'foo', number: 42
      foo2 = Foo.create! name: 'foo2', number: 42
      Foo.destroy_all(number: 42)
      Foo.find_by_name('foo').should be_nil
      Foo.find_by_name('foo2').should be_nil
    end

    it "should delete by id" do
      foo = Foo.create! name: 'foo'
      Foo.delete(foo.id)
      foo = Foo.find_by_name 'foo'
      foo.should be_nil
    end

    it "should delete by ids" do
      foo = Foo.create! name: 'foo'
      foo2 = Foo.create! name: 'foo2'
      Foo.delete([foo.id, foo2.id])
      Foo.find_by_name('foo').should be_nil
      Foo.find_by_name('foo2').should be_nil
    end

    it "should delete_all" do
      foo = Foo.create! name: 'foo', number: 42
      foo2 = Foo.create! name: 'foo2', number: 42
      Foo.delete_all(number: 42)
      Foo.find_by_name('foo').should be_nil
      Foo.find_by_name('foo2').should be_nil
    end

    it "should update one" do
      foo = Foo.create! name: 'foo', number: 42
      Foo.update foo.uuid, { number: 43 }
      foo.reload.number.should == 43
      foo.destroy
    end

    it "should update multiple" do
      foo = Foo.create! name: 'foo', number: 42
      foo2 = Foo.create! name: 'foo2', number: 42
      updates = { foo.uuid => {number: 43}, foo2.uuid => {number: 44}}
      Foo.update(updates.keys, updates.values)
      foo.reload.number.should == 43
      foo2.reload.number.should == 44
      foo.destroy
      foo2.destroy
    end

    it "should update_all" do
      foo = Foo.create! name: 'foo', number: 43
      Foo.where(number: 43).update_all({number: 44})
      Foo.find_by_number(44).should_not be_nil
      foo.destroy
    end

    it "should fail on unaccessible mass assignment" do
      Foo.attr_accessible :name
      foo = Foo.create! name: 'foo', number: 43
      foo.number.should_not eq 43
      foo.update_attributes number: 44, foo: 'bar'
      foo.number.should_not eq 44
      foo.destroy
      Foo._accessible_attributes = nil
    end

    it "should fail on protected mass assignment" do
      Foo.attr_protected :number
      foo = Foo.create! name: 'foo', number: 43
      foo.number.should_not eq 43
      foo.update_attributes number: 44, foo: 'bar'
      foo.number.should_not eq 44
      foo.destroy
      Foo._protected_attributes = nil
    end

    it "should be able to page through results" do
      bars = (1..15).collect do |i|
        Bar.create! name: "name_#{i}", value: "value_#{i+1}"
      end
      query = Bar.all
      page = query.to_a # first page
      page.count.should eq 10
      page = query.next_page # second page
      count = 10
      page.each do |bar|
        bars[count].name.should eq bar.name
        count += 1
      end
      count.should eq bars.count
      bars.each {|bar| bar.delete}
    end

    it "should iterate past page boundaries" do
      bars = (1..15).collect do |i|
        Bar.create! name: "name_#{i}", value: "value_#{i+1}"
      end
      count = 0
      Bar.all.each do |bar|
        bars[count].name.should eq bar.name
        count += 1
      end
      count.should eq bars.count
      bars.each {|bar| bar.delete}
    end

    it "should honor limit" do
      bars = (1..15).collect do |i|
        Bar.create! name: "name_#{i}", value: "value_#{i+1}"
      end
      count = 0
      Bar.limit(13).each do |bar|
        bars[count].name.should eq bar.name
        count += 1
      end
      count.should eq 13
      bars.each {|bar| bar.delete}
    end

    it "perform as admin only when requested if require_login is true" do

      organization = @foo.management.organization SPEC_SETTINGS[:organization][:name]

      creds = nil
      User.as_admin do
        creds = organization.credentials
      end

      Usergrid::Ironhorse::Base.settings[:client_id] = creds.data.credentials.client_id
      Usergrid::Ironhorse::Base.settings[:client_secret] = creds.data.credentials.client_secret
      Usergrid::Ironhorse::Base.settings[:auth_token] = nil
      User.clear_thread_context
      organization.logout
      User.settings[:require_login] = true

      # should fail (login is required)
      expect {
        organization.create_application "_test_app_#{SecureRandom.hex}"
      }.to raise_error RestClient::Unauthorized

      # should succeed under admin context
      User.as_admin do
        organization.create_application "_test_app_#{SecureRandom.hex}"
      end

      Usergrid::Ironhorse::Base.settings[:client_id] = nil
      Usergrid::Ironhorse::Base.settings[:client_secret] = nil
    end

    it "perform as admin if require_login is false" do

      organization = @foo.management.organization SPEC_SETTINGS[:organization][:name]
      organization.logout

      creds = nil
      User.as_admin do
        creds = organization.credentials
      end

      Usergrid::Ironhorse::Base.settings[:client_id] = creds.data.credentials.client_id
      Usergrid::Ironhorse::Base.settings[:client_secret] = creds.data.credentials.client_secret
      Usergrid::Ironhorse::Base.settings[:auth_token] = nil
      User.clear_thread_context
      organization.logout

      User.settings[:require_login] = false

      # should succeed
      organization.create_application "_test_app_#{SecureRandom.hex}"

      Usergrid::Ironhorse::Base.settings[:client_id] = nil
      Usergrid::Ironhorse::Base.settings[:client_secret] = nil
    end
  end
end
