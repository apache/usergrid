## Deploying or Developing

If you are just deploying...

1. install nodejs - http://nodejs.org/download/
2. install grunt - "sudo npm install grunt-cli -g"
3. from the root dir, run ./build.sh
4. this will create a directory in the root called dist, in dist is a zip file appsvc-ui.zip, unzip and deploy to your favorite web server

If you are developing...

1. from the root dir, run ./build.sh;
2. to monitor and build the performance code => run grunt --gruntfile Perf-Gruntfile.js dev; this will need to continue running in terminal as you are developing
3. to monitor and build the portal code base => run grunt dev; this will open a browser with http://localhost:3000/index-debug.html
4. to debug in the browser go to http://localhost:3000/index-debug.html; http://localhost:3000/ will point to the compressed files
5. if the libraries get out of sync run ./build.sh again and this will run grunt build in the background.

If you want to run the e2e tests

1. from the root directory ./build.sh e2e


To version open a terminal and run 'npm version x.x.x' this will add a tag and increment the package.json.
