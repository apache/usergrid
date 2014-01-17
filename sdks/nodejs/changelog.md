##Change log


###0.10.7
- Fixed issue where token was appeneded even when no token was present
- Updated tests


###0.10.5

- Added new class and methods for Groups
- Added serialization / restore methods for entities and collections
- Various bug fixes
- Added function for getting user feed
- Added function for creating user activities with an associated user entity
- Added public facing helper method for signing up users

###0.10.4

- Added new functions for creating, getting, and deleting connections
- Added test cases for said functions
- Fixed change password error
- Added getEntity method to get existing entity from server

###0.10.3

- Added set / get token methods to accomodate session storage
- Added createUserActivity method to make creating activities for logged in user easier

###0.10.2

- Removed local caching of user object in client

###0.10.1

- Minor refactor of the SDK to bring congruity with the App services Javascript SDK

###0.10.0
- Complete refactor of the entire module

- Added Mocha based test suite

- Added full coverage of all sample code in the readme file
