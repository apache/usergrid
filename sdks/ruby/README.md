# Ruby SDK

Usergrid_iron enables simple, low-level Ruby access to Apigee's App Services (aka Usergrid)
REST API with minimal dependencies.

## Installation

### Project
Add the gem to your project's Gemfile:

    gem 'usergrid_iron'

Then rebuild your bundle:

    $ bundle

### Stand-alone script
Just manually install the gem:

    $ gem install usergrid_iron


## Usage

### Prerequisite
You'll want to be at least a little bit familiar with Usergrid / Apigee's App Services before you jump in here - but it easy and it's great! Start here:

  App Services docs: <http://apigee.com/docs/usergrid/>
  Open source stack: <https://github.com/apigee/usergrid-stack>

Awesome. Let's go!

### Getting started with the Usergrid_iron SDK is simple!

#### Let's start with the basics.
For this example, we'll assume you've already set up an organization, application, and user -
just fill in your own values in the code below.

```ruby
require 'usergrid_iron'

# fill in your values here!
usergrid_api = 'http://localhost:8080'
organization = ''
application = ''
username = ''
password = ''

# create the base application resource
# (this is a RestClient.resource)
application = Usergrid::Application.new "#{usergrid_api}/#{organization}/#{application}"

# login (note: not required for sandbox)
application.login username, password

# create and store a new dog on the server
# (the "response" is an enhanced RestClient.response)
response = application.create_dog breed: 'Black Mouth Cur', name: 'Old Yeller'

# the response has returned the entity data
# response.entity wraps it in an easy-to-use object
dog = response.entity

# it's persistent now, so it has a unique id
uuid = dog.uuid

# we can retrieve the dog by UUID using Hash syntax and calling get()
# (all dogs are stored in the "dogs" collection)
response = application["dogs"][uuid].get
same_dog = response.entity

# we could also retrieve the dog by name
# we could also use path ('/') syntax instead of nested Hash
# and we can even skip get() - entity() will do it for us
same_dog = application["dogs/#{dog.name}"].entity

# is it our dog? well, he knows his name!
# (and we can retrieve its values by dot or Hash)
puts "My dog's name is: #{same_dog.name} and his breed is #{same_dog['breed']}"
```

Well that was really easy. More comments than code! :)

#### Let's try something slightly more complex.
Let's say you've registered for an organization, but you don't have an application yet
(or want to create a new one to work on). No worries, just fill in your organization and
superuser credentials below, and follow along!
(Better yet: If you used the Usergrid launcher and let it initialize your database,
you shouldn't need to do anything!)

```ruby
  require 'usergrid_iron'

  usergrid_api = 'http://localhost:8080'
  org_name = 'test-organization'
  username = 'test'
  password = 'test'
  app_name = 'dog_sitter'

  ## first, let's get that setup out of the way... ##

  # get a management context & login the superuser
  management = Usergrid::Management.new usergrid_api
  management.login username, password

  # get the organization context & create a new application
  organization = management.organization org_name
  new_application = organization.create_application app_name

  # create an user for our application
  new_application.create_user username: 'username', password: 'password'

  # login to our new application as our new user
  application = organization.application app_name
  application.login 'username', 'password'


  ## now we can play with the puppies! ##

  # we can start with our dog again
  application.create_dog breed: 'Black Mouth Cur', name: 'Old Yeller'

  # but this time let's create several more dogs all at once
  application.create_dogs [
      { breed: 'Catalan sheepdog', name: 'Einstein' },
      { breed: 'Cocker Spaniel', name: 'Lady' },
      { breed: 'Mixed', name: 'Benji' }]

  # retrieve all the dogs (well, the first 'page' anyway) and tell them hi!
  # note: we're calling collection() instead of entity() because we have several
  dogs = application['dogs'].collection

  # you can iterate a collection just like an array
  dogs.each do |dog|
    puts "Hello, #{dog.name}!"
  end

  # Let's get Benji ("Benji, come!"), but this time we'll retrieve by query
  response = dogs.query "select * where name = 'Benji'"

  # we could call "response.collection.first"
  # but there's a shortcut: entity() will also return the first
  benji = response.entity

  # modify Benji's attributes & save to the server
  benji.location = 'home'                     # use attribute access
  benji['breed'] = 'American Cocker Spaniel'  # or access attributes like a Hash
  benji.save

  # now query for the dogs that are home (should just be Benji)
  dogs = application['dogs'].query("select * where location = 'home'").collection
  if dogs.size == 1 && dogs.first.location == 'home'
    puts "Benji's home!"
  end

```

Whew. That's enough for now. But looking for a specific feature? Check out the [rspecs](http://github.com/scottganyo/usergrid_iron/tree/master/spec/usergrid/core),
there are examples of nearly everything!


## Contributing

We welcome your enhancements!

1. Fork it
2. Create your feature branch (`git checkout -b my-new-feature`)
3. Write some broken rspecs.
4. Fix the rspecs with your new code.
3. Commit your changes (`git commit -am 'Added some feature'`)
4. Push your changes to the upstream branch (`git push origin my-new-feature`)
5. Create new Pull Request

We've got 100% rspec coverage and we're looking to keep it that way!
In order to run the tests, check out the Usergrid open source project
(https://github.com/apigee/usergrid-stack), build, and launch it locally.

(Note: If you change your local Usergrid setting from the default, be sure to update
usergrid_iron/spec/spec_settings.yaml to match.)


## Release notes

### 0.9.2
* New features
  1. delete_query (batch delete by query)

### 0.9.1
* New features
  1. may now login using credentials: application.login_credentials() or organization.login_credentials()

### 0.9.0
* Backend changes
  1. login function now uses POST instead of GET

### 0.0.9
* Backend changes
  1. made Resource::response accessor public to support ugc

### 0.0.8
* Bug fixes
  1. better handling of paging

### 0.0.7
* Bug fixes
  1. multiple_entities? should check data['list']

### 0.0.6
* New features
  1. iterators can now optionally cross page boundaries, eg. `collection.follow_cursor.each`
  2. added facebook_login(fb_access_token) method to application

### 0.0.5
* New features
  1. added create_* method for application
* Backend changes
  1. eliminated serialization of reserved attributes
* Incompatible changes
  1. deprecated `Application::create_user username, password, ...`, use `create_user username: 'user', password: 'password'`

### 0.0.4
* New features
  1. empty? check for collection
  2. update queries (batch update)
* Backend changes
  1. Additional data sanity checks
  2. Additional flexibility in concat_urls
  3. Cleanup

### 0.0.3
* New features
  1. Support for lists returned when making parameterized queries:
	 <pre>select username, email where…</pre>
	 or replacement queries:
	 <pre>select { user:username, email:email } where…
* Incompatible changes
  1. Application.create_user parameter change from:
     <pre>create_user(username, name, email, password, invite=false)</pre>
     to:
     <pre>create_user(username, password, email=nil, name=nil, invite=false)</pre>
* Backend changes
  1. Replaced json_pure dependency with multi_json


## Notes

The following features are not currently implemented on the server:

* delete organization
* delete application
* delete user


## License
Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements.  See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
the ASF licenses this file to You under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License.  You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
