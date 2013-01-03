# Usergrid Command Line (ugc)

ugc enables convenient terminal access to Apigee's App Services (aka Usergrid).

## Features

* Multiple connection/login profiles
* Simple syntax
* Use relative (or absolute) URLs
* Easy-to-read tabular output
* Optionally also emits raw output (--verbose switch)

## Installation

    $ gem install ugc
    
Note: Requires Ruby 1.9.x. If you have issues, check your version:

	$ ruby -v
	
If necessary, install a new version of Ruby. [RVM](Ruby 1.9.x
) is recommended:

	$ \curl -L https://get.rvm.io | bash -s stable --ruby

## Usage

### Help

    $ ugc help

### Setup

Connect to an Apigee administrator account:

	$ ugc profile apigee
	$ ugc target organization scottganyo
	organization = scottganyo
	$ ugc target app messagee
	application = messagee
	$ ugc login --admin scott@ganyo.com
	password: **********
	logged in user: scott@ganyo.com
	

### Examples

![image](https://github.com/scottganyo/ugc/raw/master/examples.jpeg)

## Release notes

### 0.0.4
* Bug fixes
  1. include Gemfile.lock to ensure correct version of usergrid_iron is used

### 0.0.3
* New features
  1. support for query result paging


## Copyright
Copyright (c) 2013 Scott Ganyo

Licensed under the Apache License, Version 2.0 (the "License");
you may not use the included files except in compliance with the License.

You may obtain a copy of the License at

  <http://www.apache.org/licenses/LICENSE-2.0>

Unless required by applicable law or agreed to in writing, software distributed under
the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
either express or implied. See the License for the specific language governing permissions and
limitations under the License.
