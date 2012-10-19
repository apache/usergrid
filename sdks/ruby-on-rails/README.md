# Usergrid_ironhorse

Usergrid_ironhorse is based on Usergrid_iron and enables Ruby or Rails applications
native Rails-style access to Apigee's App Services (aka Usergrid) REST API.

## Installation

Add this line to your application's Gemfile:

    gem 'usergrid_ironhorse'

And then execute:

    $ bundle

Or install it yourself as:

    $ gem install usergrid_ironhorse


## Usage

### Not familiar with Usergrid / Apigee's App Services?

#### It's great stuff! Check it out, here:

  Docs: <http://apigee.com/docs/usergrid/>  
  Open source: <https://github.com/apigee/usergrid-stack>

### Getting started with the Usergrid_ironhorse SDK is super simple!

#### Setup

* Add 'gem usergrid_ironhorse' to your Gemfile
* Create a 'config/usergrid.yml' file that looks something like this (the
auth_token is your application admin token):
<pre>
development:
  application_url: http://localhost:8080/my-organization/my-application
  auth_token: YWMtc4WjqhcbEeK6UhQQn9SVgQAAATpryjMnLy9oFaPbP-0qIxoUx_4vtaOmpmE

development:
  application_url: http://localhost:8080/my-organization/my-application
  auth_token: YWMtc4WjqhcbEeK6UhQQn9SVgQAAATpryjMnLy9oFaPbP-0qIxoUx_4vtaOmpmE

production:
  application_url: http://api.usergrid.com/my-organization/my-application
  auth_token: YWMtc4WjqhcbEeK6UhQQn9SVgQAAATpryjMnLy9oFaPbP-0qIxoUx_4vtaOmpmE
</pre>
* Your User model should subclass Usergrid::Ironhorse::Base and extend
Usergrid::Ironhorse::UserContext like so:
<pre>
class User < Usergrid::Ironhorse::Base
  extend Usergrid::Ironhorse::UserContext

end
</pre>
* Set up your authentication
	* Use `User.authenticate(username, password, session)` to login.
	* Use `User.clear_authentication(session)` to log out.
* Propogate the authentication in your ApplicationController:
<pre>
before_filter :set_thread_context
def set_thread_context
  User.set_thread_context session
end
</pre>
* Optionally, if you need to access the User from your view, you may add something
like the following to your ApplicationController:
<pre>
helper_method :current_user
def current_user
  User.current_user
end
</pre>

#### Get going!

* Subclass Usergrid::Ironhorse::Base for your models.
Your models will automatically be stored in a collection according to the name of your
class as defined by Rails' ActiveModel::Naming module. (Which you may override by
implementing model_name if desired.)

<pre>
class Developer < Usergrid::Ironhorse::Base
  validates :name, :presence => true  # Yes, of course you can use validation

end
</pre>
* Now just use the Rails methods you're already familiar with:
<pre>

    dev = Developer.new language: 'Ruby'
    dev.valid? # nope!
    dev.errors # {:name=>["can't be blank"]}
    dev.name = 'Scott'
    dev.save!

    dev = Developer.find_or_create_by_name 'Scott'
    dev.favorite_color = 'green' # assign new attributes automatically

    dev = Developer.find_by_name 'Scott'
</pre>
* BTW: If you need to do management tasks, wrapping the work in an as_admin block
will use the auth_token from your settings:

<pre>
User.as_admin do
  # do protected task
end
</pre>

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
(https://github.com/apigee/usergrid-stack), build, and launch it locally.

(Note: If you change your local Usergrid settings from the default, be sure to update
usergrid_ironhorse/spec/spec_settings.yaml to match.)


## Release notes

### 0.0.2
* New Features
  1. Authentication and user propagation features

### 0.0.1
* Initial commit
  1. Support for most ActiveModel stuff including Validations
  1. No scoping support


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
