##Change log

###0.10.4

- Added new functions for creating, getting, and deleting connections
- Added test cases for said functions
- Added logout call to client create to clear out any remnant token from a past session
- Fixed change password error
- Added getEntity method to get existing entity from server

###0.10.3

- Added set / get token methods to accomodate session storage
- Added createUserActivity method to make creating activities for logged in user easier

###0.10.2

- Removed local caching of user object in client

###0.10.1

- Complete refactor of the SDK to bring congruity with the App services Node module

- Client object is now main entry point - all objects are created from the client, and all calls are run from the client

- Removed Curl extension - now just use boolean in options object when creating client

- Added full test coverage for all sample code in the readme file

- Renamed SDK file to usergrid.js


###0.9.10

- Refactored directory structure.  SDK file now at root, extensions in their own directory, examples in their own directory.

- Moved cURL command generator into a separate file (extensions/usergrid.curl.js).  Include this file after the the SDK if you want cURL command generation.

- Moved Validation functionality into a separate file (extensions/usergrid.validation.js). Include this file after the the SDK if you want to use this functionality.

- Moved Session file into a separate file (extensions/usergrid.session.js). Include this file after the the SDK if you want to use this functionality.

- Removed deprecated get function from Collection object.

- Added timeout callback.

- Added beginnings of a qUnit test suite - only a few functions tested so far, but we will add to the suite as we progress.

- Removed minified files.  We hope to host these files on a CDN soon.
