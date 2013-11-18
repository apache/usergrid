
Usergrid REST API Web App

Installs as a webapp in Tomcat. Has not been extensively tested in other web containers.
See usergrid-standalone for an example of running inside Grizzly.

To verify installation, go here:

http://localhost:8080/test/hello

Eclipse insists on deploying with the ROOT servlet prefix:

http://localhost:8080/ROOT/test/hello

Before you can use, you need to make sure that the database is setup. You can
do that at the following URLs:

http://localhost:8080/system/database/setup
http://localhost:8080/ROOT/system/database/setup

You'll need to enter the superuser credentials (superuser/superuser), assuming
they haven't been changed from the defaults in the runtime properties file:

config/src/main/resources/properties.txt

usergrid.sysadmin.login.name=superuser
usergrid.sysadmin.login.password=superuser
usergrid.sysadmin.login.allowed=true

--------------------
Implementation Notes
--------------------

The REST API is built using Jersey:

http://jersey.java.net/

Jersey is the reference implementation of JAX-RS, Java API for RESTful Web
Services, which was defined by JSR 311, which is detailed here:

http://jcp.org/en/jsr/detail?id=311

The Spring context in the Usergrid webapp also launches the Mongo and
WebSocket API servers.

The Usergrid webapp is meant to install as a root servlet. This means that on
the production server, it can be found at:

http://api.usergrid.com

However, on a local Tomcat server when running within Eclipse, it's
going to be found at:

http://localhost:8080/ROOT

The usergrid-standalone project is set up to run in Grizzly and will be at:

http://localhost:8080


