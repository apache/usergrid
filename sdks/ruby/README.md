# Usergrid_iron

Usergrid_iron enables simple, low-level Ruby access to Apigee's App Services (aka Usergrid)
REST API with minimal dependencies.

## Installation

Add this line to your application's Gemfile:

    gem 'usergrid_iron'

And then execute:

    $ bundle

Or install it yourself as:

    $ gem install usergrid_iron


## Usage

### Not familiar with Usergrid / Apigee's App Services?

#### It's great stuff! Check it out, here:

  Docs: <http://apigee.com/docs/usergrid/>  
  Open source: <https://github.com/apigee/usergrid-stack>

### Getting started with the Usergrid_iron SDK is simple! 

#### Let's start with the basics.
For this example, we'll assume you've already set up an organization, application, and user -
just fill in your own values in the code below.

```
require 'usergrid_iron'

# fill in your values here!
usergrid_api = 'http://localhost:8080'
organization = ''
application = ''
username = ''
password = ''

application = Usergrid::Application.new "#{usergrid_api}/#{organization}/#{application}"
application.login username, password

# create and store a dog in the 'dogs' collection on the server
response = application.create_dog breed: 'Black Mouth Cur', name: 'Old Yeller'

# let's get the dog from the response and grab its persistent id
dog = response.entity
uuid = dog.uuid

# let's retrieve a dog from the server by UUID
same_dog = application['dogs'][uuid].entity

# is it our dog? well, he knows his name!
puts "My dog's name is: #{same_dog.name}"
```

Well that was really easy. 

#### Let's try something slightly more complex.
Let's say you've registered for an organization, but you don't have an application yet
(or want to create a new one to work on). No worries, just fill in your organization and
superuser credentials below, and follow along!
(Better yet: If you used the Usergrid launcher and let it initialize your database,
you shouldn't need to do anything!)

```
  require 'usergrid_iron'

  usergrid_api = 'http://localhost:8080'
  org_name = 'test-organization'
  username = 'test'
  password = 'test'
  app_name = 'dog_sitter'

  ## first, let's get that setup out of the way ##

  # get a management context & login the superuser
  management = Usergrid::Management.new usergrid_api
  management.login username, password

  # get the organization context & create a new application
  organization = management.organization org_name
  new_application = organization.create_application app_name

  # create an user for our application
  new_application.create_user username: 'username', password: 'password'


  ## now we can play with the puppies! ##

  # login to our new application as our new user
  application = Usergrid::Application.new "#{usergrid_api}/#{org_name}/#{app_name}"
  application.login 'username', 'password'

  # we can start with our dog again
  application.create_dog breed: 'Black Mouth Cur', name: 'Old Yeller'

  # but this time let's create several more dogs at once
  application.create_dogs [
      { breed: 'Catalan sheepdog', name: 'Einstein' },
      { breed: 'Cocker Spaniel', name: 'Lady' },
      { breed: 'Mixed', name: 'Benji' }]

  # retrieve all the dogs (well, the first 'page' anyway) and tell them hi!
  dogs = application['dogs'].collection
  dogs.each do |dog|                          # works just like an array
    puts "Hello, #{dog.name}!"                # entities automatically have attributes
  end

  # "Benji, come!"
  benji = dogs.query("select * where name = 'Benji'").entity  # shortcut: entity() returns the first

  # modify Benji's attributes & save
  benji.location = 'home'                     # use attribute access
  benji['breed'] = 'American Cocker Spaniel'  # or access it like a Hash
  benji.save

  # query for the dogs that are home (should just be Benji)
  dogs = application['dogs'].query("select * where location = 'home'").collection
  if dogs.size == 1 && dogs.first.location == 'home'
    puts "Benji's home!"
  end

```

Whew. That's enough for now. But looking for a specific feature? Check out the rspecs,
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


## Copyright
Copyright (c) 2012 Scott Ganyo 

Licensed under the Apache License, Version 2.0 (the "License");
you may not use the included files except in compliance with the License.

You may obtain a copy of the License at

  <http://www.apache.org/licenses/LICENSE-2.0>
  
Unless required by applicable law or agreed to in writing, software distributed under
the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
either express or implied. See the License for the specific language governing permissions and
limitations under the License.
