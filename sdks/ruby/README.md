# Usergrid_iron

Usergrid_iron enables simple, low-level Ruby access to Apigee's App Services (aka Usergrid) REST API with minimal
dependencies.


## Installation

Add this line to your application's Gemfile:

    gem 'usergrid_iron'

And then execute:

    $ bundle

Or install it yourself as:

    $ gem install usergrid_iron


## Usage

1. Not familiar with Usergrid / Apigee's App Services? It's great stuff! Check it out, here:

Docs: http://apigee.com/docs/usergrid/
Open source: https://github.com/apigee/usergrid-stack

2. Getting started with the Usergrid_iron SDK is simple! Let's run through some of the
basic concepts:

<pre>
  require 'usergrid_iron'

  # set the Usergrid (or Apigee App Services) URL
  usergrid_api = Usergrid::Resource.new 'http://localhost:8080'

  # get a management context & login the superuser
  management = usergrid_api.management
  management.login 'test', 'test'

  # get the organization context & create a new application
  app_name = 'dogs_application'
  organization = management.organization 'test-organization'
  application = organization.create_application app_name

  # create an application user
  application.create_user 'app_username', 'app_name', 'app_email@test.com', 'password'

  # get a new application context and login as the new user
  application = usergrid_api.application 'test-organization', app_name
  application.login 'app_username', 'password'

  # create a dog
  application.create_entity   'dogs', { breed: 'Black Mouth Cur', name: 'Old Yeller' }

  # create several more dogs
  application.create_entities 'dogs', [{ breed: 'Catalan sheepdog', name: 'Einstein' },
                                       { breed: 'Cocker Spaniel', name: 'Lady' },
                                       { breed: 'Mixed', name: 'Benji' }]

  # retrieves the dogs collection from Usergrid and prints their names
  dogs = application['dogs'].get.collection
  dogs.each do |dog|                          # works just like an array
    pp dog.name                               # entities automatically have attributes
  end

  # query for a dog named Benji
  dogs.query "select * where name = 'Benji'"  # repopulates the collection
  benji = dogs.first

  # modify Benji's attributes & save
  benji.command = 'come home'                     # use attribute access
  benji['breed'] = 'American Cocker Spaniel'  # or access it like a Hash
  benji.save

  # get the dog by uuid - the attributes were saved!
  dog = application["dogs/#{benji.uuid}"].get.entity
  if dog.command == 'come home'
    puts "Benji's coming home!"
  else
    raise 'Benji is a lost puppy!'
  end

</pre>

Looking for a specific feature? Check out the rspecs, there are examples of almost everything!


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


## Notes

The following features are not currently implemented on the server:

* delete organization
* delete application
* delete user


## Copyright

 * Copyright (c) 2012 Scott Ganyo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use the included files except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
