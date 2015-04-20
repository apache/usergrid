# Usergrid Command Line (ugc)

ugc enables convenient terminal access to Apache Usergrid.

## Features

* Multiple connection/login profiles
* Simple syntax to Usergrid data
* Will automatically perform paging on long query results
* Use relative or absolute URLs
* Convenient path reference '@n' to previous list entities
* Easy-to-read tabular output
* Simplified json-generating syntax for data (key: 'value')
* Ruby evaluation within data elements
* Can optionally emits raw output (--verbose switch)
* Extended SQL syntax (adds 'from' and 'limit' clauses to standard Usergrid syntax)
* File upload (-f or --file option) on PUT and POST
* Emit curl equivalent for all REST commands

## Installation

    $ gem install ugc
    
Note: Requires Ruby 1.9.3+ or Ruby 2.0+. If you have issues, check your version:

	$ ruby -v
	
If necessary, install a new version of Ruby. [RVM](https://rvm.io) is recommended:

	$ curl -L https://get.rvm.io | bash -s stable --ruby

## Usage

### Commands

    delete              - delete an entity
    get, show, ls, list - retrieve and display a collection or entity
    help                - Shows a list of commands or help for one command
    login               - Performs a login to the current profile
    post, create        - non-idempotent create or update (post is usually create)
    profile, profiles   - set the current profile (creates if it doesn't exist)
    put, update         - idempotent create or update (put is usually an update)
    query               - query (uses sql-like syntax)
    target              - set the base url, org, and app for the current profile

### Setup

#### Create a profile:

	$ ugc profile apigee

#### Set targets:

    $ ugc target base https://api.usergrid.com
	base_url = https://api.usergrid.com

	$ ugc target organization scottganyo
	organization = scottganyo

	$ ugc target app messagee
	application = messagee

#### (Alternatively, you can just specify the whole url at once)

	$ ugc target url https://api.usergrid.com/scottganyo/messagee
    base_url = https://api.usergrid.com
    organization = scottganyo
    application = messagee

#### Login:

(Note the user in this case is an admin user)

	$ ugc login --admin scott@ganyo.com
	password: **********
	logged in user: scott@ganyo.com

#### Verify your profile:

    $ ugc profile apigee
    Set active profile:
     *apigee
        base_url: https://api.usergrid.com
        organization: scottganyo
        application: messagee
        access_token: YWMtKgZnO1ULEeKKqQLoGuZA3AAAATwesjMsUYmz_ZGk8vkTwp0lh66Cv_CCEM4


### Examples

#### list all Usergrid entity collections

    $ ugc list collections
    title      count      name       type
    Assets     0          assets     asset
    Users      0          users      user
    Events     0          events     event
    Roles      3          roles      role
    Folders    0          folders    folder
    Activities 0          activities activity
    Devices    0          devices    device
    Groups     0          groups     group

What? No dogs? A travesty!

#### create a dog

(Note the use of simplified json syntax)

    $ ugc create dog "breed: 'Black Mouth Cur', name: 'Old Yeller'"
    name     value
    uuid     91833fd9-56c5-11e2-a6b8-14109fd49581
    name     Old Yeller
    created  1357341732438
    modified 1357341732438
    breed    Black Mouth Cur

#### he's lonely. we need more dogs.

(Note use of square brackets to create an array of dogs)

    $ ugc create dogs "[{ name: 'Tramp' },{ breed: 'Cocker Spaniel', name: 'Lady' }]"
    #  uuid                                 name  created       modified      breed
    1  79dfb563-56cb-11e2-a6b8-14109fd49581 Tramp 1357344269759 1357344269759
    2  79e11500-56cb-11e2-a6b8-14109fd49581 Lady  1357344269768 1357344269768 Cocker Spaniel

All the created dogs were returned as a list.

#### oops, show that first dog

(Note use of @1 to reference the 1st row from the previous list.)

    $ ugc show @1
    name     value
    uuid     79dfb563-56cb-11e2-a6b8-14109fd49581
    name     Tramp
    created  1357344269759
    modified 1357344269768

Note synonym commands:

    $ ugc show dogs/79dfb563-56cb-11e2-a6b8-14109fd49581
    $ ugc show dogs/Tramp


#### yup. forgot to set the breed, update that dog

(Note use of standard json data)

    $ ugc update @1 '{ "breed" : "Mixed" }'
    name     value
    uuid     79dfb563-56cb-11e2-a6b8-14109fd49581
    name     Tramp
    created  1357344269759
    modified 1357344537483
    breed    Mixed
    
If you have more than ten dogs, it might be easier to target your specific dog when you update it:

```ugc update dogs/79dfb563-56cb-11e2-a6b8-14109fd49581 '{ "breed" : "Mixed" }'```

#### show off our latest dogs

    $ ugc query dogs 'select name, breed where modified >= 1357344269768'
    #  name     breed
    1  Einstein Mixed
    2  Lady     Cocker Spaniel

Note: with ugc, you can also use extended sql syntax...

    $ ugc query 'select name, breed from dogs where modified >= 1357344269768 limit 1'
    #  name     breed
    1  Einstein Mixed

#### -- Special note on specifying column names in queries --

If you specify column names in your query, you will be unable to reference the returned rows by @1 reference in later commands. (The current Usergrid implementation doesn't return any metadata for the entries.) In addition, for your safety the history will be cleared so that you don't inadvertently reference entities from a previous list.

## Release notes

### 0.9.9
* Bug Fixes
  1. fix setting profile & target configurations
* Misc
  1. Add license to gem

### 0.9.8
* New features
  1. logout (remove access_token)
  2. no longer force log in - as it's not always necessary
  3. get (ls) command now defaults to list collections when no arguments
* Bug fixes
  1. updated to work with latest gli (> 2.6)

### 0.9.7
* Bug Fixes
  1. allow headers to vary during paging

### 0.9.6
* Bug Fixes
  1. url encode url for curl

### 0.9.5
* Bug Fixes
  1. Allow for complex matrixed queries including spaces

### 0.9.4
* New features
  1. Emit curl equivalent commands with -c (--curl) switch
    * eg. `$ ugc -c get something`

### 0.9.3
* New features
  1. file upload function with -f (--file) switch
    * eg. `$ ugc post something "property1Name: 'property1Value'" --file property2Name=/path/to/myfile`

### 0.9.2
* New features
  1. access management functions with -m (--management) global switch
    * eg. `$ ugc -m get orgs/my-org`

### 0.9.1
* New features
  1. allow non-interactive password on login (-p or --password)
* Bug fixes
  1. fix display issue with assign query (select {user:username} from...)

### 0.9.0
* New features
  1. path reference '@n' in commands to previous list entities
     * eg. `$ ugc put @1 foo: 'bar'`
  2. added --no-border flag
  3. added rm alias for delete
* Bug fixes
  1. lock required version of usergrid_iron

### 0.0.6
* New features
  1. Ruby eval of data in put and post commands
     * eg. may now use `key: 'value'` for json instead of `{"key": "value"}`
  2. Added alias: 'show' for 'get'
  3. Made 'list' an alias of 'get' and updated get to include 'list' functionality
  4. smart column width for entities
  5. Added aliases 'create' and 'update' for 'post' and 'put' (yes, I am aware this isn't technically correct)

### 0.0.5
* New features
  1. smart column widths for collections
  2. add "limit" keyword to sql syntax
* Bug fixes
  1. fixed issue with selecting a single column
  2. fixed formatting of entities with heterogeneous attributes

### 0.0.4
* Bug fixes
  1. include Gemfile.lock to ensure correct version of usergrid_iron is used

### 0.0.3
* New features
  1. support for query result paging


## Copyright
Licensed to the Apache Software Foundation (ASF) under one or more contributor
license agreements.  See the NOTICE.txt file distributed with this work for
additional information regarding copyright ownership.  The ASF licenses this
file to you under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License.  You may obtain a copy of
the License at

     http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
License for the specific language governing permissions and limitations under
the License.

