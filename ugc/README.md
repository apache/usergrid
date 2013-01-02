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
