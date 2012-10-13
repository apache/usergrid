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

### Getting started with the Usergrid_ironhorse SDK is super simple!*

Well, it will be simple, as soon as I get around to documenting how to do it!
In the mean time, hold off just a bit... I'm not quite ready for you yet.


## Contributing

We welcome your enhancements!

1. Fork it
2. Create your feature branch (`git checkout -b my-new-feature`)
3. Write some broken rspecs.
4. Fix the rspecs with your new code.
3. Commit your changes (`git commit -am 'Added some feature'`)
4. Push your changes to the upstream branch (`git push origin my-new-feature`)
5. Create new Pull Request

We've got 100% rspec coverage and we're looking to keep it that way!*
(*Not yet, but soon)
In order to run the tests, check out the Usergrid open source project
(https://github.com/apigee/usergrid-stack), build, and launch it locally.

(Note: If you change your local Usergrid settings from the default, be sure to update
usergrid_ironhorse/spec/spec_settings.yaml to match.)


## Release notes

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
