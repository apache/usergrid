#Usergrid RESTful Integration Tests

These tests will run against a deployed instance of Usergrid and validate that APIs respond as expected. Tests require [Node.js](https://nodejs.org), [Mocha](http://mochajs.org), and [Should.js](http://shouldjs.github.io). 

Get Started:

1. Install [Node.js](https://nodejs.org/download) version 0.12.1 or newer
2. Install Mocha: `$ [sudo] npm -g install mocha`
3. `$ cd` to `/integration_tests` and run `$ npm install`.
4. Using `config/default.js` as a template, create a copy `config/override.js` and modify it according to your environment.
5. Once configured, run `$ mocha test` from `/integration_tests` to perform tests.

Notes:

- Connections do not currently support org/app credentials. For tests to pass, you will need to give `Guest` POST rights to `/**` in the Usergrid authorizations table.
- In order for notifications tests to pass, you will need to create an Apple notifier named `apple-dev` using a valid development APNS certificate.
- In order to skip tests, you can append `.skip` to the test method, e.g.: `describe.skip()` or `it.skip()`.
- Depending on your environment, certain tests may take longer than expected. You can override timeouts by setting `this.timeout(timeInMilliseconds)` and `this.slow(timeInMilliseconds)` inside the `describe()` method before the tests execute.
- For more information on adding or modifying tests, check out the [Mocha](http://mochajs.org), and [Should.js](http://shouldjs.github.io) documentation.