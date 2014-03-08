## Running the tests
Before running the tests you will need to run the following steps.

1. install nodejs - http://nodejs.org/download/

2. install grunt - "sudo npm install grunt-cli -g"

3. install karma - "sudo npm install karma -g"

4. install protractor - "sudo npm install protractor -g"

5. in terminal navigate to the root directory of git repo for the Usergrid Admin Portal

6. run npm install in your terminal - "npm install"

7. run grunt in your terminal - "grunt", this will also run the tests, if you want to run the tests independently follow the next steps

8. run karma in your terminal - "karma start tests/karma.conf.js"

9. this will open a browser window where you can debug

10. if you want to run the e2e tests open a terminal, navigate to the root and run "protractor ./tests/protractorConf.js"

for more info see http://karma-runner.github.io/0.10/index.html or https://github.com/angular/protractor
