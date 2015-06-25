---
title: REST API
category: docs
layout: docs
---

Organizations
-------------

```bash
# Create an Organization
curl -X POST "https://api.usergrid.com/management/organizations" \
     -d '{ "password":"test12345", "email":"tester123@hotmail.com", "name":"test", "username":"test123", "organization":"testorg" }'

# Read an Organization
curl -X GET "https://api.usergrid.com/management/organizations/testorg"
```