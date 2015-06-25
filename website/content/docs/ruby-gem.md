---
title: Ruby gem
category: docs
layout: docs
---

Installation
------------

From the command-line run:

```bash
$ gem install usergrid_iron
```

Organizations
-------------

````ruby
# Initialize Management object
mgmt = Usergrid::Management.new 'https://api.usergrid.com/'

# Create a new Organization
mgmt.create_organization 'testorg', 'test123', 'test', 'tester123@hotmail.com', 'test12345'

# Load an exiting Organization
org = mgmt.organization 'testorg'
````