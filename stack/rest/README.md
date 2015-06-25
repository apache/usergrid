
Usergrid REST API Web App
=========================


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


Running Tests
-------------

To test, add the following configuration to the TOMCAT_HOME/conf/tomcat-users.xml

```xml
<tomcat-users>
    <role rolename="manager-gui"/>
    <role rolename="manager-jmx"/>
    <role rolename="manager-script"/>
    <role rolename="manager-status"/>
    <!-- this username and password is set into src/test/resources/arquillian.xml -->
    <user username="usergrid" password="testpassword" roles="manager-script, manager-jmx, manager-gui, manager-status"/>
</tomcat-users>
```


See the [documentation here](https://docs.jboss.org/author/display/ARQ/Tomcat+7.0+-+Managed) for more setup information.

Also, you will need to set the runtime to allow JMX deployments.  [Add the following](https://docs.jboss.org/author/display/ARQ/Tomcat+7.0+-+Remote) java runtime options to your tomcat instance.


```
JAVA_OPTS="$JAVA_OPTS -Dcom.sun.management.jmxremote.port=8089 "
JAVA_OPTS="$JAVA_OPTS -Dcom.sun.management.jmxremote.ssl=false "
JAVA_OPTS="$JAVA_OPTS -Dcom.sun.management.jmxremote.authenticate=false"
```




Add the following properties to you maven settings.xml

```xml
<catalina.host>localhost</catalina.host>
<catalina.jmx.port>8089</catalina.jmx.port>
 ```
