# Apache Usergrid_ironhorse

Usergrid_ironhorse is based on Usergrid_iron and enables Ruby or Rails applications
native Rails-style access to Apigee's App Services (aka Usergrid) REST API.

## Compatibility

**Usergrid_ironhorse is currently only compatible with Ruby on Rails 3.2.**

Thus, if you are using Rails 4.x, you must use Usergrid_iron (the low-level API for Ruby).
Alternatively, we would welcome your contributions to make Usergrid_ironhorse compatible with Rails 4.x. Thanks!

## Installation

Add this line to your application's Gemfile:

    gem 'usergrid_ironhorse'

And then execute:

    $ bundle

Or install it yourself as:

    $ gem install usergrid_ironhorse


## Usage

### Not familiar with Usergrid's App Services?

#### It's great stuff! Check it out, here:

  Docs: <https://usergrid.apache.org/docs/>  
  Open source: <https://github.com/usergrid/usergrid/>

### Getting started with the Usergrid_ironhorse SDK is super simple!

#### Setup

* Add 'gem usergrid_ironhorse' to your Gemfile
* Create a 'config/usergrid.yml' file that looks something like this (the
auth_token is your application token):

```
development:
  application_url: http://localhost:8080/my-organization/my-application
  client_id: YXA6BVYasLdNEeKBd1A2yYstg
  client_secret: YXA60Dnbzaxg1ObkE8ffsIxsGzsSo8
  require_login: false

test:
  application_url: http://localhost:8080/my-organization/my-application
  client_id: YXA6BVYasLdNEeKBd1A2yYstg
  client_secret: YXA60Dnbzaxg1ObkE8ffsIxsGzsSo8
  require_login: false

production:
  application_url: http://api.usergrid.com/my-organization/my-application
  client_id: YXA6BVYasLdNEeKBd1A2yYstg
  client_secret: YXA60Dnbzaxg1ObkE8ffsIxsGzsSo8
  require_login: false
```

#### Get going!

* Subclass Usergrid::Ironhorse::Base for your models.
Your models will automatically be stored in a collection according to the name of your
class as defined by Rails' ActiveModel::Naming module. (Which you may override by
implementing model_name if desired.)

```
class Developer < Usergrid::Ironhorse::Base
  validates :name, :presence => true  # Yes, of course you can use validation

end
```

* Now just use the Rails methods you're already familiar with:

```

    dev = Developer.new language: 'Ruby'
    dev.valid? # nope!
    dev.errors # {:name=>["can't be blank"]}
    dev.name = 'Scott'
    dev.save!

    dev = Developer.find_or_create_by_name 'Scott'
    dev.favorite_color = 'green' # assign new attributes automatically

    dev = Developer.find_by_name 'Scott'
```

* BTW: If you need to do management tasks, wrapping the work in an as_admin block
will use the auth_token from your settings:

```
User.as_admin do
  # do protected task
end
```


#### (Optional) Need to have user-specific logins to UserGrid?

* Create a User model and subclass `Usergrid::Ironhorse::Base` and `extend
Usergrid::Ironhorse::UserContext` like so:

```
class User < Usergrid::Ironhorse::Base
  extend Usergrid::Ironhorse::UserContext
  ...
end
```

* Set up your authentication
	* Use `User.authenticate(username, password, session)` to login.
	* Use `User.clear_authentication(session)` to log out.
* Propogate the authentication in your ApplicationController:

```
before_filter :set_user_context
def set_user_context
  User.set_context session
end
```

* Optionally, if you need to access the User from your view, you may add something
like the following to your ApplicationController:

```
helper_method :current_user
def current_user
  User.current_user
end
```

## Contributing

We welcome your enhancements!

1. Fork it
2. Create your feature branch (`git checkout -b my-new-feature`)
3. Write some broken rspecs.
4. Fix the rspecs with your new code.
3. Commit your changes (`git commit -am 'Added some feature'`)
4. Push your changes to the upstream branch (`git push origin my-new-feature`)
5. Create new Pull Request

We're shooting for 100% rspec coverage, so keep that in mind!

In order to run the tests, check out the Usergrid open source project
(https://github.com/usergrid/usergrid/), build, and launch it locally.

(Note: If you change your local Usergrid settings from the default, be sure to update
usergrid_ironhorse/spec/spec_settings.yaml to match.)


## Release notes

### 0.1.1
* New Features
  1. Now prefer application client_id and client_secret (instead of auth_token) in usergrid.yml.

### 0.1.0
* New Features
  1. next_page() added to return the next page of results from the server. An example of this used in conjunction
     with to_a() is in base_spec.rb (see "should be able to page through results").
* Incompatible changes
  1. each() iteration will now transparently cross page boundaries (as generally expected by Rails users).
     You may use limit(n) to restrict the result set, but note that limit will retrieve the number of entities
     specified as a single batch (no paging).

### 0.0.5
* New Features
  1. support MassAssignmentSecurity (attr_accessible & attr_protected)

### 0.0.4
* New Features
  1. add require_login to config (with ability to skip individual logins)

### 0.0.3
* Internal
  1. Improve authentication and user propagation

### 0.0.2
* New Features
  1. Authentication and user propagation features

### 0.0.1
* Initial commit
  1. Support for most ActiveModel stuff including Validations
  1. No scoping support


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

