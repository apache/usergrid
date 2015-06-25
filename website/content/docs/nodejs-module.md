---
title: Node.js module
category: docs
layout: docs
---

Installation
------------

From the command-line, run:

```bash
$ npm install usergrid
```


Client Initialization
---------------------

TODO



Organizations
-------------

```javascript
// Create an Organization
var options = {
    method:'POST',
    endpoint:'management/orgs',
    body:{ 
          password:'test12345', 
          email:'tester12345@gmail.com', 
          name:'test', 
          username:'tes123', 
          organization:'testorg' 
    }    
};
client.request(options, function (err, data) {
    if (err) {
        //error — POST failed
    } else {
        //success — data will contain raw results from API call        
    }
});


// Read an Organization
var options = {
    method:'GET',
    endpoint:'management/orgs/testorg'
};
client.request(options, function (err, data) {
    if (err) {
        //error — GET failed
    } else {
        //success — data will contain raw results from API call        
    }
});
```