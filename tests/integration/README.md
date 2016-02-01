#REST Test framework

Tests will run via nodejs and mocha framework. The tests will make network calls to the specified environment.

1. install node above 0.12.1
2. run "npm install"
3. to override settings in config/default.js add a file config/override.js with the same structure, two documents will be merged.
4. run "mocha"